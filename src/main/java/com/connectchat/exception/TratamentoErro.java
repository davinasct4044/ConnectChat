package com.connectchat.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class TratamentoErro {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> tratarErro(MethodArgumentNotValidException erro) {
        Map<String, String> erros = new HashMap<>();

        for (FieldError erroCampo : erro.getBindingResult().getFieldErrors()) {
            erros.put(
                    erroCampo.getField(),
                    erroCampo.getDefaultMessage()
            );
        }

        return new ResponseEntity<>(
                erros,
                HttpStatus.BAD_REQUEST
        );
    }
}
