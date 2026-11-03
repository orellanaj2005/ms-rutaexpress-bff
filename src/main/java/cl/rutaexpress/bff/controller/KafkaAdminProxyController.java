package cl.rutaexpress.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Proxy hacia ms-rutaexpress-kafka-admin. Expone los mismos endpoints que el
 * administrador, de lectura y de escritura, solo para el rol Admin (regla de
 * SecurityConfig para /api/kafka/** y, además, @PreAuthorize en la clase).
 *
 * El JWT del usuario se reenvía con el interceptor del RestTemplate definido en
 * BffApplication, y los errores 4xx/5xx del administrador (400 de validación,
 * 404, 409 de tópico protegido o grupo activo...) se devuelven tal cual mediante
 * DownstreamErrorHandler.
 */
@RestController
@RequestMapping("/api/kafka")
@PreAuthorize("hasAuthority('Admin')")
public class KafkaAdminProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.kafka-admin-url}")
    private String kafkaAdminUrl;

    public KafkaAdminProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ---------- Tópicos ----------

    @GetMapping("/topics")
    public ResponseEntity<Object> listTopics(@RequestParam(defaultValue = "false") boolean includeInternal) {
        URI target = UriComponentsBuilder.fromUriString(kafkaAdminUrl)
                .path("/api/kafka/topics")
                .queryParam("includeInternal", includeInternal)
                .build()
                .toUri();
        return forward(HttpMethod.GET, target, null);
    }

    @GetMapping("/topics/{name}")
    public ResponseEntity<Object> describeTopic(@PathVariable String name) {
        return forward(HttpMethod.GET, uri("/api/kafka/topics/{name}", name), null);
    }

    @PostMapping("/topics")
    public ResponseEntity<Object> createTopic(@RequestBody Object body) {
        return forward(HttpMethod.POST, uri("/api/kafka/topics"), body);
    }

    @DeleteMapping("/topics/{name}")
    public ResponseEntity<Object> deleteTopic(@PathVariable String name) {
        return forward(HttpMethod.DELETE, uri("/api/kafka/topics/{name}", name), null);
    }

    @GetMapping("/topics/{name}/configs")
    public ResponseEntity<Object> getTopicConfigs(@PathVariable String name,
                                                  @RequestParam(defaultValue = "false") boolean onlyOverrides) {
        URI target = UriComponentsBuilder.fromUriString(kafkaAdminUrl)
                .path("/api/kafka/topics/{name}/configs")
                .queryParam("onlyOverrides", onlyOverrides)
                .encode()
                .buildAndExpand(name)
                .toUri();
        return forward(HttpMethod.GET, target, null);
    }

    @PutMapping("/topics/{name}/configs")
    public ResponseEntity<Object> updateTopicConfigs(@PathVariable String name, @RequestBody Object body) {
        return forward(HttpMethod.PUT, uri("/api/kafka/topics/{name}/configs", name), body);
    }

    @PostMapping("/topics/{name}/partitions")
    public ResponseEntity<Object> increasePartitions(@PathVariable String name, @RequestBody Object body) {
        return forward(HttpMethod.POST, uri("/api/kafka/topics/{name}/partitions", name), body);
    }

    // ---------- Consumer groups ----------

    @GetMapping("/consumer-groups")
    public ResponseEntity<Object> listConsumerGroups() {
        return forward(HttpMethod.GET, uri("/api/kafka/consumer-groups"), null);
    }

    @GetMapping("/consumer-groups/{groupId}")
    public ResponseEntity<Object> describeConsumerGroup(@PathVariable String groupId) {
        return forward(HttpMethod.GET, uri("/api/kafka/consumer-groups/{groupId}", groupId), null);
    }

    @GetMapping("/consumer-groups/{groupId}/lag")
    public ResponseEntity<Object> getConsumerGroupLag(@PathVariable String groupId) {
        return forward(HttpMethod.GET, uri("/api/kafka/consumer-groups/{groupId}/lag", groupId), null);
    }

    @PostMapping("/consumer-groups/{groupId}/reset")
    public ResponseEntity<Object> resetOffsets(@PathVariable String groupId, @RequestBody Object body) {
        return forward(HttpMethod.POST, uri("/api/kafka/consumer-groups/{groupId}/reset", groupId), body);
    }

    @DeleteMapping("/consumer-groups/{groupId}")
    public ResponseEntity<Object> deleteConsumerGroup(@PathVariable String groupId) {
        return forward(HttpMethod.DELETE, uri("/api/kafka/consumer-groups/{groupId}", groupId), null);
    }

    // ---------- Auxiliares ----------

    /** Codifica las variables de ruta una sola vez (nombres de tópico con puntos, guiones, etc.). */
    private URI uri(String path, Object... variables) {
        return UriComponentsBuilder.fromUriString(kafkaAdminUrl)
                .path(path)
                .encode()
                .buildAndExpand(variables)
                .toUri();
    }

    /**
     * Reenvía la petición y devuelve el mismo código (200, 201, 204...) y cuerpo del
     * administrador, sin copiar sus headers de transporte.
     */
    private ResponseEntity<Object> forward(HttpMethod method, URI target, Object body) {
        HttpEntity<?> request = body == null ? HttpEntity.EMPTY : new HttpEntity<>(body);
        ResponseEntity<Object> response = restTemplate.exchange(target, method, request, Object.class);
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
}
