package com.siemens.internship;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@EnableAsync
public class ItemService {
    private static final Logger logger = LoggerFactory.getLogger(ItemService.class);

    @Autowired
    private ItemRepository itemRepository;

     //core pool size: Number of threads to keep in the pool
     //-keep alive time: When threads are greater than core, this is maximum time excess idle threads will wait for new tasks
     //-queue: Holds tasks when all threads are busy
     //executed by the caller thread when queue is full

    private static final ExecutorService executor = new ThreadPoolExecutor(
            5,
            10, // max pool size
            60L, // keep alive time
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public List<Item> findAll() {
        logger.debug("Finding all items");
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        logger.debug("Finding item with ID: {}", id);
        return itemRepository.findById(id);
    }

     //@param item The item to save
     //@return The saved item with any updates (like generated ID)

    public Item save(Item item) {
        logger.debug("Saving item: {}", item.getId() != null ? item.getId() : "new item");
        return itemRepository.save(item);
    }
     //Deletes an item by its ID.
    public void deleteById(Long id) {
        logger.debug("Deleting item with ID: {}", id);
        itemRepository.deleteById(id);
    }

     //1 Retrieving each item from the database
     //2 Updating its status to "PROCESSED"
     //3 Saving the updated item back to the database

     //@return CompletableFuture containing a list of all successfully processed items
    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        logger.info("Starting async processing of items");

        try {
            List<Long> itemIds = itemRepository.findAllIds();
            logger.info("Found {} items to process", itemIds.size());

            if (itemIds.isEmpty()) {
                return CompletableFuture.completedFuture(new ArrayList<>());
            }

            List<CompletableFuture<Item>> futures = itemIds.stream()
                    .map(id -> CompletableFuture.supplyAsync(() -> processItem(id), executor))
                    .toList();
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));

            return allFutures.thenApply(v ->
                    futures.stream()
                            .map(future -> {
                                try {
                                    return future.join(); // Safe after allOf completes
                                } catch (CompletionException e) {
                                    logger.error("Error retrieving future result", e.getCause());
                                    return null;
                                }
                            })
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
            );
        } catch (Exception e) {
            logger.error("Failed to initiate item processing", e);
            CompletableFuture<List<Item>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(
                    new RuntimeException("Failed to process items", e));
            return failedFuture;
        }
    }

    //@param id ID of the item to process
    //@return The processed item if successful, null otherwise

    private Item processItem(Long id) {
        try {
            logger.debug("Processing item with ID: {}", id);

            // Simulate some processing work
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Processing interrupted", e);
            }

            // Find the item
            Optional<Item> optionalItem = itemRepository.findById(id);
            if (optionalItem.isEmpty()) {
                logger.warn("Item with ID {} not found during processing", id);
                return null;
            }

            // Update and save the item
            Item item = optionalItem.get();
            item.setStatus("PROCESSED");
            Item savedItem = itemRepository.save(item);

            logger.debug("Successfully processed item with ID: {}", id);
            return savedItem;
        } catch (Exception e) {
            logger.error("Error processing item ID: {}", id, e);
            return null;
        }
    }
}