package com.siemens.internship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
		"spring.jpa.properties.jakarta.persistence.validation.mode=auto"
})
class InternshipApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private ItemRepository itemRepository;

	private String baseUrl;

	@BeforeEach
	void setUp() {
		baseUrl = "http://localhost:" + port + "/api/items";
		itemRepository.deleteAll();
	}

	//this basic test ensures that Spring can initialize the application.
	@Test
	void contextLoads() {
	}

	//1 creating an item via POST
	//2 retrieving the item via GET
	//3 ppdating the item via PUT
	//4 deleting the item via DELETE
	@Test
	void crudItemLifecycleTest() {

		Item item = new Item();
		item.setName("Integration Test Item");
		item.setDescription("Testing the full lifecycle");
		item.setStatus("NEW");
		item.setEmail("integration@test.com");

		ResponseEntity<Item> createResponse = restTemplate.postForEntity(
				baseUrl, item, Item.class);

		assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
		assertNotNull(createResponse.getBody());
		assertNotNull(createResponse.getBody().getId());
		assertEquals("Integration Test Item", createResponse.getBody().getName());

		Long itemId = createResponse.getBody().getId();

		ResponseEntity<Item> getResponse = restTemplate.getForEntity(
				baseUrl + "/" + itemId, Item.class);

		assertEquals(HttpStatus.OK, getResponse.getStatusCode());
		assertEquals("Integration Test Item", Objects.requireNonNull(getResponse.getBody()).getName());

		Item updatedItem = getResponse.getBody();
		updatedItem.setStatus("IN_PROGRESS");
		updatedItem.setDescription("Updated in integration test");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Item> requestEntity = new HttpEntity<>(updatedItem, headers);

		ResponseEntity<Item> updateResponse = restTemplate.exchange(
				baseUrl + "/" + itemId, HttpMethod.PUT, requestEntity, Item.class);

		assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
		assertEquals("IN_PROGRESS", Objects.requireNonNull(updateResponse.getBody()).getStatus());
		assertEquals("Updated in integration test", updateResponse.getBody().getDescription());

		ResponseEntity<Void> deleteResponse = restTemplate.exchange(
				baseUrl + "/" + itemId, HttpMethod.DELETE, null, Void.class);

		assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

		ResponseEntity<Item> verifyDeleteResponse = restTemplate.getForEntity(
				baseUrl + "/" + itemId, Item.class);
		assertEquals(HttpStatus.NOT_FOUND, verifyDeleteResponse.getStatusCode());
	}

	//1 creates several test items
	//2 calls the process endpoint
	//3 verifies all items were processed correctly

	@Test
	void testAsyncItemProcessing() {
		// Create test items
		IntStream.rangeClosed(1, 5).forEach(i -> {
			Item item = new Item();
			item.setName("Test Item " + i);
			item.setDescription("Description " + i);
			item.setStatus("NEW");
			item.setEmail("test" + i + "@example.com");
			itemRepository.save(item);
		});

		String processUrl = baseUrl + "/process";
		ResponseEntity<Item[]> processResponse = restTemplate.getForEntity(
				processUrl, Item[].class);

		assertEquals(HttpStatus.OK, processResponse.getStatusCode());
		Item[] processedItems = processResponse.getBody();
		assertNotNull(processedItems);
		assertEquals(5, processedItems.length);

		//all items have status PROCESSED
		for (Item item : processedItems) {
			assertEquals("PROCESSED", item.getStatus());
		}

		//retrieving all items again
		ResponseEntity<Item[]> getAllResponse = restTemplate.getForEntity(
				baseUrl, Item[].class);

		Item[] allItems = getAllResponse.getBody();
		assertNotNull(allItems);
		for (Item item : allItems) {
			assertEquals("PROCESSED", item.getStatus());
		}
	}

	@Test
	void testInvalidEmailValidation() {
		//verify that Hibernate Validator is on the classpath
		try {
			Class.forName("jakarta.validation.Validator");
		} catch (ClassNotFoundException e) {
			System.out.println("Skipping email validation test - Validator not found");
			return;
		}

		Item invalidItem = new Item();
		invalidItem.setName("Invalid Email Item");
		invalidItem.setDescription("Item with invalid email");
		invalidItem.setStatus("NEW");
		invalidItem.setEmail("not-a-valid-email");

		ResponseEntity<Map<String, String>> response = restTemplate.exchange(
				baseUrl,
				HttpMethod.POST,
				new HttpEntity<>(invalidItem),
				new ParameterizedTypeReference<Map<String, String>>() {}
		);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertTrue(response.getBody().containsKey("email"));
	}


	@Test
	void testInvalidStatusValidation() {
		//create an item with invalid status
		Item invalidItem = new Item();
		invalidItem.setName("Invalid Status Item");
		invalidItem.setDescription("Item with invalid status");
		invalidItem.setStatus("INVALID_STATUS");
		invalidItem.setEmail("valid@example.com");

		ResponseEntity<Map<String, String>> response = restTemplate.exchange(
				baseUrl,
				HttpMethod.POST,
				new HttpEntity<>(invalidItem),
				new ParameterizedTypeReference<Map<String, String>>() {}
		);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertTrue(response.getBody().containsKey("status"));
	}


	@Test
	void testNonExistentItemHandling() {
		//get a non-existent item
		ResponseEntity<Object> getResponse = restTemplate.getForEntity(
				baseUrl + "/99999", Object.class);
		assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());

		//update a non-existent item
		Item item = new Item();
		item.setName("Non-existent Item");
		item.setDescription("This item doesn't exist");
		item.setStatus("NEW");
		item.setEmail("nonexistent@example.com");

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Item> requestEntity = new HttpEntity<>(item, headers);

		ResponseEntity<Object> updateResponse = restTemplate.exchange(
				baseUrl + "/99999", HttpMethod.PUT, requestEntity, Object.class);
		assertEquals(HttpStatus.NOT_FOUND, updateResponse.getStatusCode());

		ResponseEntity<Object> deleteResponse = restTemplate.exchange(
				baseUrl + "/99999", HttpMethod.DELETE, null, Object.class);
		assertEquals(HttpStatus.NOT_FOUND, deleteResponse.getStatusCode());
	}
}