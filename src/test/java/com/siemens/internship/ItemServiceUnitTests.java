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
