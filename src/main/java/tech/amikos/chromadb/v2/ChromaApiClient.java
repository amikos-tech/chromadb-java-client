package tech.amikos.chromadb.v2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import tech.amikos.chromadb.Constants;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Package-private HTTP transport for the Chroma v2 REST API.
 * Owns the {@link OkHttpClient} and {@link Gson} instances and must be closed
 * to release HTTP thread and connection-pool resources.
 */
class ChromaApiClient implements AutoCloseable {

    private static final int MAX_BODY_SNIPPET_LENGTH = 200;

    private final String baseUrl;
    private final AuthProvider authProvider;
    private final Map<String, String> defaultHeaders;
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    ChromaApiClient(String baseUrl, AuthProvider authProvider,
                    Map<String, String> defaultHeaders,
                    Duration connectTimeout, Duration readTimeout,
                    Duration writeTimeout) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (HttpUrl.parse(normalizedBaseUrl) == null) {
            throw new IllegalArgumentException("baseUrl must be a valid URL: " + baseUrl);
        }
        this.baseUrl = normalizedBaseUrl;
        this.authProvider = authProvider;
        this.defaultHeaders = defaultHeaders == null
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, String>(defaultHeaders));

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (connectTimeout != null) {
            builder.connectTimeout(connectTimeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        if (readTimeout != null) {
            builder.readTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        if (writeTimeout != null) {
            builder.writeTimeout(writeTimeout.toMillis(), TimeUnit.MILLISECONDS);
        }
        this.httpClient = builder.build();
        this.gson = new GsonBuilder().create();
    }

    <T> T get(String path, Type responseType) {
        return get(path, null, responseType);
    }

    <T> T get(String path, Map<String, String> queryParams, Type responseType) {
        ensureOpen();
        HttpUrl.Builder urlBuilder = buildUrl(path).newBuilder();
        if (queryParams != null) {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }
        Request request = newRequest()
                .url(urlBuilder.build())
                .get()
                .build();
        SuccessfulResponse response = execute(request);
        return deserialize(response.body, responseType, response.statusCode);
    }

    <T> T post(String path, Object body, Type responseType) {
        ensureOpen();
        Request request = newRequest()
                .url(buildUrl(path))
                .post(jsonBody(body))
                .build();
        SuccessfulResponse response = execute(request);
        return deserialize(response.body, responseType, response.statusCode);
    }

    void post(String path, Object body) {
        ensureOpen();
        Request request = newRequest()
                .url(buildUrl(path))
                .post(jsonBody(body))
                .build();
        execute(request);
    }

    <T> T put(String path, Object body, Type responseType) {
        ensureOpen();
        Request request = newRequest()
                .url(buildUrl(path))
                .put(jsonBody(body))
                .build();
        SuccessfulResponse response = execute(request);
        return deserialize(response.body, responseType, response.statusCode);
    }

    void delete(String path) {
        ensureOpen();
        Request request = newRequest()
                .url(buildUrl(path))
                .delete()
                .build();
        execute(request);
    }

    Gson gson() {
        return gson;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        try {
            httpClient.dispatcher().executorService().shutdown();
        } finally {
            httpClient.connectionPool().evictAll();
        }
    }

    // -- internals --

    private HttpUrl buildUrl(String path) {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        HttpUrl url = HttpUrl.parse(baseUrl + path);
        if (url == null) {
            throw new IllegalArgumentException("Invalid URL: " + baseUrl + path);
        }
        return url;
    }

    private <T> T deserialize(String body, Type type, int statusCode) {
        if (isBlank(body)) {
            throw new ChromaDeserializationException(
                    "Server returned a successful response (HTTP " + statusCode + ") with an empty response body",
                    statusCode
            );
        }
        try {
            T value = gson.fromJson(body, type);
            if (value == null) {
                throw new ChromaDeserializationException(
                        "Server returned a successful response (HTTP " + statusCode + ") with a null deserialized payload. Body: "
                                + truncateBody(body),
                        statusCode
                );
            }
            return value;
        } catch (JsonParseException e) {
            throw new ChromaDeserializationException(
                    "Server returned a successful response (HTTP " + statusCode + ") but the body could not be deserialized. Body: "
                            + truncateBody(body),
                    statusCode,
                    e
            );
        }
    }

    private Request.Builder newRequest() {
        Request.Builder builder = new Request.Builder()
                .header("User-Agent", Constants.HTTP_AGENT)
                .header("Accept", "application/json");

        for (Map.Entry<String, String> entry : defaultHeaders.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }

        if (authProvider != null) {
            Map<String, String> authHeaders = new LinkedHashMap<String, String>();
            try {
                authProvider.applyAuth(authHeaders);
            } catch (RuntimeException e) {
                throw new ChromaException("Failed to apply authentication headers", e);
            }
            for (Map.Entry<String, String> entry : authHeaders.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        return builder;
    }

    private SuccessfulResponse execute(Request request) {
        Response response;
        try {
            response = httpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new ChromaConnectionException("Network error communicating with " + request.url(), e);
        }

        try {
            int statusCode = response.code();
            ResponseBody responseBody = response.body();
            String bodyString = responseBody != null ? responseBody.string() : null;

            if (statusCode >= 400) {
                ErrorBody error = parseErrorBody(bodyString, statusCode);
                throw ChromaExceptions.fromHttpResponse(statusCode, error.message, error.errorCode);
            }

            if (statusCode < 200 || statusCode >= 300) {
                throw new ChromaException("Unexpected non-2xx response: " + formatStatusWithBody(statusCode, bodyString),
                        statusCode, null);
            }

            return new SuccessfulResponse(statusCode, bodyString);
        } catch (ChromaException e) {
            throw e;
        } catch (IOException e) {
            throw new ChromaConnectionException("Network error reading response from " + request.url(), e);
        } finally {
            response.close();
        }
    }

    private ErrorBody parseErrorBody(String body, int statusCode) {
        if (isBlank(body)) {
            return new ErrorBody("HTTP " + statusCode, null);
        }
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            String message = getStringField(json, "error");
            if (message == null) {
                message = getStringField(json, "message");
            }
            if (message == null) {
                message = "HTTP " + statusCode + ": " + truncateBody(json.toString());
            }
            String errorCode = getStringField(json, "error_code");
            return new ErrorBody(message, errorCode);
        } catch (JsonParseException e) {
            // Body was not valid JSON.
        } catch (IllegalStateException e) {
            // JSON root was not an object.
        }
        return new ErrorBody("HTTP " + statusCode + ": " + truncateBody(body), null);
    }

    private static String getStringField(JsonObject json, String field) {
        if (!json.has(field) || json.get(field).isJsonNull()) {
            return null;
        }
        try {
            String value = json.get(field).getAsString();
            return value.trim().isEmpty() ? null : value;
        } catch (UnsupportedOperationException e) {
            // Field was a JSON array/object, not a primitive.
            return null;
        }
    }

    private RequestBody jsonBody(Object body) {
        if (body == null) {
            throw new IllegalArgumentException("request body must not be null");
        }
        return RequestBody.create(gson.toJson(body), Constants.JSON);
    }

    private void ensureOpen() {
        // Best-effort fast-fail: close() may still race after this check.
        if (closed.get()) {
            throw new IllegalStateException("ChromaApiClient is closed");
        }
    }

    private String formatStatusWithBody(int statusCode, String body) {
        if (isBlank(body)) {
            return "HTTP " + statusCode;
        }
        return "HTTP " + statusCode + ": " + truncateBody(body);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String truncateBody(String body) {
        if (body == null) {
            return "<null>";
        }
        if (body.length() <= MAX_BODY_SNIPPET_LENGTH) {
            return body;
        }
        return body.substring(0, MAX_BODY_SNIPPET_LENGTH) + "...";
    }

    private static final class ErrorBody {
        private final String message;
        private final String errorCode;

        private ErrorBody(String message, String errorCode) {
            this.message = message;
            this.errorCode = errorCode;
        }
    }

    private static final class SuccessfulResponse {
        private final int statusCode;
        private final String body;

        private SuccessfulResponse(int statusCode, String body) {
            if (statusCode < 200 || statusCode >= 300) {
                throw new IllegalArgumentException(
                        "SuccessfulResponse requires a 2xx status code, got: " + statusCode
                );
            }
            this.statusCode = statusCode;
            this.body = body;
        }
    }
}
