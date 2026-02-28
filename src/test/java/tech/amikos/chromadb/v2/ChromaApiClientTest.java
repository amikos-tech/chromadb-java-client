package tech.amikos.chromadb.v2;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.reflect.TypeToken;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.*;

public class ChromaApiClientTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().dynamicPort());

    private ChromaApiClient client;

    @After
    public void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    private ChromaApiClient newClient() {
        return newClient(null, null);
    }

    private ChromaApiClient newClient(AuthProvider auth) {
        return newClient(auth, null);
    }

    private ChromaApiClient newClient(AuthProvider auth, Map<String, String> defaultHeaders) {
        client = new ChromaApiClient(
                "http://localhost:" + wireMock.port(),
                auth,
                defaultHeaders,
                Duration.ofSeconds(5),
                Duration.ofSeconds(5),
                Duration.ofSeconds(5)
        );
        return client;
    }

    // --- Happy path: GET ---

    @Test
    public void testGetReturnsDeserializedObject() {
        stubFor(get(urlEqualTo("/api/v2/heartbeat"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"nanosecond heartbeat\":12345}")));

        ChromaApiClient c = newClient();
        Type type = new TypeToken<Map<String, Long>>() {}.getType();
        Map<String, Long> result = c.get("/api/v2/heartbeat", type);

        assertEquals(Long.valueOf(12345), result.get("nanosecond heartbeat"));
    }

    @Test
    public void testGetWithQueryParams() {
        stubFor(get(urlPathEqualTo("/api/v2/items"))
                .withQueryParam("limit", equalTo("10"))
                .withQueryParam("offset", equalTo("5"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[\"a\",\"b\"]")));

        ChromaApiClient c = newClient();
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put("limit", "10");
        params.put("offset", "5");
        Type type = new TypeToken<java.util.List<String>>() {}.getType();
        java.util.List<String> result = c.get("/api/v2/items", params, type);

        assertEquals(2, result.size());
        assertEquals("a", result.get(0));
    }

    // --- Happy path: POST ---

    @Test
    public void testPostWithBodyAndResponse() {
        stubFor(post(urlEqualTo("/api/v2/tenants"))
                .withRequestBody(equalToJson("{\"name\":\"my_tenant\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"my_tenant\"}")));

        ChromaApiClient c = newClient();
        Map<String, String> body = Collections.singletonMap("name", "my_tenant");
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> result = c.post("/api/v2/tenants", body, type);

        assertEquals("my_tenant", result.get("name"));
    }

    @Test
    public void testVoidPost() {
        stubFor(post(urlEqualTo("/api/v2/collections/id1/add"))
                .willReturn(aResponse().withStatus(200)));

        ChromaApiClient c = newClient();
        Map<String, String> body = Collections.singletonMap("ids", "id1");
        c.post("/api/v2/collections/id1/add", body); // no exception = pass
    }

    // --- Happy path: PUT ---

    @Test
    public void testPutWithBodyAndResponse() {
        stubFor(put(urlEqualTo("/api/v2/collections/id1"))
                .withRequestBody(equalToJson("{\"name\":\"renamed\"}"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"name\":\"renamed\"}")));

        ChromaApiClient c = newClient();
        Map<String, String> body = Collections.singletonMap("name", "renamed");
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> result = c.put("/api/v2/collections/id1", body, type);

        assertEquals("renamed", result.get("name"));
    }

    // --- Happy path: DELETE ---

    @Test
    public void testDeleteSuccess() {
        stubFor(delete(urlEqualTo("/api/v2/collections/id1"))
                .willReturn(aResponse().withStatus(200)));

        ChromaApiClient c = newClient();
        c.delete("/api/v2/collections/id1"); // no exception = pass
    }

    @Test
    public void testDelete204() {
        stubFor(delete(urlEqualTo("/api/v2/collections/id1"))
                .willReturn(aResponse().withStatus(204)));

        ChromaApiClient c = newClient();
        c.delete("/api/v2/collections/id1"); // no exception = pass
    }

    // --- Error mapping ---

    @Test
    public void testError400MapsToBadRequest() {
        stubFor(get(urlEqualTo("/api/v2/test"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"invalid input\"}")));

        ChromaApiClient c = newClient();
        try {
            c.get("/api/v2/test", String.class);
            fail("Expected ChromaBadRequestException");
        } catch (ChromaBadRequestException e) {
            assertEquals(400, e.getStatusCode());
            assertEquals("invalid input", e.getMessage());
        }
    }

    @Test
    public void testError401MapsToUnauthorized() {
        stubFor(get(urlEqualTo("/api/v2/test"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"bad token\"}")));

        ChromaApiClient c = newClient();
        try {
            c.get("/api/v2/test", String.class);
            fail("Expected ChromaUnauthorizedException");
        } catch (ChromaUnauthorizedException e) {
            assertEquals(401, e.getStatusCode());
        }
    }

    @Test
    public void testError403MapsToForbidden() {
        stubFor(get(urlEqualTo("/api/v2/test"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"access denied\"}")));

        ChromaApiClient c = newClient();
        try {
            c.get("/api/v2/test", String.class);
            fail("Expected ChromaForbiddenException");
        } catch (ChromaForbiddenException e) {
            assertEquals(403, e.getStatusCode());
        }
    }

    @Test
    public void testError404MapsToNotFound() {
        stubFor(get(urlEqualTo("/api/v2/test"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"not found\",\"error_code\":\"NotFoundError\"}")));

        ChromaApiClient c = newClient();
        try {
            c.get("/api/v2/test", String.class);
            fail("Expected ChromaNotFoundException");
        } catch (ChromaNotFoundException e) {
            assertEquals(404, e.getStatusCode());
            assertEquals("NotFoundError", e.getErrorCode());
        }
    }

    @Test
    public void testError409MapsToConflict() {
        stubFor(get(urlEqualTo("/api/v2/test"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"already exists\"}")));

        ChromaApiClient c = newClient();
        try {
            c.get("/api/v2/test", String.class);
            fail("Expected ChromaConflictException");
        } catch (ChromaConflictException e) {
            assertEquals(409, e.getStatusCode());
        }
    }

    @Test
    public void testError500MapsToServerException() {
        stubFor(get(urlEqualTo("/api/v2/test"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"internal error\"}")));

        ChromaApiClient c = newClient();
        try {
            c.get("/api/v2/test", String.class);
            fail("Expected ChromaServerException");
        } catch (ChromaServerException e) {
            assertEquals(500, e.getStatusCode());
            assertEquals("internal error", e.getMessage());
        }
    }

    @Test
    public void testNonJsonErrorBodyFallsBackToHttpStatus() {
        stubFor(get(urlEqualTo("/api/v2/test"))
                .willReturn(aResponse()
                        .withStatus(502)
                        .withBody("Bad Gateway")));

        ChromaApiClient c = newClient();
        try {
            c.get("/api/v2/test", String.class);
            fail("Expected ChromaServerException");
        } catch (ChromaServerException e) {
            assertEquals(502, e.getStatusCode());
            assertEquals("HTTP 502", e.getMessage());
        }
    }

    @Test
    public void testEmptyErrorBodyFallsBackToHttpStatus() {
        stubFor(get(urlEqualTo("/api/v2/test"))
                .willReturn(aResponse()
                        .withStatus(500)));

        ChromaApiClient c = newClient();
        try {
            c.get("/api/v2/test", String.class);
            fail("Expected ChromaServerException");
        } catch (ChromaServerException e) {
            assertEquals(500, e.getStatusCode());
            assertEquals("HTTP 500", e.getMessage());
        }
    }

    @Test
    public void testErrorWithMessageFieldInsteadOfError() {
        stubFor(get(urlEqualTo("/api/v2/test"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"validation failed\"}")));

        ChromaApiClient c = newClient();
        try {
            c.get("/api/v2/test", String.class);
            fail("Expected ChromaBadRequestException");
        } catch (ChromaBadRequestException e) {
            assertEquals("validation failed", e.getMessage());
        }
    }

    // --- Connection errors ---

    @Test
    public void testConnectionErrorMapsToConnectionException() {
        // Use a port that's not listening
        client = new ChromaApiClient(
                "http://localhost:1",
                null, null,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1)
        );

        try {
            client.get("/api/v2/test", String.class);
            fail("Expected ChromaConnectionException");
        } catch (ChromaConnectionException e) {
            assertNotNull(e.getCause());
        }
    }

    // --- Auth injection ---

    @Test
    public void testTokenAuthSendsBearerHeader() {
        stubFor(get(urlEqualTo("/api/v2/test"))
                .withHeader("Authorization", equalTo("Bearer my-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("\"ok\"")));

        ChromaApiClient c = newClient(TokenAuth.of("my-token"));
        String result = c.get("/api/v2/test", String.class);
        assertEquals("ok", result);
    }

    @Test
    public void testBasicAuthSendsBasicHeader() {
        stubFor(get(urlEqualTo("/api/v2/test"))
                .withHeader("Authorization", matching("Basic .*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("\"ok\"")));

        ChromaApiClient c = newClient(BasicAuth.of("user", "pass"));
        String result = c.get("/api/v2/test", String.class);
        assertEquals("ok", result);
    }

    @Test
    public void testChromaTokenAuthSendsXChromaTokenHeader() {
        stubFor(get(urlEqualTo("/api/v2/test"))
                .withHeader("X-Chroma-Token", equalTo("ck-token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("\"ok\"")));

        ChromaApiClient c = newClient(ChromaTokenAuth.of("ck-token"));
        String result = c.get("/api/v2/test", String.class);
        assertEquals("ok", result);
    }

    @Test
    public void testNullAuthSendsNoAuthHeader() {
        stubFor(get(urlEqualTo("/api/v2/test"))
                .withHeader("Authorization", absent())
                .withHeader("X-Chroma-Token", absent())
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("\"ok\"")));

        ChromaApiClient c = newClient(null);
        String result = c.get("/api/v2/test", String.class);
        assertEquals("ok", result);
    }

    // --- Headers ---

    @Test
    public void testDefaultHeadersSentOnEveryRequest() {
        stubFor(get(urlEqualTo("/api/v2/test"))
                .withHeader("X-Custom", equalTo("custom-value"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("\"ok\"")));

        Map<String, String> headers = Collections.singletonMap("X-Custom", "custom-value");
        ChromaApiClient c = newClient(null, headers);
        String result = c.get("/api/v2/test", String.class);
        assertEquals("ok", result);
    }

    @Test
    public void testUserAgentHeader() {
        stubFor(get(urlEqualTo("/api/v2/test"))
                .withHeader("User-Agent", equalTo("chroma-java-client"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("\"ok\"")));

        ChromaApiClient c = newClient();
        String result = c.get("/api/v2/test", String.class);
        assertEquals("ok", result);
    }

    @Test
    public void testAcceptJsonHeader() {
        stubFor(get(urlEqualTo("/api/v2/test"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("\"ok\"")));

        ChromaApiClient c = newClient();
        String result = c.get("/api/v2/test", String.class);
        assertEquals("ok", result);
    }

    // --- Trailing slash stripped ---

    @Test
    public void testTrailingSlashStripped() {
        stubFor(get(urlEqualTo("/api/v2/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("\"ok\"")));

        client = new ChromaApiClient(
                "http://localhost:" + wireMock.port() + "/",
                null, null,
                Duration.ofSeconds(5),
                Duration.ofSeconds(5),
                Duration.ofSeconds(5)
        );
        String result = client.get("/api/v2/test", String.class);
        assertEquals("ok", result);
    }

    // --- Gson accessor ---

    @Test
    public void testGsonAccessor() {
        ChromaApiClient c = newClient();
        assertNotNull(c.gson());
    }

    // --- Constructor validation ---

    @Test(expected = IllegalArgumentException.class)
    public void testNullBaseUrlThrows() {
        new ChromaApiClient(null, null, null, null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBlankBaseUrlThrows() {
        new ChromaApiClient("   ", null, null, null, null, null);
    }
}
