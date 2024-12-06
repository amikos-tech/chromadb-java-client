package tech.amikos.chromadb.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class ApiClient {
    private OkHttpClient httpClient;
    private boolean debugging = false;
    private HttpLoggingInterceptor loggingInterceptor;

    public ApiClient() {
        this.httpClient = new OkHttpClient();
    }

    public ApiClient setDebugging(boolean debugging) {
        if (debugging != this.debugging) {
            if (debugging) {
                loggingInterceptor = new HttpLoggingInterceptor();
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                httpClient.interceptors().add(loggingInterceptor);
            } else {
                httpClient.interceptors().remove(loggingInterceptor);
                loggingInterceptor = null;
            }
        }
        this.debugging = debugging;
        return this;
    }
}
