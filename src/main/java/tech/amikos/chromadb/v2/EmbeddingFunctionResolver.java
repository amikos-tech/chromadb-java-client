package tech.amikos.chromadb.v2;

import tech.amikos.chromadb.embeddings.EmbeddingFunction;
import tech.amikos.chromadb.embeddings.EmbeddingFunctionRegistry;
import tech.amikos.chromadb.embeddings.WithParam;
import tech.amikos.chromadb.embeddings.hf.HuggingFaceEmbeddingFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds runtime embedding-function instances from configuration/schema descriptors.
 *
 * <p>Delegates to {@link EmbeddingFunctionRegistry#getDefault()} for provider resolution.
 * The static helper methods ({@link #buildParams}, {@link #buildHuggingFaceParams}) are
 * public so the registry can reuse the parameter-building logic.</p>
 */
public final class EmbeddingFunctionResolver {

    private EmbeddingFunctionResolver() {}

    /**
     * Resolves a runtime embedding function from a descriptor.
     *
     * <p>Delegates to {@link EmbeddingFunctionRegistry#getDefault()}.{@link
     * EmbeddingFunctionRegistry#resolveDense(EmbeddingFunctionSpec) resolveDense(spec)}.</p>
     *
     * @throws ChromaException if descriptor values are invalid/unsupported or provider initialization fails
     */
    static EmbeddingFunction resolve(EmbeddingFunctionSpec spec) {
        if (spec == null) return null;
        if (!spec.isKnownType()) {
            throw new ChromaException(
                    "Unsupported embedding function type '" + spec.getType()
                    + "' for provider '" + spec.getName()
                    + "'. Only 'known' types can be auto-resolved. "
                    + "Pass your own EmbeddingFunction or use queryEmbeddings to supply vectors directly."
            );
        }
        try {
            return EmbeddingFunctionRegistry.getDefault().resolveDense(spec);
        } catch (UnsupportedEmbeddingProviderException e) {
            throw new ChromaException(
                    "Unsupported embedding provider '" + spec.getName()
                    + "'. Pass your own EmbeddingFunction or use queryEmbeddings to supply vectors directly.",
                    e
            );
        } catch (ChromaException e) {
            throw e;
        }
    }

    /**
     * Builds a HuggingFace-specific parameter array from config map, including api_type handling.
     *
     * @param config the configuration map (may be null)
     * @return parameter array for HuggingFaceEmbeddingFunction constructor
     */
    public static WithParam[] buildHuggingFaceParams(Map<String, Object> config) {
        List<WithParam> params = buildParamsList(config, HuggingFaceEmbeddingFunction.HF_API_KEY_ENV);
        if (config != null) {
            String apiType = firstString(config, "api_type", "apiType");
            if (apiType != null) {
                String normalized = apiType.trim().toUpperCase(Locale.ROOT);
                HuggingFaceEmbeddingFunction.APIType type;
                if ("HFEI_API".equals(normalized)) {
                    type = HuggingFaceEmbeddingFunction.APIType.HFEI_API;
                } else if ("HF_API".equals(normalized)) {
                    type = HuggingFaceEmbeddingFunction.APIType.HF_API;
                } else {
                    throw new IllegalArgumentException("unsupported huggingface api_type: " + apiType);
                }
                params.add(new HuggingFaceEmbeddingFunction.WithAPIType(type));
            }
        }
        return params.toArray(new WithParam[params.size()]);
    }

    /**
     * Builds a parameter array from a config map with a default API key environment variable.
     *
     * @param config          the configuration map (may be null)
     * @param defaultApiKeyEnv the default environment variable name for API key (may be null)
     * @return parameter array suitable for embedding function constructors
     */
    public static WithParam[] buildParams(Map<String, Object> config, String defaultApiKeyEnv) {
        List<WithParam> params = buildParamsList(config, defaultApiKeyEnv);
        return params.toArray(new WithParam[params.size()]);
    }

    private static List<WithParam> buildParamsList(Map<String, Object> config, String defaultApiKeyEnv) {
        List<WithParam> params = new ArrayList<WithParam>();
        if (config != null) {
            String baseUrl = firstString(config, "base_url", "base_api", "baseAPI");
            if (baseUrl != null) {
                params.add(WithParam.baseAPI(baseUrl));
            }
            String modelName = firstString(config, "model_name", "model");
            if (modelName != null) {
                params.add(WithParam.model(modelName));
            }
            String apiKey = firstString(config, "api_key", "apiKey");
            if (apiKey != null) {
                params.add(WithParam.apiKey(apiKey));
            } else {
                String apiKeyEnv = firstString(config, "api_key_env_var", "apiKeyEnvVar");
                if (apiKeyEnv != null) {
                    params.add(WithParam.apiKeyFromEnv(apiKeyEnv));
                } else if (defaultApiKeyEnv != null) {
                    params.add(WithParam.apiKeyFromEnv(defaultApiKeyEnv));
                }
            }
        } else if (defaultApiKeyEnv != null) {
            params.add(WithParam.apiKeyFromEnv(defaultApiKeyEnv));
        }
        return params;
    }

    static String firstString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (!map.containsKey(key)) {
                continue;
            }
            Object value = map.get(key);
            if (value == null) {
                continue;
            }
            if (!(value instanceof String)) {
                throw new IllegalArgumentException(key + " must be a string");
            }
            String normalized = ((String) value).trim();
            if (!normalized.isEmpty()) {
                return normalized;
            }
        }
        return null;
    }
}
