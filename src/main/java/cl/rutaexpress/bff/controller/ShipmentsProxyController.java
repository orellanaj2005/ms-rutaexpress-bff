package cl.rutaexpress.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/shipments")
public class ShipmentsProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.shipments-url}")
    private String shipmentsUrl;

    public ShipmentsProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getShipment(@PathVariable String id) {
        return restTemplate.getForEntity(shipmentsUrl + "/api/shipments/" + id, Object.class);
    }
}