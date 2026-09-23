package com.example.demo.exceptions;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String messaggio) { super(messaggio); }
    public NotFoundException(Long id) { super("Record con id " + id + " non trovato"); }
}
