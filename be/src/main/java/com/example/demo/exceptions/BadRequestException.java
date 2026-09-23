package com.example.demo.exceptions;

public class BadRequestException extends RuntimeException {
    public BadRequestException(String messaggio) { super(messaggio); }
}
