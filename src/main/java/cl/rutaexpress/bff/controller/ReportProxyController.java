package cl.rutaexpress.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/report")
public class ReportProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.report-url}")
    private String reportUrl;

    public ReportProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/kpis")
    public ResponseEntity<Object> getKpis(@RequestParam String range) {
        return restTemplate.getForEntity(reportUrl + "/api/report/kpis?range=" + range, Object.class);
    }

    @GetMapping("/top-services")
    public ResponseEntity<Object> getTopServices(@RequestParam String range) {
        return restTemplate.getForEntity(reportUrl + "/api/report/top-services?range=" + range, Object.class);
    }
}