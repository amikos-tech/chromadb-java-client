package tech.amikos.chromadb.v2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
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

/**
 * Package-private HTTP transport for the Chroma v2 REST API.
 * Owns the {@link OkHttpClient} and {@link Gson} instances.
 */
class ChromaApiClient {

    private final String baseUrl;
    private final AuthProvider authProvider;
    private final Map<String, String> defaultHeaders;
    private final OkHttpClient httpClient;
    private final Gson gson;

    ChromaApiClient(String baseUrl, AuthProvider authProvider,
                    Map<String, String> defaultHeaders,
                    Duration connectTimeout, Duration readTimeout,
                    Duration writeTimeout) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
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
        HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl + path).newBuilder();
        if (queryParams != null) {
            for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
            }
        }
        Request request = newRequest()
                .url(urlBuilder.build())
                .get()
                .build();
        String body = execute(request);
        return gson.fromJson(body, responseType);
    }

    <T> T post(String path, Object body, Type responseType) {
        Request request = newRequest()
                .url(baseUrl + path)
                .post(jsonBody(body))
                .build();
        String responseBody = execute(request);
        return gson.fromJson(responseBody, responseType);
    }

    void post(String path, Object body) {
        Request request = newRequest()
                .url(baseUrl + path)
                .post(jsonBody(body))
                .build();
        execute(request);
    }

    <T> T put(String path, Object body, Type responseType) {
        Request request = newRequest()
                .url(baseUrl + path)
                .put(jsonBody(body))
                .build();
        String responseBody = execute(request);
        return gson.fromJson(responseBody, responseType);
    }

    void delete(String path) {
        Request request = newRequest()
                .url(baseUrl + path)
                .delete()
                .build();
        execute(request);
    }

    Gson gson() {
        return gson;
    }

    void close() {
        httpClient.dispatcher().executorService().shutdown();
        httpClient.connectionPool().evictAll();
    }

    // -- internals --

    private Request.Builder newRequest() {
        Request.Builder builder = new Request.Builder()
                .header("User-Agent", Constants.HTTP_AGENT)
                .header("Accept", "application/json");

        for (Map.Entry<String, String> entry : defaultHeaders.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }

        if (authProvider != null) {
            Map<String, String> authHeaders = new LinkedHashMap<String, String>();
            authProvider.applyAuth(authHeaders);
            for (Map.Entry<String, String> entry : authHeaders.entrySet()) {
                builder.header(entry.getKey(), entry.getValue());
            }
        }

        return builder;
    }

    private String execute(Request request) {
        Response response;
        try {
            response = httpClient.newCall(request).execute();
        } catch (IOException e) {
            throw new ChromaConnectionException("Failed to connect to " + request.url(), e);
        }

        try {
            int statusCode = response.code();
            ResponseBody responseBody = response.body();
            String bodyString = responseBody != null ? responseBody.string() : null;

            if (statusCode >= 400) {
                String message = parseErrorMessage(bodyString, statusCode);
                String errorCode = parseErrorCode(bodyString);
                throw ChromaExceptions.fromHttpResponse(statusCode, message, errorCode);
            }

            return bodyString;
        } catch (ChromaException e) {
            throw e;
        } catch (IOException e) {
            throw new ChromaConnectionException("Failed to read response from " + request.url(), e);
        } finally {
            response.close();
        }
    }

    private String parseErrorMessage(String body, int statusCode) {
        if (body == null || body.trim().isEmpty()) {
            return "HTTP " + statusCode;
        }
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("error") && !json.get("error").isJsonNull()) {
                return json.get("error").getAsString();
            }
            if (json.has("message") && !json.get("message").isJsonNull()) {
                return json.get("message").getAsString();
            }
        } catch (Exception ignored) {
            // body is not valid JSON or doesn't have expected fields
        }
        return "HTTP " + statusCode;
    }

    private String parseErrorCode(String body) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }
        try {
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (json.has("error_code") && !json.get("error_code").isJsonNull()) {
                return json.get("error_code").getAsString();
            }
        } catch (Exception ignored) {
            // not JSON or no error_code field
        }
        return null;
    }

    private RequestBody jsonBody(Object body) {
        return RequestBody.create(gson.toJson(body), Constants.JSON);
    }
}
