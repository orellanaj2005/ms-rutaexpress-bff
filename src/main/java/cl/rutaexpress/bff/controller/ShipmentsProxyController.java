package cl.rutaexpress.bff.controller;

import jakarta.servlet.http.HttpServletRequest;
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

    @GetMapping
    public ResponseEntity<Object> listShipments(HttpServletRequest request) {
        String queryString = request.getQueryString();
        String url = shipmentsUrl + "/api/shipments" + (queryString != null ? "?" + queryString : "");
        return restTemplate.getForEntity(url, Object.class);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getShipment(@PathVariable String id) {
        return restTemplate.getForEntity(shipmentsUrl + "/api/shipments/" + id, Object.class);
    }

    @PostMapping
    public ResponseEntity<Object> createShipment(@RequestBody Object body) {
        return restTemplate.postForEntity(shipmentsUrl + "/api/shipments", body, Object.class);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Object> updateStatus(@PathVariable String id, @RequestBody Object body) {
        restTemplate.put(shipmentsUrl + "/api/shipments/" + id + "/status", body);
        return ResponseEntity.ok().build();
    }
}