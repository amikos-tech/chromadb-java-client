package tech.amikos.chromadb.v2;

import org.junit.Test;

import static org.junit.Assert.*;

public class ChromaExceptionTest {

    @Test
    public void testBaseExceptionFields() {
        ChromaException ex = new ChromaException("test error", 500, "ServerError");
        assertEquals("test error", ex.getMessage());
        assertEquals(500, ex.getStatusCode());
        assertEquals("ServerError", ex.getErrorCode());
        assertNull(ex.getCause());
    }

    @Test
    public void testBaseExceptionWithCause() {
        Throwable cause = new RuntimeException("root cause");
        ChromaException ex = new ChromaException("wrapped", 400, "BadRequest", cause);
        assertEquals("wrapped", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void testBaseExceptionDefaultStatusCode() {
        ChromaException ex = new ChromaException("simple error");
        assertEquals(-1, ex.getStatusCode());
        assertNull(ex.getErrorCode());
    }

    @Test
    public void testBadRequestExceptionStatusCode() {
        ChromaBadRequestException ex = new ChromaBadRequestException("bad", "ValidationError");
        assertEquals(400, ex.getStatusCode());
        assertEquals("ValidationError", ex.getErrorCode());
        assertTrue(ex instanceof ChromaClientException);
    }

    @Test
    public void testUnauthorizedExceptionStatusCode() {
        ChromaUnauthorizedException ex = new ChromaUnauthorizedException("unauth", "AuthError");
        assertEquals(401, ex.getStatusCode());
        assertTrue(ex instanceof ChromaClientException);
    }

    @Test
    public void testForbiddenExceptionStatusCode() {
        ChromaForbiddenException ex = new ChromaForbiddenException("forbidden", "ForbiddenError");
        assertEquals(403, ex.getStatusCode());
        assertTrue(ex instanceof ChromaClientException);
    }

    @Test
    public void testNotFoundExceptionStatusCode() {
        ChromaNotFoundException ex = new ChromaNotFoundException("not found", "NotFoundError");
        assertEquals(404, ex.getStatusCode());
        assertTrue(ex instanceof ChromaClientException);
    }

    @Test
    public void testNotFoundExceptionDefaultErrorCode() {
        ChromaNotFoundException ex = new ChromaNotFoundException("missing");
        assertEquals(404, ex.getStatusCode());
        assertEquals("NotFoundError", ex.getErrorCode());
    }

    @Test
    public void testConflictExceptionStatusCode() {
        ChromaConflictException ex = new ChromaConflictException("conflict", "UniqueConstraintError");
        assertEquals(409, ex.getStatusCode());
        assertTrue(ex instanceof ChromaClientException);
    }

    @Test
    public void testServerExceptionStatusCode() {
        ChromaServerException ex = new ChromaServerException("server error", 503, "ServiceUnavailable");
        assertEquals(503, ex.getStatusCode());
        assertTrue(ex instanceof ChromaException);
    }

    @Test
    public void testConnectionExceptionPreservesCause() {
        Throwable cause = new java.net.ConnectException("Connection refused");
        ChromaConnectionException ex = new ChromaConnectionException("Cannot connect", cause);
        assertSame(cause, ex.getCause());
        assertEquals(-1, ex.getStatusCode());
        assertTrue(ex instanceof ChromaException);
    }

    // --- Cause constructor tests ---

    @Test
    public void testBadRequestWithCause() {
        Throwable cause = new RuntimeException("parse error");
        ChromaBadRequestException ex = new ChromaBadRequestException("bad", "ValidationError", cause);
        assertEquals(400, ex.getStatusCode());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void testUnauthorizedWithCause() {
        Throwable cause = new RuntimeException("token expired");
        ChromaUnauthorizedException ex = new ChromaUnauthorizedException("unauth", "AuthError", cause);
        assertEquals(401, ex.getStatusCode());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void testNotFoundWithCause() {
        Throwable cause = new RuntimeException("json error");
        ChromaNotFoundException ex = new ChromaNotFoundException("not found", "NotFoundError", cause);
        assertEquals(404, ex.getStatusCode());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void testClientExceptionWithCause() {
        Throwable cause = new RuntimeException("parsing failed");
        ChromaClientException ex = new ChromaClientException("error", 422, "ValidationError", cause);
        assertEquals(422, ex.getStatusCode());
        assertSame(cause, ex.getCause());
    }

    @Test
    public void testServerExceptionWithCause() {
        Throwable cause = new RuntimeException("timeout");
        ChromaServerException ex = new ChromaServerException("error", 502, "GatewayError", cause);
        assertEquals(502, ex.getStatusCode());
        assertSame(cause, ex.getCause());
    }

    // --- instanceof hierarchy tests ---

    @Test
    public void testExceptionHierarchy() {
        ChromaException badRequest = new ChromaBadRequestException("bad", null);
        assertTrue(badRequest instanceof ChromaClientException);
        assertTrue(badRequest instanceof ChromaException);
        assertTrue(badRequest instanceof RuntimeException);

        ChromaException server = new ChromaServerException("err", 500, null);
        assertFalse(server instanceof ChromaClientException);
        assertTrue(server instanceof ChromaException);

        ChromaException conn = new ChromaConnectionException("err", null);
        assertFalse(conn instanceof ChromaClientException);
        assertFalse(conn instanceof ChromaServerException);
        assertTrue(conn instanceof ChromaException);
    }

    // --- Factory method tests ---

    @Test
    public void testFactoryMaps400() {
        ChromaException ex = ChromaExceptions.fromHttpResponse(400, "bad request", "ValidationError");
        assertTrue(ex instanceof ChromaBadRequestException);
        assertEquals(400, ex.getStatusCode());
    }

    @Test
    public void testFactoryMaps401() {
        ChromaException ex = ChromaExceptions.fromHttpResponse(401, "unauthorized", "AuthError");
        assertTrue(ex instanceof ChromaUnauthorizedException);
    }

    @Test
    public void testFactoryMaps403() {
        ChromaException ex = ChromaExceptions.fromHttpResponse(403, "forbidden", null);
        assertTrue(ex instanceof ChromaForbiddenException);
    }

    @Test
    public void testFactoryMaps404() {
        ChromaException ex = ChromaExceptions.fromHttpResponse(404, "not found", "NotFoundError");
        assertTrue(ex instanceof ChromaNotFoundException);
    }

    @Test
    public void testFactoryMaps404WithNullErrorCodeUsesDefault() {
        ChromaException ex = ChromaExceptions.fromHttpResponse(404, "not found", null);
        assertTrue(ex instanceof ChromaNotFoundException);
        assertEquals("NotFoundError", ex.getErrorCode());
    }

    @Test
    public void testFactoryMaps409() {
        ChromaException ex = ChromaExceptions.fromHttpResponse(409, "conflict", null);
        assertTrue(ex instanceof ChromaConflictException);
    }

    @Test
    public void testFactoryMapsUnknown4xxToClientException() {
        ChromaException ex = ChromaExceptions.fromHttpResponse(422, "unprocessable", "ValidationError");
        assertTrue(ex instanceof ChromaClientException);
        assertFalse(ex instanceof ChromaBadRequestException);
        assertEquals(422, ex.getStatusCode());
    }

    @Test
    public void testFactoryMaps5xxToServerException() {
        ChromaException ex = ChromaExceptions.fromHttpResponse(503, "unavailable", null);
        assertTrue(ex instanceof ChromaServerException);
        assertEquals(503, ex.getStatusCode());
    }

    @Test
    public void testFactoryWithCausePreservesCause() {
        Throwable cause = new RuntimeException("json parse error");
        ChromaException ex = ChromaExceptions.fromHttpResponse(404, "not found", null, cause);
        assertTrue(ex instanceof ChromaNotFoundException);
        assertEquals("NotFoundError", ex.getErrorCode());
        assertSame(cause, ex.getCause());
    }
}
