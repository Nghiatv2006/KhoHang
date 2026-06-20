package com.example.Hehe.exception;

public class ProductDeletedConflictException extends RuntimeException {
    private final Integer productId;

    public ProductDeletedConflictException(Integer productId, String message) {
        super(message);
        this.productId = productId;
    }

    public Integer getProductId() {
        return productId;
    }
}
