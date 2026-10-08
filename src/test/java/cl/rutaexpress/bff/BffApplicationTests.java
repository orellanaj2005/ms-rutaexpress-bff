package cl.rutaexpress.bff;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Verifica que el contexto de Spring arranca. Se entregan valores ficticios para
 * las variables de Azure AD y se reemplaza el JwtDecoder por un mock, para que la
 * prueba no dependa de variables de entorno ni de conexión a internet.
 */
@SpringBootTest(properties = {
        "TENANT_ID=00000000-0000-0000-0000-000000000000",
        "API_CLIENT_ID=00000000-0000-0000-0000-000000000000"
})
class BffApplicationTests {

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void contextLoads() {
    }
}