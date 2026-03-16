package com.e108.be.domain.dog.exception;

import com.e108.be.global.error.exception.InvalidGroupException;

public class InvalidDogRequestException extends InvalidGroupException {
    public InvalidDogRequestException(String message) {
        super(message);
    }
}
