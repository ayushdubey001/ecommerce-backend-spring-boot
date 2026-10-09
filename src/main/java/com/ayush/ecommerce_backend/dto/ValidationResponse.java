package com.ayush.ecommerce_backend.dto;

import java.time.LocalDateTime;
import java.util.List;



public class ValidationResponse {
    private LocalDateTime timeStamp;
    private int statusCode;
    private String message;
    List<ValidationError> errors;

    public ValidationResponse(LocalDateTime timeStamp, int statusCode, String message, List<ValidationError> errors) {
        this.timeStamp = timeStamp;
        this.statusCode = statusCode;
        this.message = message;
        this.errors = errors;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(LocalDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
    }
}
