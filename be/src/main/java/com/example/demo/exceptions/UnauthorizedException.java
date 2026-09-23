package com.example.demo.exceptions;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String messaggio) { super(messaggio); }
}
