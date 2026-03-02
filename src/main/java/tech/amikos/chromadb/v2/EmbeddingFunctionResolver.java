package tech.amikos.chromadb.v2;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.embeddings.DefaultEmbeddingFunction;
import tech.amikos.chromadb.embeddings.EmbeddingFunction;
import tech.amikos.chromadb.embeddings.WithParam;
import tech.amikos.chromadb.embeddings.cohere.CohereEmbeddingFunction;
import tech.amikos.chromadb.embeddings.hf.HuggingFaceEmbeddingFunction;
import tech.amikos.chromadb.embeddings.ollama.OllamaEmbeddingFunction;
import tech.amikos.chromadb.embeddings.openai.OpenAIEmbeddingFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builds runtime embedding-function instances from configuration/schema descriptors. */
final class EmbeddingFunctionResolver {

    private EmbeddingFunctionResolver() {}

    /**
     * Resolves a runtime embedding function from a descriptor.
     *
     * <p>Returns {@code null} when {@code spec} is {@code null}. For non-null descriptors this method
     * either returns an initialized embedding function or throws {@link ChromaException} with context.
     * Unsupported descriptor type/provider values fail fast with actionable guidance.</p>
     *
     * @throws ChromaException if descriptor values are invalid/unsupported or provider initialization fails
     */
    static EmbeddingFunction resolve(EmbeddingFunctionSpec spec) {
        if (spec == null) {
            return null;
        }
        String rawProviderName = spec.getName();
        if (rawProviderName == null || rawProviderName.trim().isEmpty()) {
            throw new ChromaException(
                    "Embedding function provider name is missing. "
                            + "Use queryEmbeddings(...) or set a valid embedding_function descriptor."
            );
        }
        String type = spec.getType();
        if (type != null && !"known".equals(type.toLowerCase(Locale.ROOT))) {
            throw unsupported("Unsupported embedding function type '" + type + "' for provider '" + rawProviderName + "'");
        }

        String provider = rawProviderName.trim().toLowerCase(Locale.ROOT);
        try {
            if ("default".equals(provider)) {
                return new DefaultEmbeddingFunction();
            }
            if ("openai".equals(provider)) {
                return new OpenAIEmbeddingFunction(buildParams(spec.getConfig(), OpenAIEmbeddingFunction.OPENAI_API_KEY_ENV));
            }
            if ("cohere".equals(provider)) {
                return new CohereEmbeddingFunction(buildParams(spec.getConfig(), CohereEmbeddingFunction.COHERE_API_KEY_ENV));
            }
            if ("huggingface".equals(provider) || "hugging_face".equals(provider) || "hf".equals(provider)) {
                return new HuggingFaceEmbeddingFunction(buildHuggingFaceParams(spec.getConfig()));
            }
            if ("ollama".equals(provider)) {
                return new OllamaEmbeddingFunction(buildParams(spec.getConfig(), null));
            }
            throw unsupported("Unsupported embedding function provider '" + rawProviderName + "'");
        } catch (ChromaException e) {
            throw e;
        } catch (EFException e) {
            throw new ChromaException(
                    "Failed to initialize embedding function provider '" + rawProviderName + "': " + e.getMessage(),
                    e
            );
        } catch (RuntimeException e) {
            throw new ChromaException(
                    "Failed to initialize embedding function provider '" + rawProviderName + "': " + e.getMessage(),
                    e
            );
        }
    }

    private static WithParam[] buildHuggingFaceParams(Map<String, Object> config) {
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

    private static WithParam[] buildParams(Map<String, Object> config, String defaultApiKeyEnv) {
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

    private static String firstString(Map<String, Object> map, String... keys) {
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

    private static ChromaException unsupported(String message) {
        return new ChromaException(
                message + ". Use queryEmbeddings(...) or one of [default, openai, cohere, huggingface, ollama]."
        );
    }
}
