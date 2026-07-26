package com.house.houseviewing.global.exception;

import com.house.houseviewing.global.exception.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateLoginId(AppException e){
        ExceptionCode code = e.getExceptionCode();
        ErrorResponse errorResponse = new ErrorResponse(code.getCode(), e.getMessage());
        log.info("status = "+code.getStatus());
        log.info("code  = "+code.getCode());
        log.info("message = "+e.getMessage());
        return ResponseEntity.status(code.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e){
        String message = e.getBindingResult()
                .getAllErrors()
                .get(0)
                .getDefaultMessage();
        ErrorResponse response = new ErrorResponse("VP000", message);
        log.info("message = " + response.getMessage());
        return ResponseEntity.badRequest().body(response);
    }
}
