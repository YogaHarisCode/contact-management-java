package com.yogaharis.contactmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the Contact Management REST API.
 *
 * @author Yoga Harisistianugroho
 */
@SpringBootApplication
public class ContactManagementJavaApplication {

	/**
	 * Starts the Spring Boot application.
	 *
	 * @param args command-line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(ContactManagementJavaApplication.class, args);
	}

}