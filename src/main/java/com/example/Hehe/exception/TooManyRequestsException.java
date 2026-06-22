package com.example.Hehe.exception;

import java.time.LocalDateTime;

/**
 * Exception ném ra khi user spam quá nhiều lần đăng nhập/đăng xuất.
 * Tương ứng với HTTP 429 Too Many Requests.
 */
public class TooManyRequestsException extends RuntimeException {
    private final LocalDateTime banUntil;

    public TooManyRequestsException(String message, LocalDateTime banUntil) {
        super(message);
        this.banUntil = banUntil;
    }

    public LocalDateTime getBanUntil() {
        return banUntil;
    }
}
