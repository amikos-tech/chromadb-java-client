package tech.amikos.chromadb.v2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import tech.amikos.chromadb.v2.AuthProvider;
import tech.amikos.chromadb.v2.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HttpClient {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Gson GSON = new GsonBuilder().create();

    private final OkHttpClient client;
    private final String baseUrl;
    private final AuthProvider authProvider;

    private HttpClient(Builder builder) {
        this.baseUrl = builder.baseUrl.endsWith("/")
            ? builder.baseUrl.substring(0, builder.baseUrl.length() - 1)
            : builder.baseUrl;
        this.authProvider = builder.authProvider;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(builder.connectTimeout, TimeUnit.SECONDS)
                .readTimeout(builder.readTimeout, TimeUnit.SECONDS)
                .writeTimeout(builder.writeTimeout, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request.Builder requestBuilder = original.newBuilder()
                            .header("User-Agent", "ChromaDB-Java-Client-V2/0.2.0");
                    return chain.proceed(requestBuilder.build());
                })
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public <T> T get(String path, Class<T> responseType) {
        String url = baseUrl + path;
        Request.Builder requestBuilder = new Request.Builder().url(url).get();
        requestBuilder = authProvider.authenticate(requestBuilder);

        return executeRequest(requestBuilder.build(), responseType);
    }

    public <T> T post(String path, Object body, Class<T> responseType) {
        String url = baseUrl + path;
        String json = GSON.toJson(body);
        RequestBody requestBody = RequestBody.create(json, JSON);

        Request.Builder requestBuilder = new Request.Builder().url(url).post(requestBody);
        requestBuilder = authProvider.authenticate(requestBuilder);

        return executeRequest(requestBuilder.build(), responseType);
    }

    public <T> T put(String path, Object body, Class<T> responseType) {
        String url = baseUrl + path;
        String json = GSON.toJson(body);
        RequestBody requestBody = RequestBody.create(json, JSON);

        Request.Builder requestBuilder = new Request.Builder().url(url).put(requestBody);
        requestBuilder = authProvider.authenticate(requestBuilder);

        return executeRequest(requestBuilder.build(), responseType);
    }

    public <T> T delete(String path, Class<T> responseType) {
        String url = baseUrl + path;
        Request.Builder requestBuilder = new Request.Builder().url(url).delete();
        requestBuilder = authProvider.authenticate(requestBuilder);

        return executeRequest(requestBuilder.build(), responseType);
    }

    public <T> T patch(String path, Object body, Class<T> responseType) {
        String url = baseUrl + path;
        String json = GSON.toJson(body);
        RequestBody requestBody = RequestBody.create(json, JSON);

        Request.Builder requestBuilder = new Request.Builder().url(url).patch(requestBody);
        requestBuilder = authProvider.authenticate(requestBuilder);

        return executeRequest(requestBuilder.build(), responseType);
    }

    private <T> T executeRequest(Request request, Class<T> responseType) {
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";

            if (!response.isSuccessful()) {
                handleErrorResponse(response.code(), responseBody);
            }

            if (responseType == Void.class) {
                return null;
            }

            if (responseType == String.class) {
                return responseType.cast(responseBody);
            }

            return GSON.fromJson(responseBody, responseType);
        } catch (IOException e) {
            throw new ChromaV2Exception("Failed to execute request: " + e.getMessage(), e);
        }
    }

    private void handleErrorResponse(int statusCode, String responseBody) {
        ErrorResponse errorResponse = null;
        try {
            errorResponse = GSON.fromJson(responseBody, ErrorResponse.class);
        } catch (Exception ignored) {
        }

        String message = errorResponse != null
            ? errorResponse.getMessage()
            : "HTTP " + statusCode + ": " + responseBody;

        switch (statusCode) {
            case 400:
                throw new ChromaBadRequestException(message);
            case 401:
                throw new ChromaUnauthorizedException(message);
            case 404:
                throw new ChromaNotFoundException(message);
            case 500:
                throw new ChromaServerException(message);
            default:
                throw new ChromaV2Exception(statusCode, "HTTP_ERROR", message);
        }
    }

    public static class Builder {
        private String baseUrl;
        private AuthProvider authProvider = AuthProvider.none();
        private int connectTimeout = 60;
        private int readTimeout = 60;
        private int writeTimeout = 60;

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder auth(AuthProvider authProvider) {
            this.authProvider = authProvider;
            return this;
        }

        public Builder connectTimeout(int seconds) {
            this.connectTimeout = seconds;
            return this;
        }

        public Builder readTimeout(int seconds) {
            this.readTimeout = seconds;
            return this;
        }

        public Builder writeTimeout(int seconds) {
            this.writeTimeout = seconds;
            return this;
        }

        public HttpClient build() {
            if (baseUrl == null || baseUrl.isEmpty()) {
                throw new IllegalArgumentException("baseUrl is required");
            }
            return new HttpClient(this);
        }
    }

    private static class ErrorResponse {
        private String error;
        private String message;

        public String getMessage() {
            return message != null ? message : error;
        }
    }
}