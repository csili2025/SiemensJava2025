package com.siemens.internship;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@RestController
@RequestMapping("/api/items")
public class ItemController {
    private static final Logger logger = LoggerFactory.getLogger(ItemController.class);

    @Autowired
    private ItemService itemService;

    private static final int ASYNC_TIMEOUT_SECONDS = 30;

     //Retrieves all itms from the database.
     //@return ResponseEntity containing a list of all items with HTTP-200-OK
     //@throws ResponseSatusException with 500 status if an error occurs

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        try {
            List<Item> items = itemService.findAll();
            logger.debug("Retrieved {} items", items.size());
            return new ResponseEntity<>(items, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving all items", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving items", e);
        }
    }

    //Creates a new item with validation.
    //@return ResponseEntity with the created item (HTTP 201) or validation errors (HTTP 400)

    @PostMapping
    public ResponseEntity<?> createItem(@Valid @RequestBody Item item, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : result.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            logger.warn("Validation errors when creating item: {}", errors);
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }

        // This is redundant with annotation validation, but keeping it for test compatibility
        Map<String, String> errors = new HashMap<>();
        boolean hasErrors = false;

        //validate email format
        if (item.getEmail() != null && !item.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            errors.put("email", "Email must be in a valid format");
            hasErrors = true;
            logger.warn("Email validation failed for: {}", item.getEmail());
        }

        //validate status values
        if (item.getStatus() != null && !item.getStatus().matches("^(NEW|IN_PROGRESS|PROCESSED|COMPLETED)$")) {
            errors.put("status", "Status must be one of: NEW, IN_PROGRESS, PROCESSED, COMPLETED");
            hasErrors = true;
            logger.warn("Status validation failed for: {}", item.getStatus());
        }

        if (hasErrors) {
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }

        try {
            Item savedItem = itemService.save(item);
            logger.info("Created new item with ID: {}", savedItem.getId());
            return new ResponseEntity<>(savedItem, HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Error creating item", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creating item", e);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) {
        try {
            return itemService.findById(id)
                    .map(item -> {
                        logger.debug("Found item with ID: {}", id);
                        return new ResponseEntity<>(item, HttpStatus.OK);
                    })
                    .orElseGet(() -> {
                        logger.debug("Item with ID {} not found", id);
                        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                    });
        } catch (Exception e) {
            logger.error("Error retrieving item with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving item", e);
        }
    }
     //@param result Validation result from Spring's validation framework
     //@return ResponseEntity with the updated item (HTTP 200), validation errors (HTTP 400),or HTTP 404 if not found
     //@throws ResponseStatusException with 500 status if an error occurs

    @PutMapping("/{id}")
    public ResponseEntity<?> updateItem(@PathVariable Long id, @Valid @RequestBody Item item, BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : result.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
            }
            logger.warn("Validation errors when updating item {}: {}", id, errors);
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }

        Map<String, String> errors = new HashMap<>();
        boolean hasErrors = false;

        // vlidate email format
        if (item.getEmail() != null && !item.getEmail().matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            errors.put("email", "Email must be in a valid format");
            hasErrors = true;
            logger.warn("Email validation failed for: {}", item.getEmail());
        }

        //validate status values
        if (item.getStatus() != null && !item.getStatus().matches("^(NEW|IN_PROGRESS|PROCESSED|COMPLETED)$")) {
            errors.put("status", "Status must be one of: NEW, IN_PROGRESS, PROCESSED, COMPLETED");
            hasErrors = true;
            logger.warn("Status validation failed for: {}", item.getStatus());
        }

        if (hasErrors) {
            return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
        }

        try {
            Optional<Item> existingItem = itemService.findById(id);
            if (existingItem.isPresent()) {
                item.setId(id);
                Item updatedItem = itemService.save(item);
                logger.info("Updated item with ID: {}", id);
                return new ResponseEntity<>(updatedItem, HttpStatus.OK);
            } else {
                logger.debug("Item with ID {} not found for update", id);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error updating item with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating item", e);
        }
    }

     //@param id The ID of the item to delete
     //@return ResponseEntity with HTTP-204-NO_CONTENT if successful or HTTP-404 if not found
     //@throws ResponseStatusException with 500-status if an error occurs

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        try {
            if (itemService.findById(id).isPresent()) {
                itemService.deleteById(id);
                logger.info("Deleted item with ID: {}", id);
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                logger.debug("Item with ID {} not found for deletion", id);
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error deleting item with ID: {}", id, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting item", e);
        }
    }

}