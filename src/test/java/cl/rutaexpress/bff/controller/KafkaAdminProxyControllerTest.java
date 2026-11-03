package cl.rutaexpress.bff.controller;

import cl.rutaexpress.bff.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que el BFF reenvía cada ruta de administración Kafka al microservicio, con el
 * mismo método, cuerpo y código de respuesta, y que solo el rol Admin puede usarlas.
 */
@WebMvcTest(KafkaAdminProxyController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "TENANT_ID=00000000-0000-0000-0000-000000000000",
        "API_CLIENT_ID=00000000-0000-0000-0000-000000000000",
        "services.kafka-admin-url=http://kafka-admin-test:8086"
})
class KafkaAdminProxyControllerTest {

    private static final String ADMIN_URL = "http://kafka-admin-test:8086";
    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("Admin");
    private static final SimpleGrantedAuthority OPERADOR = new SimpleGrantedAuthority("Operador");

    /** El RestTemplate real de BffApplication (con el interceptor que reenvía el JWT). */
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RestTemplate restTemplate;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void createTopicForwardsBodyAndReturns201() throws Exception {
        String body = "{\"name\":\"pruebas.demo\",\"partitions\":3,\"replicationFactor\":3}";
        server.expect(requestTo(ADMIN_URL + "/api/kafka/topics"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(body))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(body));

        mockMvc.perform(post("/api/kafka/topics").with(jwt().authorities(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("pruebas.demo"));
        server.verify();
    }

    @Test
    void updateConfigsForwardsPutAndReturns204() throws Exception {
        server.expect(requestTo(ADMIN_URL + "/api/kafka/topics/audit.timeline/configs"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        mockMvc.perform(put("/api/kafka/topics/audit.timeline/configs").with(jwt().authorities(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"set\":{\"retention.ms\":\"1000\"}}"))
                .andExpect(status().isNoContent());
        server.verify();
    }

    @Test
    void deleteTopicForwardsDeleteAndReturns204() throws Exception {
        server.expect(requestTo(ADMIN_URL + "/api/kafka/topics/pruebas.demo"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        mockMvc.perform(delete("/api/kafka/topics/pruebas.demo").with(jwt().authorities(ADMIN)))
                .andExpect(status().isNoContent());
        server.verify();
    }

    @Test
    void downstreamConflictIsReturnedAsIs() throws Exception {
        server.expect(requestTo(ADMIN_URL + "/api/kafka/topics/shipments.events"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"PROTECTED_RESOURCE\",\"message\":\"protegido\"}"));

        mockMvc.perform(delete("/api/kafka/topics/shipments.events").with(jwt().authorities(ADMIN)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("PROTECTED_RESOURCE"));
    }

    @Test
    void partitionsResetAndLagAreForwarded() throws Exception {
        server.expect(requestTo(ADMIN_URL + "/api/kafka/topics/pruebas.demo/partitions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"partitions\":6}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ADMIN_URL + "/api/kafka/consumer-groups/ms-rutaexpress-report/reset"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"strategy\":\"EARLIEST\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ADMIN_URL + "/api/kafka/consumer-groups/ms-rutaexpress-report/lag"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"totalLag\":7}", MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/api/kafka/topics/pruebas.demo/partitions").with(jwt().authorities(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"totalPartitions\":6}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/kafka/consumer-groups/ms-rutaexpress-report/reset").with(jwt().authorities(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"strategy\":\"EARLIEST\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/kafka/consumer-groups/ms-rutaexpress-report/lag").with(jwt().authorities(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLag").value(7));
        server.verify();
    }

    @Test
    void queryParamsAreForwarded() throws Exception {
        server.expect(requestTo(ADMIN_URL + "/api/kafka/topics?includeInternal=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        server.expect(requestTo(ADMIN_URL + "/api/kafka/topics/audit.timeline/configs?onlyOverrides=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/api/kafka/topics").param("includeInternal", "true").with(jwt().authorities(ADMIN)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/kafka/topics/audit.timeline/configs").param("onlyOverrides", "true")
                        .with(jwt().authorities(ADMIN)))
                .andExpect(status().isOk());
        server.verify();
    }

    @Test
    void writeRoutesAreForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(post("/api/kafka/topics").with(jwt().authorities(OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/kafka/topics/x/configs").with(jwt().authorities(OPERADOR))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/kafka/consumer-groups/x").with(jwt().authorities(OPERADOR)))
                .andExpect(status().isForbidden());
        server.verify(); // no se llamó al administrador
    }

    @Test
    void routesRequireAToken() throws Exception {
        mockMvc.perform(get("/api/kafka/consumer-groups"))
                .andExpect(status().isUnauthorized());
    }
}
