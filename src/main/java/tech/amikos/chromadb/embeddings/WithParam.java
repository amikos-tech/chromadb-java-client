package tech.amikos.chromadb.embeddings;


import tech.amikos.chromadb.Constants;
import tech.amikos.chromadb.EFException;

import java.util.Map;

public abstract class WithParam {
    public abstract void apply(Map<String, Object> params) throws EFException;

    public static WithParam apiKey(String apiKey) {
        return new WithAPIKey(apiKey);
    }

    public static WithParam apiKeyFromEnv(String apiKeyEnvVarName) {
        return new WithEnvAPIKey(apiKeyEnvVarName);
    }

    public static WithParam model(String model) {
        return new WithModel(model);
    }

    public static WithParam modelFromEnv(String modelEnvVarName) {
        return new WithModelFromEnv(modelEnvVarName);
    }

    public static WithParam baseAPI(String baseAPI) {
        return new WithBaseAPI(baseAPI);
    }

    public static WithParam defaultModel(String model) {
        return new WithDefaultModel(model);
    }


}

class WithBaseAPI extends WithParam {
    private final String baseAPI;

    public WithBaseAPI(String baseAPI) {
        this.baseAPI = baseAPI;
    }

    @Override
    public void apply(Map<String, Object> params) {
        params.put(Constants.EF_PARAMS_BASE_API, baseAPI);
    }
}

class WithModel extends WithParam {
    private final String model;

    public WithModel(String model) {
        this.model = model;
    }

    @Override
    public void apply(Map<String, Object> params) {
        params.put(Constants.EF_PARAMS_MODEL, model);
    }
}

class WithModelFromEnv extends WithParam {

    private String modelEnvVarName = Constants.MODEL_NAME;

    public WithModelFromEnv(String modelEnvVarName) {
        this.modelEnvVarName = modelEnvVarName;
    }

    /**
     * Reads MODEL_NAME from the environment
     */
    public WithModelFromEnv() {
    }

    @Override
    public void apply(Map<String, Object> params) throws EFException {
        if (System.getenv(modelEnvVarName) == null) {
            throw new EFException("Model not found in environment variable: " + modelEnvVarName);
        }
        params.put(Constants.EF_PARAMS_MODEL, System.getenv(modelEnvVarName));
    }
}

class WithDefaultModel extends WithParam {

    private final String model;

    public WithDefaultModel(String model) {
        this.model = model;
    }

    @Override
    public void apply(Map<String, Object> params) {
        params.put(Constants.EF_PARAMS_MODEL, model);
    }
}

class WithAPIKey extends WithParam {
    private final String apiKey;

    public WithAPIKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public void apply(Map<String, Object> params) {
        params.put(Constants.EF_PARAMS_API_KEY, apiKey);
    }
}

class WithEnvAPIKey extends WithParam {
    private final String apiKeyEnvVarName;

    public WithEnvAPIKey(String apiKeyEnvVarName) {
        this.apiKeyEnvVarName = apiKeyEnvVarName;
    }

    @Override
    public void apply(Map<String, Object> params) throws EFException {
        if (System.getenv(apiKeyEnvVarName) == null) {
            throw new EFException("API Key not found in environment variable: " + apiKeyEnvVarName);
        }
        params.put(Constants.EF_PARAMS_API_KEY, System.getenv(apiKeyEnvVarName));
    }
}
