package com.akhilesh.shopnest.exceptions;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productName) {
        super("Not enough stock for " + productName);
    }
}
