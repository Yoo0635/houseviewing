package com.house.houseviewing.global.exception;

import lombok.Getter;

@Getter
public class AppException extends RuntimeException {

    private final ExceptionCode exceptionCode;

    public AppException(ExceptionCode exceptionCode){
        super(exceptionCode.getMessage());
        this.exceptionCode = exceptionCode;
    }

    public AppException(ExceptionCode exceptionCode, String message){
        super(message);
        this.exceptionCode = exceptionCode;
    }
}
