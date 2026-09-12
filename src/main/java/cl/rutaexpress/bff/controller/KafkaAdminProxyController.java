package cl.rutaexpress.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/kafka")
public class KafkaAdminProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.kafka-admin-url}")
    private String kafkaAdminUrl;

    public KafkaAdminProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/topics")
    public ResponseEntity<Object> listTopics() {
        return restTemplate.getForEntity(kafkaAdminUrl + "/api/kafka/topics", Object.class);
    }

    @GetMapping("/topics/{name}")
    public ResponseEntity<Object> describeTopic(@PathVariable String name) {
        return restTemplate.getForEntity(kafkaAdminUrl + "/api/kafka/topics/" + name, Object.class);
    }

    @GetMapping("/consumer-groups")
    public ResponseEntity<Object> listConsumerGroups() {
        return restTemplate.getForEntity(kafkaAdminUrl + "/api/kafka/consumer-groups", Object.class);
    }

    @GetMapping("/consumer-groups/{groupId}/lag")
    public ResponseEntity<Object> getConsumerGroupLag(@PathVariable String groupId) {
        return restTemplate.getForEntity(kafkaAdminUrl + "/api/kafka/consumer-groups/" + groupId + "/lag", Object.class);
    }
}