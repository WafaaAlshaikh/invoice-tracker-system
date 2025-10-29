    package com.example.invoicetracker.exception;

    public class UserDeactivatedException extends RuntimeException {
        public UserDeactivatedException(String message) {
            super(message);
        }
    }
