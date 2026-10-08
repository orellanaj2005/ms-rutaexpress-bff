package cl.rutaexpress.bff.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.util.Map;

/**
 * Sin este manejador, RestTemplate lanza una excepción ante cualquier 4xx/5xx del
 * microservicio de destino y el BFF responde 500. Aquí se devuelve el mismo código
 * y el mismo cuerpo que entregó el microservicio (400, 404, 409, 502...).
 */
@RestControllerAdvice
public class DownstreamErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(DownstreamErrorHandler.class);

    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<String> handleDownstreamStatus(HttpStatusCodeException ex) {
        log.warn("El microservicio respondió {}: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
        return ResponseEntity.status(ex.getStatusCode())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ex.getResponseBodyAsString());
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<Map<String, String>> handleUnreachable(ResourceAccessException ex) {
        log.error("No se pudo contactar al microservicio: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "El servicio de destino no responde"));
    }
}