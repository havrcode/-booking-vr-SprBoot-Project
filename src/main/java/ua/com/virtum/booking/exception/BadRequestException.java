package ua.com.virtum.booking.exception;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) { super(message); }
}

