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

}