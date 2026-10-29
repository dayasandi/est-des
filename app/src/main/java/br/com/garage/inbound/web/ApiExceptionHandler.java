package br.com.garage.inbound.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(InvalidRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail invalidRequest(InvalidRequestException e) {
        log.atWarn().addKeyValue("reason", e.getMessage()).log("requisicao invalida");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ProblemDetail unreadable(HttpMessageNotReadableException e) {
        log.atWarn().addKeyValue("reason", e.getMostSpecificCause().getMessage()).log("corpo ilegivel");
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "corpo da requisicao invalido");
    }
}
