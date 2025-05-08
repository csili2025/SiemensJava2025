package com.siemens.internship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
class ItemControllerUnitTests {

    @Mock
    private ItemService itemService;

    @Mock
    private BindingResult bindingResult;

    @InjectMocks
    private ItemController itemController;

    private Item testItem;

    @BeforeEach
    void setUp() {
        testItem = new Item();
        testItem.setId(1L);
        testItem.setName("Test Item");
        testItem.setDescription("Test Description");
        testItem.setStatus("NEW");
        testItem.setEmail("test@example.com");
    }

    @Test
    void getAllItems_ShouldReturnAllItems() {
        // Arrange
        List<Item> items = Collections.singletonList(testItem);
        when(itemService.findAll()).thenReturn(items);

        // Act
        ResponseEntity<List<Item>> response = itemController.getAllItems();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(items, response.getBody());
        verify(itemService, times(1)).findAll();
    }

    @Test
    void createItem_WithValidItem_ShouldReturnCreated() {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(false);
        when(itemService.save(any(Item.class))).thenReturn(testItem);

        // Act
        ResponseEntity<?> response = itemController.createItem(testItem, bindingResult);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(testItem, response.getBody());
        verify(itemService, times(1)).save(testItem);
    }

    @Test
    void createItem_WithValidationErrors_ShouldReturnBadRequest() {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(true);
        FieldError error = new FieldError("item", "email", "Invalid email");
        when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(error));

        // Act
        ResponseEntity<?> response = itemController.createItem(testItem, bindingResult);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertInstanceOf(Map.class, response.getBody());
        Map<String, String> errors = (Map<String, String>) response.getBody();
        assertEquals("Invalid email", errors.get("email"));
        verify(itemService, never()).save(any(Item.class));
    }

    @Test
    void getItemById_WhenItemExists_ShouldReturnItem() {
        // Arrange
        when(itemService.findById(1L)).thenReturn(Optional.of(testItem));

        // Act
        ResponseEntity<Item> response = itemController.getItemById(1L);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testItem, response.getBody());
        verify(itemService, times(1)
        ).findById(1L);
    }

    @Test
    void getItemById_WhenItemDoesNotExist_ShouldReturnNotFound() {
        // Arrange
        when(itemService.findById(99L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Item> response = itemController.getItemById(99L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(itemService, times(1)).findById(99L);
    }

    @Test
    void updateItem_WhenItemExists_ShouldReturnUpdatedItem() {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(false);
        when(itemService.findById(1L)).thenReturn(Optional.of(testItem));

        Item updatedItem = new Item();
        updatedItem.setId(1L);
        updatedItem.setName("Updated Name");
        updatedItem.setDescription("Updated Description");
        updatedItem.setStatus("IN_PROGRESS");
        updatedItem.setEmail("updated@example.com");

        when(itemService.save(any(Item.class))).thenReturn(updatedItem);

        // Act
        ResponseEntity<?> response = itemController.updateItem(1L, updatedItem, bindingResult);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedItem, response.getBody());

        // Verify that ID is set correctly before saving
        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
        verify(itemService).save(itemCaptor.capture());
        assertEquals(1L, itemCaptor.getValue().getId());
    }

    @Test
    void updateItem_WhenItemDoesNotExist_ShouldReturnNotFound() {
        // Arrange
        when(bindingResult.hasErrors()).thenReturn(false);
        when(itemService.findById(99L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<?> response = itemController.updateItem(99L, testItem, bindingResult);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(itemService, never()).save(any(Item.class));
    }

    @Test
    void deleteItem_WhenItemExists_ShouldReturnNoContent() {
        // Arrange
        when(itemService.findById(1L)).thenReturn(Optional.of(testItem));
        doNothing().when(itemService).deleteById(1L);

        // Act
        ResponseEntity<Void> response = itemController.deleteItem(1L);

        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(itemService, times(1)).deleteById(1L);
    }

    @Test
    void deleteItem_WhenItemDoesNotExist_ShouldReturnNotFound() {
        // Arrange
        when(itemService.findById(99L)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Void> response = itemController.deleteItem(99L);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(itemService, never()).deleteById(anyLong());
    }

    @Test
    void processItems_ShouldReturnProcessedItems() throws Exception {
        // Arrange
        List<Item> processedItems = Arrays.asList(testItem);
        CompletableFuture<List<Item>> future = CompletableFuture.completedFuture(processedItems);
        when(itemService.processItemsAsync()).thenReturn(future);

        // Act
        ResponseEntity<List<Item>> response = itemController.processItems();

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(processedItems, response.getBody());
        verify(itemService, times(1)).processItemsAsync();
    }
}

/**
 * Additional tests for Item entity validation
 */
@SpringBootTest
class ItemValidationTests {

    private Item item;

    @BeforeEach
    void setUp() {
        item = new Item();
        item.setId(1L);
        item.setName("Valid Name");
        item.setDescription("Valid Description");
        item.setStatus("NEW");
        item.setEmail("valid@example.com");
    }

    @Test
    void toStringMethod_ShouldExcludeId() {
        // Verify toString implementation excludes ID for better logging
        String itemString = item.toString();
        assertFalse(itemString.contains("id=1"));
        assertTrue(itemString.contains("name=Valid Name"));
    }

    /**
     * Additional tests for Item repository
     */
    @SpringBootTest
    static class ItemRepositoryIntegrationTests {
        // Additional repository tests could be added here
    }
}