package cl.rutaexpress.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/catalog")
public class CatalogProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.catalog-url}")
    private String catalogUrl;

    public CatalogProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/services")
    public ResponseEntity<Object> listServices() {
        return restTemplate.getForEntity(catalogUrl + "/api/catalog/services", Object.class);
    }

    @PostMapping("/services")
    public ResponseEntity<Object> createService(@RequestBody Object body) {
        return restTemplate.postForEntity(catalogUrl + "/api/catalog/services", body, Object.class);
    }

    @PutMapping("/services/{id}")
    public ResponseEntity<Object> updateService(@PathVariable String id, @RequestBody Object body) {
        restTemplate.put(catalogUrl + "/api/catalog/services/" + id, body);
        return ResponseEntity.ok().build();
    }
}