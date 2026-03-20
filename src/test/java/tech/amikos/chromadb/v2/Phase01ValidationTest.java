package tech.amikos.chromadb.v2;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.*;

/**
 * Nyquist validation tests for Phase 1: Transport and Auth Hardening.
 *
 * <p>These tests verify the behavioral requirements of the phase from a
 * user-observable perspective:
 * <ul>
 *   <li>AUTH-01: Any supported auth provider configured via the builder is honored on every client request.</li>
 *   <li>AUTH-02: Cloud preflight/identity flows return typed results or typed exceptions.</li>
 *   <li>AUTH-03: Auth misconfiguration yields actionable validation errors, never silent fallback.</li>
 *   <li>API-04: HTTP error responses map to the correct ChromaException subclass with metadata.</li>
 * </ul>
 */
public class Phase01ValidationTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule(wireMockConfig().dynamicPort());

    private Client client;

    @After
    public void tearDown() {
        if (client != null) {
            client.close();
            client = null;
        }
    }

    private String baseUrl() {
        return "http://localhost:" + wireMock.port();
    }

    // =====================================================================
    // AUTH-01: User can configure auth and have it honored on client calls
    // =====================================================================

    @Test
    public void testUserCanConfigureTokenAuthAndHeartbeatHonorsIt() {
        stubFor(get(urlEqualTo("/api/v2/heartbeat"))
                .withHeader("Authorization", equalTo("Bearer user-tok-123"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"nanosecond heartbeat\":99}")));

        client = ChromaClient.builder()
                .baseUrl(baseUrl())
                .auth(TokenAuth.of("user-tok-123"))
                .build();

        String heartbeat = client.heartbeat();
        assertEquals("99", heartbeat);

        // Verify the auth header was actually sent
        verify(getRequestedFor(urlEqualTo("/api/v2/heartbeat"))
                .withHeader("Authorization", equalTo("Bearer user-tok-123")));
    }

    @Test
    public void testUserCanConfigureBasicAuthAndVersionEndpointHonorsIt() {
        stubFor(get(urlEqualTo("/api/v2/version"))
                .withHeader("Authorization", matching("Basic .*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("\"2.0.0\"")));

        client = ChromaClient.builder()
                .baseUrl(baseUrl())
                .auth(BasicAuth.of("admin", "secret"))
                .build();

        String version = client.version();
        assertEquals("2.0.0", version);

        verify(getRequestedFor(urlEqualTo("/api/v2/version"))
                .withHeader("Authorization", matching("Basic .*")));
    }

    @Test
    public void testUserCanConfigureChromaTokenAuthAndCollectionListHonorsIt() {
        stubFor(get(urlPathEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections"))
                .withHeader("X-Chroma-Token", equalTo("ck-my-cloud-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        client = ChromaClient.builder()
                .baseUrl(baseUrl())
                .auth(ChromaTokenAuth.of("ck-my-cloud-key"))
                .build();

        client.listCollections();

        verify(getRequestedFor(urlPathEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections"))
                .withHeader("X-Chroma-Token", equalTo("ck-my-cloud-key")));
    }

    @Test
    public void testUserCanConfigureApiKeyShortcutAndItSendsBearerAuth() {
        stubFor(get(urlEqualTo("/api/v2/heartbeat"))
                .withHeader("Authorization", equalTo("Bearer my-api-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"nanosecond heartbeat\":1}")));

        client = ChromaClient.builder()
                .baseUrl(baseUrl())
                .apiKey("my-api-key")
                .build();

        client.heartbeat();

        verify(getRequestedFor(urlEqualTo("/api/v2/heartbeat"))
                .withHeader("Authorization", equalTo("Bearer my-api-key")));
    }

    @Test
    public void testAuthIsHonoredAcrossMultipleClientOperations() {
        // Auth should apply to every request, not just the first
        stubFor(get(urlEqualTo("/api/v2/heartbeat"))
                .withHeader("Authorization", equalTo("Bearer persistent-tok"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"nanosecond heartbeat\":1}")));

        stubFor(get(urlEqualTo("/api/v2/version"))
                .withHeader("Authorization", equalTo("Bearer persistent-tok"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("\"1.0.0\"")));

        client = ChromaClient.builder()
                .baseUrl(baseUrl())
                .auth(TokenAuth.of("persistent-tok"))
                .build();

        client.heartbeat();
        client.version();

        verify(getRequestedFor(urlEqualTo("/api/v2/heartbeat"))
                .withHeader("Authorization", equalTo("Bearer persistent-tok")));
        verify(getRequestedFor(urlEqualTo("/api/v2/version"))
                .withHeader("Authorization", equalTo("Bearer persistent-tok")));
    }

    // =====================================================================
    // AUTH-02: Cloud preflight/identity flows are strict and typed
    // =====================================================================

    @Test
    public void testPreFlightUnauthorizedReturnsTypedExceptionNotFallback() {
        stubFor(get(urlEqualTo("/api/v2/pre-flight-checks"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"invalid token\"}")));

        client = ChromaClient.builder()
                .baseUrl(baseUrl())
                .build();

        try {
            client.preFlight();
            fail("Expected ChromaUnauthorizedException for 401 on preFlight");
        } catch (ChromaUnauthorizedException e) {
            assertEquals(401, e.getStatusCode());
            assertTrue("Message should reference the endpoint",
                    e.getMessage().contains("/api/v2/pre-flight-checks"));
            assertTrue("Message should contain actionable hint",
                    e.getMessage().contains("Verify your Chroma credentials"));
        }
    }

    @Test
    public void testIdentityForbiddenReturnsTypedExceptionNotFallback() {
        stubFor(get(urlEqualTo("/api/v2/auth/identity"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"access denied\"}")));

        client = ChromaClient.builder()
                .baseUrl(baseUrl())
                .build();

        try {
            client.getIdentity();
            fail("Expected ChromaForbiddenException for 403 on getIdentity");
        } catch (ChromaForbiddenException e) {
            assertEquals(403, e.getStatusCode());
            assertTrue("Message should reference the endpoint",
                    e.getMessage().contains("/api/v2/auth/identity"));
            assertTrue("Message should contain actionable hint",
                    e.getMessage().contains("Verify your Chroma credentials"));
        }
    }

    @Test
    public void testPreFlightMalformedPayloadThrowsDeserializationWithEndpointContext() {
        stubFor(get(urlEqualTo("/api/v2/pre-flight-checks"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"something_else\":true}")));

        client = ChromaClient.builder()
                .baseUrl(baseUrl())
                .build();

        try {
            client.preFlight();
            fail("Expected ChromaDeserializationException for malformed preFlight response");
        } catch (ChromaDeserializationException e) {
            assertTrue("Message should include endpoint",
                    e.getMessage().contains("/api/v2/pre-flight-checks"));
            assertTrue("Message should include missing field name",
                    e.getMessage().contains("max_batch_size"));
        }
    }

    @Test
    public void testIdentityMalformedPayloadThrowsDeserializationWithFieldContext() {
        stubFor(get(urlEqualTo("/api/v2/auth/identity"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"tenant\":\"t\",\"databases\":[\"db1\"]}")));

        client = ChromaClient.builder()
                .baseUrl(baseUrl())
                .build();

        try {
            client.getIdentity();
            fail("Expected ChromaDeserializationException for missing user_id");
        } catch (ChromaDeserializationException e) {
            assertTrue("Message should include endpoint",
                    e.getMessage().contains("/api/v2/auth/identity"));
            assertTrue("Message should identify the missing field",
                    e.getMessage().contains("user_id"));
        }
    }

    // =====================================================================
    // AUTH-03: Auth misconfiguration yields actionable validation errors
    // =====================================================================

    @Test
    public void testSecondAuthStrategyOnBuilderFailsWithActionableMessage() {
        try {
            ChromaClient.builder()
                    .auth(TokenAuth.of("first"))
                    .apiKey("second");
            fail("Expected IllegalStateException when setting a second auth strategy");
        } catch (IllegalStateException e) {
            assertTrue("Error message must say 'exactly one auth strategy'",
                    e.getMessage().contains("exactly one auth strategy"));
            assertTrue("Error message must mention auth(...) for guidance",
                    e.getMessage().contains("auth(...)"));
        }
    }

    @Test
    public void testCloudBuilderSecondApiKeyFailsWithActionableMessage() {
        try {
            ChromaClient.cloud()
                    .apiKey("first-key")
                    .apiKey("second-key");
            fail("Expected IllegalStateException on second cloud apiKey call");
        } catch (IllegalStateException e) {
            assertTrue("Error message must say 'exactly one auth strategy'",
                    e.getMessage().contains("exactly one auth strategy"));
        }
    }

    @Test
    public void testReservedAuthHeaderInDefaultHeadersFailsWithGuidance() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer leaked");

        try {
            ChromaClient.builder().defaultHeaders(headers);
            fail("Expected IllegalArgumentException for reserved auth header");
        } catch (IllegalArgumentException e) {
            assertTrue("Error message must guide user to auth(...)",
                    e.getMessage().contains("auth(...)"));
        }
    }

    @Test
    public void testReservedXChromaTokenHeaderInDefaultHeadersFailsWithGuidance() {
        Map<String, String> headers = new LinkedHashMap<String, String>();
        headers.put("X-Chroma-Token", "leaked");

        try {
            ChromaClient.builder().defaultHeaders(headers);
            fail("Expected IllegalArgumentException for X-Chroma-Token in defaultHeaders");
        } catch (IllegalArgumentException e) {
            assertTrue("Error message must guide user to auth(...)",
                    e.getMessage().contains("auth(...)"));
        }
    }

    @Test
    public void testNullTokenAuthRejectedWithClearException() {
        try {
            TokenAuth.of(null);
            fail("Expected NullPointerException for null token");
        } catch (NullPointerException e) {
            // Expected
        }
    }

    @Test
    public void testBlankTokenAuthRejectedWithActionableMessage() {
        try {
            TokenAuth.of("   ");
            fail("Expected IllegalArgumentException for blank token");
        } catch (IllegalArgumentException e) {
            assertTrue("Message should mention 'token'",
                    e.getMessage().contains("token"));
            assertTrue("Message should say 'must not be blank'",
                    e.getMessage().contains("must not be blank"));
        }
    }

    @Test
    public void testMissingEnvVarTokenAuthFailsWithVariableName() {
        String varName = "PHASE01_VALIDATION_MISSING_" + System.nanoTime();
        try {
            TokenAuth.fromEnv(varName);
            fail("Expected IllegalStateException for missing env var");
        } catch (IllegalStateException e) {
            assertTrue("Message should mention the variable name",
                    e.getMessage().contains(varName));
        }
    }

    @Test
    public void testCloudBuilderMissingRequiredFieldsFailsAtBuildTime() {
        // Missing apiKey
        try {
            ChromaClient.cloud()
                    .tenant("t")
                    .database("d")
                    .build();
            fail("Expected IllegalStateException for missing apiKey");
        } catch (IllegalStateException e) {
            // Expected
        }

        // Missing tenant
        try {
            ChromaClient.cloud()
                    .apiKey("k")
                    .database("d")
                    .build();
            fail("Expected IllegalStateException for missing tenant");
        } catch (IllegalStateException e) {
            // Expected
        }

        // Missing database
        try {
            ChromaClient.cloud()
                    .apiKey("k")
                    .tenant("t")
                    .build();
            fail("Expected IllegalStateException for missing database");
        } catch (IllegalStateException e) {
            // Expected
        }
    }

    // =====================================================================
    // API-04: HTTP errors map to correct ChromaException subclasses with metadata
    // =====================================================================

    @Test
    public void testClientLevelCallMaps400ToBadRequest() {
        stubFor(get(urlPathEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections/bad-col"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"invalid collection name\"}")));

        client = ChromaClient.builder()
                .baseUrl(baseUrl())
                .build();

        try {
            client.getCollection("bad-col");
            fail("Expected ChromaBadRequestException");
        } catch (ChromaBadRequestException e) {
            assertEquals(400, e.getStatusCode());
            assertEquals("invalid collection name", e.getMessage());
        }
    }

    @Test
    public void testClientLevelCallMaps404ToNotFoundWithErrorCode() {
        stubFor(get(urlPathEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections/missing"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Collection missing not found\",\"error_code\":\"NotFoundError\"}")));

        client = ChromaClient.builder()
                .baseUrl(baseUrl())
                .build();

        try {
            client.getCollection("missing");
            fail("Expected ChromaNotFoundException");
        } catch (ChromaNotFoundException e) {
            assertEquals(404, e.getStatusCode());
            assertEquals("NotFoundError", e.getErrorCode());
            assertTrue(e instanceof ChromaClientException);
        }
    }

    @Test
    public void testClientLevelCallMaps409ToConflict() {
        stubFor(post(urlPathEqualTo("/api/v2/tenants/default_tenant/databases/default_database/collections"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"already exists\"}")));

        client = ChromaClient.builder()
                .baseUrl(baseUrl())
                .build();

        try {
            client.createCollection("existing_col");
            fail("Expected ChromaConflictException");
        } catch (ChromaConflictException e) {
            assertEquals(409, e.getStatusCode());
            assertTrue(e instanceof ChromaClientException);
        }
    }

    @Test
    public void testClientLevelCallMaps500ToServerException() {
        stubFor(get(urlEqualTo("/api/v2/heartbeat"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"internal server error\"}")));

        client = ChromaClient.builder()
                .baseUrl(baseUrl())
                .build();

        try {
            client.heartbeat();
            fail("Expected ChromaServerException");
        } catch (ChromaServerException e) {
            assertEquals(500, e.getStatusCode());
            assertEquals("internal server error", e.getMessage());
        }
    }

    @Test
    public void testClientLevelCallMaps401ToUnauthorized() {
        stubFor(get(urlEqualTo("/api/v2/heartbeat"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"bad token\"}")));

        client = ChromaClient.builder()
                .baseUrl(baseUrl())
                .build();

        try {
            client.heartbeat();
            fail("Expected ChromaUnauthorizedException");
        } catch (ChromaUnauthorizedException e) {
            assertEquals(401, e.getStatusCode());
        }
    }

    @Test
    public void testNonJsonErrorFallsBackToDeterministicFormat() {
        stubFor(get(urlEqualTo("/api/v2/heartbeat"))
                .willReturn(aResponse()
                        .withStatus(502)
                        .withBody("Bad Gateway")));

        client = ChromaClient.builder()
                .baseUrl(baseUrl())
                .build();

        try {
            client.heartbeat();
            fail("Expected ChromaServerException");
        } catch (ChromaServerException e) {
            assertEquals(502, e.getStatusCode());
            assertEquals("HTTP 502: Bad Gateway", e.getMessage());
        }
    }

    @Test
    public void testErrorCodePreservedThroughClientCall() {
        stubFor(get(urlEqualTo("/api/v2/heartbeat"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"server timeout\",\"error_code\":\"ProxyTimeout\"}")));

        client = ChromaClient.builder()
                .baseUrl(baseUrl())
                .build();

        try {
            client.heartbeat();
            fail("Expected ChromaServerException");
        } catch (ChromaServerException e) {
            assertEquals(500, e.getStatusCode());
            assertEquals("ProxyTimeout", e.getErrorCode());
            assertEquals("server timeout", e.getMessage());
        }
    }

    @Test
    public void testConnectionErrorMapsToConnectionExceptionWithEndpointContext() {
        client = ChromaClient.builder()
                .baseUrl("http://localhost:1")
                .connectTimeout(Duration.ofSeconds(1))
                .build();

        try {
            client.heartbeat();
            fail("Expected ChromaConnectionException");
        } catch (ChromaConnectionException e) {
            assertTrue("Message should reference endpoint",
                    e.getMessage().contains("/api/v2/heartbeat"));
            assertTrue("Message should describe network error",
                    e.getMessage().contains("Network error"));
            assertNotNull("Should preserve root cause", e.getCause());
            // Must not leak auth headers
            assertFalse("Must not leak Authorization header",
                    e.getMessage().contains("Authorization"));
            assertFalse("Must not leak X-Chroma-Token header",
                    e.getMessage().contains("X-Chroma-Token"));
        }
    }

    @Test
    public void testExceptionHierarchyIsRuntimeBased() {
        // All ChromaException subclasses extend RuntimeException for unchecked semantics
        assertTrue("ChromaException must be a RuntimeException",
                RuntimeException.class.isAssignableFrom(ChromaException.class));
        assertTrue("ChromaClientException must extend ChromaException",
                ChromaException.class.isAssignableFrom(ChromaClientException.class));
        assertTrue("ChromaServerException must extend ChromaException",
                ChromaException.class.isAssignableFrom(ChromaServerException.class));
        assertTrue("ChromaConnectionException must extend ChromaException",
                ChromaException.class.isAssignableFrom(ChromaConnectionException.class));
        assertTrue("ChromaDeserializationException must extend ChromaException",
                ChromaException.class.isAssignableFrom(ChromaDeserializationException.class));

        // Specific subtypes
        assertTrue("ChromaBadRequestException must extend ChromaClientException",
                ChromaClientException.class.isAssignableFrom(ChromaBadRequestException.class));
        assertTrue("ChromaUnauthorizedException must extend ChromaClientException",
                ChromaClientException.class.isAssignableFrom(ChromaUnauthorizedException.class));
        assertTrue("ChromaForbiddenException must extend ChromaClientException",
                ChromaClientException.class.isAssignableFrom(ChromaForbiddenException.class));
        assertTrue("ChromaNotFoundException must extend ChromaClientException",
                ChromaClientException.class.isAssignableFrom(ChromaNotFoundException.class));
        assertTrue("ChromaConflictException must extend ChromaClientException",
                ChromaClientException.class.isAssignableFrom(ChromaConflictException.class));
    }

    @Test
    public void testFactoryMappingCoversAllDocumentedStatusCodes() {
        // 400 -> BadRequest
        assertTrue(ChromaExceptions.fromHttpResponse(400, "bad", null)
                instanceof ChromaBadRequestException);
        // 401 -> Unauthorized
        assertTrue(ChromaExceptions.fromHttpResponse(401, "unauth", null)
                instanceof ChromaUnauthorizedException);
        // 403 -> Forbidden
        assertTrue(ChromaExceptions.fromHttpResponse(403, "forbidden", null)
                instanceof ChromaForbiddenException);
        // 404 -> NotFound
        assertTrue(ChromaExceptions.fromHttpResponse(404, "missing", null)
                instanceof ChromaNotFoundException);
        // 409 -> Conflict
        assertTrue(ChromaExceptions.fromHttpResponse(409, "conflict", null)
                instanceof ChromaConflictException);
        // Unknown 4xx -> ClientException
        ChromaException ex422 = ChromaExceptions.fromHttpResponse(422, "unprocessable", null);
        assertTrue(ex422 instanceof ChromaClientException);
        assertFalse(ex422 instanceof ChromaBadRequestException);
        // 5xx -> ServerException
        assertTrue(ChromaExceptions.fromHttpResponse(500, "error", null)
                instanceof ChromaServerException);
        assertTrue(ChromaExceptions.fromHttpResponse(503, "unavailable", null)
                instanceof ChromaServerException);
    }
}
