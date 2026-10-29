package br.com.garage.inbound.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebhookController {

    private final WebhookDispatcher dispatcher;

    public WebhookController(WebhookDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Object> receive(@RequestBody WebhookEvent event) {
        return dispatcher.dispatch(event);
    }
}
