package cl.rutaexpress.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Proxy hacia ms-rutaexpress-rabbitmq-admin. Expone los mismos endpoints que el
 * administrador. El JWT del usuario se reenvía con el interceptor del RestTemplate
 * definido en BffApplication, y los errores 4xx/5xx del administrador se devuelven
 * tal cual mediante DownstreamErrorHandler.
 */
@RestController
@RequestMapping("/api/rabbitmq")
public class RabbitMqProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.rabbitmq-admin-url}")
    private String rabbitmqAdminUrl;

    public RabbitMqProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ---------- Colas ----------

    @GetMapping("/queues")
    public ResponseEntity<Object> listQueues() {
        return get(uri("/api/rabbitmq/queues"));
    }

    @GetMapping("/queues/{name}")
    public ResponseEntity<Object> getQueue(@PathVariable String name) {
        return get(uri("/api/rabbitmq/queues/{name}", name));
    }

    @PostMapping("/queues")
    public ResponseEntity<Object> createQueue(@RequestBody Object body) {
        return post(uri("/api/rabbitmq/queues"), body);
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<Void> deleteQueue(@PathVariable String name) {
        return delete(uri("/api/rabbitmq/queues/{name}", name));
    }

    // ---------- Exchanges ----------

    @GetMapping("/exchanges")
    public ResponseEntity<Object> listExchanges() {
        return get(uri("/api/rabbitmq/exchanges"));
    }

    @PostMapping("/exchanges")
    public ResponseEntity<Object> createExchange(@RequestBody Object body) {
        return post(uri("/api/rabbitmq/exchanges"), body);
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<Void> deleteExchange(@PathVariable String name) {
        return delete(uri("/api/rabbitmq/exchanges/{name}", name));
    }

    // ---------- Bindings ----------

    @GetMapping("/bindings")
    public ResponseEntity<Object> listBindings() {
        return get(uri("/api/rabbitmq/bindings"));
    }

    @PostMapping("/bindings")
    public ResponseEntity<Object> createBinding(@RequestBody Object body) {
        return post(uri("/api/rabbitmq/bindings"), body);
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<Void> deleteBinding(@RequestParam String exchange,
                                              @RequestParam String queue,
                                              @RequestParam String routingKey) {
        // Las routing keys pueden traer '#' o '*', por eso los parámetros se codifican
        // como variables y no se concatenan al texto de la URL.
        URI target = UriComponentsBuilder.fromUriString(rabbitmqAdminUrl)
                .path("/api/rabbitmq/bindings")
                .queryParam("exchange", "{exchange}")
                .queryParam("queue", "{queue}")
                .queryParam("routingKey", "{routingKey}")
                .encode()
                .buildAndExpand(exchange, queue, routingKey)
                .toUri();
        return delete(target);
    }

    // ---------- Overview ----------

    @GetMapping("/overview")
    public ResponseEntity<Object> getOverview() {
        return get(uri("/api/rabbitmq/overview"));
    }

    // ---------- Auxiliares ----------

    private URI uri(String path, Object... variables) {
        return UriComponentsBuilder.fromUriString(rabbitmqAdminUrl)
                .path(path)
                .encode()
                .buildAndExpand(variables)
                .toUri();
    }

    private ResponseEntity<Object> get(URI target) {
        ResponseEntity<Object> response = restTemplate.getForEntity(target, Object.class);
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    private ResponseEntity<Object> post(URI target, Object body) {
        ResponseEntity<Object> response = restTemplate.postForEntity(target, body, Object.class);
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    private ResponseEntity<Void> delete(URI target) {
        restTemplate.exchange(target, HttpMethod.DELETE, HttpEntity.EMPTY, Void.class);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}