package com.connectchat.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.HttpStatus;

@RestControllerAdvice
public class TratamentoErro {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<String> tratarErro(MethodArgumentNotValidException erro) {
        ResponseEntity<String> resposta = new ResponseEntity<String>("Dados inválidos", HttpStatus.BAD_REQUEST);
        return resposta;
    }
}
