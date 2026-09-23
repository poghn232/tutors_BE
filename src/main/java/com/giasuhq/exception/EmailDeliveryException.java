package com.giasuhq.exception;

public class EmailDeliveryException extends RuntimeException {

    private final String category;

    public EmailDeliveryException(String category, String message) {
        super(message);
        this.category = category;
    }

    public EmailDeliveryException(String category, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
    }

    public String getCategory() {
        return category;
    }
}
