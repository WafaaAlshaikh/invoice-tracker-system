package com.example.invoicetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.validation.annotation.Validated;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;


@SpringBootApplication
@Validated
public class InvoicetrackerApplication extends SpringBootServletInitializer  {

	public static void main(String[] args) {

		SpringApplication.run(InvoicetrackerApplication.class, args);
	}

	@Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(InvoicetrackerApplication.class);
    }

}
