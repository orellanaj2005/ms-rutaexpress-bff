package cl.rutaexpress.bff.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/audit")
public class AuditProxyController {

    private final RestTemplate restTemplate;

    @Value("${services.audit-url}")
    private String auditUrl;

    public AuditProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping
    public ResponseEntity<Object> getTimeline(@RequestParam(required = false) String user,
                                               @RequestParam(required = false) String from,
                                               @RequestParam(required = false) String to,
                                               @RequestParam(required = false) String eventType) {
        String url = auditUrl + "/api/audit"
            + "?user=" + nullToEmpty(user)
            + "&from=" + nullToEmpty(from)
            + "&to=" + nullToEmpty(to)
            + "&eventType=" + nullToEmpty(eventType);
        return restTemplate.getForEntity(url, Object.class);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}