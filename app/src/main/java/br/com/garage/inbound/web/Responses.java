package br.com.garage.inbound.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

final class Responses {

    private Responses() {}

    static ResponseEntity<Object> ok(Object body) {
        return ResponseEntity.ok(body);
    }

    static ResponseEntity<Object> rejected(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
