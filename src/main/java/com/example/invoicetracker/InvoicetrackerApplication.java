package com.example.invoicetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.validation.annotation.Validated;

@SpringBootApplication
@Validated
public class InvoicetrackerApplication {

	public static void main(String[] args) {

		SpringApplication.run(InvoicetrackerApplication.class, args);
	}

}
