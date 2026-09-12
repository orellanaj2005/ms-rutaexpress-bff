package cl.rutaexpress.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/rabbitmq")
public class RabbitMqProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.rabbitmq-admin-url}")
    private String rabbitmqAdminUrl;

    public RabbitMqProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/queues")
    public ResponseEntity<Object> listQueues() {
        return restTemplate.getForEntity(rabbitmqAdminUrl + "/api/rabbitmq/queues", Object.class);
    }

    @GetMapping("/queues/{name}")
    public ResponseEntity<Object> getQueue(@PathVariable String name) {
        return restTemplate.getForEntity(rabbitmqAdminUrl + "/api/rabbitmq/queues/" + name, Object.class);
    }

    @GetMapping("/overview")
    public ResponseEntity<Object> getOverview() {
        return restTemplate.getForEntity(rabbitmqAdminUrl + "/api/rabbitmq/overview", Object.class);
    }
}