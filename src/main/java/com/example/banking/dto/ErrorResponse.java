package com.example.banking.dto;



import java.time.Instant;
import java.util.Map;

/**
 * One consistent JSON shape for every error returned by the API.
 * fieldErrors is filled only for validation errors (otherwise it is empty).
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {
}
