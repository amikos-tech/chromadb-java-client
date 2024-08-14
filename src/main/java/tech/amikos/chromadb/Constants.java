package tech.amikos.chromadb;

import okhttp3.MediaType;

public class Constants {

    public static final String EF_PARAMS_BASE_API = "baseAPI";
    public static final String EF_PARAMS_MODEL = "modelName";
    public static final String EF_PARAMS_API_KEY = "apiKey";
    public static final String EF_PARAMS_API_KEY_FROM_ENV = "envAPIKey";
    public static final String MODEL_NAME = "MODEL_NAME";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final String HTTP_AGENT = "chroma-java-client";
}
