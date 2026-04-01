package tech.amikos.chromadb.embeddings;

import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.embeddings.bm25.BM25EmbeddingFunction;
import tech.amikos.chromadb.embeddings.chromacloudsplade.ChromaCloudSpladeEmbeddingFunction;
import tech.amikos.chromadb.embeddings.cohere.CohereEmbeddingFunction;
import tech.amikos.chromadb.embeddings.hf.HuggingFaceEmbeddingFunction;
import tech.amikos.chromadb.embeddings.ollama.OllamaEmbeddingFunction;
import tech.amikos.chromadb.embeddings.openai.OpenAIEmbeddingFunction;
import tech.amikos.chromadb.embeddings.voyage.VoyageEmbeddingFunction;
import tech.amikos.chromadb.v2.ChromaException;
import tech.amikos.chromadb.v2.EmbeddingFunctionResolver;
import tech.amikos.chromadb.v2.EmbeddingFunctionSpec;
import tech.amikos.chromadb.v2.UnsupportedEmbeddingProviderException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registry for embedding function factories (dense, sparse, and content).
 *
 * <p>The default singleton instance ({@link #getDefault()}) comes pre-loaded with all built-in
 * providers. Users can register custom providers via {@link #registerDense}, {@link #registerSparse},
 * and {@link #registerContent}.</p>
 *
 * <p>All register/resolve methods are synchronized for thread safety.</p>
 */
public final class EmbeddingFunctionRegistry {

    private static final EmbeddingFunctionRegistry DEFAULT = new EmbeddingFunctionRegistry(true);

    /**
     * Factory for creating dense embedding functions from configuration.
     */
    public interface DenseFactory {
        EmbeddingFunction create(Map<String, Object> config) throws EFException;
    }

    /**
     * Factory for creating sparse embedding functions from configuration.
     */
    public interface SparseFactory {
        SparseEmbeddingFunction create(Map<String, Object> config) throws EFException;
    }

    /**
     * Factory for creating content embedding functions from configuration.
     */
    public interface ContentFactory {
        ContentEmbeddingFunction create(Map<String, Object> config) throws EFException;
    }

    private final Map<String, DenseFactory> denseFactories = new LinkedHashMap<String, DenseFactory>();
    private final Map<String, SparseFactory> sparseFactories = new LinkedHashMap<String, SparseFactory>();
    private final Map<String, ContentFactory> contentFactories = new LinkedHashMap<String, ContentFactory>();

    /**
     * Creates an empty registry with no built-in providers.
     * Use this for testing or when full control over registered providers is needed.
     */
    public EmbeddingFunctionRegistry() {
    }

    private EmbeddingFunctionRegistry(boolean registerBuiltins) {
        if (registerBuiltins) {
            registerBuiltinProviders();
        }
    }

    /**
     * Returns the default singleton registry pre-loaded with all built-in providers.
     *
     * @return the shared default registry instance
     */
    public static EmbeddingFunctionRegistry getDefault() {
        return DEFAULT;
    }

    /**
     * Registers a dense embedding function factory under the given provider name.
     *
     * @param name    the provider name (case-insensitive)
     * @param factory the factory to create instances
     */
    public synchronized void registerDense(String name, DenseFactory factory) {
        denseFactories.put(normalizeProviderName(name, "dense"), requireFactory(factory, "dense"));
    }

    /**
     * Registers a sparse embedding function factory under the given provider name.
     *
     * @param name    the provider name (case-insensitive)
     * @param factory the factory to create instances
     */
    public synchronized void registerSparse(String name, SparseFactory factory) {
        sparseFactories.put(normalizeProviderName(name, "sparse"), requireFactory(factory, "sparse"));
    }

    /**
     * Registers a content embedding function factory under the given provider name.
     *
     * @param name    the provider name (case-insensitive)
     * @param factory the factory to create instances
     */
    public synchronized void registerContent(String name, ContentFactory factory) {
        contentFactories.put(normalizeProviderName(name, "content"), requireFactory(factory, "content"));
    }

    /**
     * Resolves a dense embedding function from a spec descriptor.
     *
     * <p>Returns {@code null} when {@code spec} is {@code null}.</p>
     *
     * @param spec the embedding function spec (may be null)
     * @return the resolved embedding function, or null if spec is null
     * @throws ChromaException if the provider name is unknown or initialization fails
     */
    public synchronized EmbeddingFunction resolveDense(EmbeddingFunctionSpec spec) {
        if (spec == null) {
            return null;
        }
        String name = resolveProviderName(spec, "dense");
        DenseFactory factory = denseFactories.get(name);
        if (factory == null) {
            throw new UnsupportedEmbeddingProviderException("Unsupported embedding function provider '" + spec.getName()
                    + "'. Registered dense providers: " + denseFactories.keySet());
        }
        try {
            return factory.create(spec.getConfig());
        } catch (ChromaException e) {
            throw e;
        } catch (EFException e) {
            throw new ChromaException("Failed to create dense provider '" + name + "': " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new ChromaException("Failed to create dense provider '" + name + "': " + e.getMessage(), e);
        }
    }

    /**
     * Resolves a sparse embedding function from a spec descriptor.
     *
     * <p>Returns {@code null} when {@code spec} is {@code null}.</p>
     *
     * @param spec the embedding function spec (may be null)
     * @return the resolved sparse embedding function, or null if spec is null
     * @throws ChromaException if the provider name is unknown or initialization fails
     */
    public synchronized SparseEmbeddingFunction resolveSparse(EmbeddingFunctionSpec spec) {
        if (spec == null) {
            return null;
        }
        String name = resolveProviderName(spec, "sparse");
        SparseFactory factory = sparseFactories.get(name);
        if (factory == null) {
            throw new UnsupportedEmbeddingProviderException("Unsupported sparse embedding function provider '" + spec.getName()
                    + "'. Registered sparse providers: " + sparseFactories.keySet());
        }
        try {
            return factory.create(spec.getConfig());
        } catch (ChromaException e) {
            throw e;
        } catch (EFException e) {
            throw new ChromaException("Failed to create sparse provider '" + name + "': " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new ChromaException("Failed to create sparse provider '" + name + "': " + e.getMessage(), e);
        }
    }

    /**
     * Resolves a content embedding function from a spec descriptor.
     *
     * <p>First tries the content factory map. If no content factory is registered for the
     * provider, falls back to the dense factory and wraps it with
     * {@link ContentEmbeddingFunction#fromTextOnly(EmbeddingFunction)}.</p>
     *
     * <p>Returns {@code null} when {@code spec} is {@code null}.</p>
     *
     * @param spec the embedding function spec (may be null)
     * @return the resolved content embedding function, or null if spec is null
     * @throws ChromaException if neither content nor dense factory is found
     */
    public synchronized ContentEmbeddingFunction resolveContent(EmbeddingFunctionSpec spec) {
        if (spec == null) {
            return null;
        }
        String name = resolveProviderName(spec, "content");

        // Try content factory first
        ContentFactory cf = contentFactories.get(name);
        if (cf != null) {
            try {
                return cf.create(spec.getConfig());
            } catch (ChromaException e) {
                throw e;
            } catch (EFException e) {
                throw new ChromaException("Failed to create content provider '" + name + "': " + e.getMessage(), e);
            } catch (RuntimeException e) {
                throw new ChromaException("Failed to create content provider '" + name + "': " + e.getMessage(), e);
            }
        }

        // Fallback: try dense + adapter
        DenseFactory df = denseFactories.get(name);
        if (df != null) {
            try {
                return ContentEmbeddingFunction.fromTextOnly(df.create(spec.getConfig()));
            } catch (ChromaException e) {
                throw e;
            } catch (EFException e) {
                throw new ChromaException("Failed to create content provider '" + name + "' (via dense fallback): " + e.getMessage(), e);
            } catch (RuntimeException e) {
                throw new ChromaException("Failed to create content provider '" + name + "' (via dense fallback): " + e.getMessage(), e);
            }
        }

        throw new UnsupportedEmbeddingProviderException("Unsupported content embedding provider '" + spec.getName()
                + "'. Registered content providers: " + contentFactories.keySet()
                + ", dense providers (fallback): " + denseFactories.keySet());
    }

    private static String normalizeProviderName(String name, String type) {
        if (name == null) {
            throw new IllegalArgumentException(type + " provider name must not be null");
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(type + " provider name must not be blank");
        }
        return normalized;
    }

    private static String resolveProviderName(EmbeddingFunctionSpec spec, String type) {
        try {
            return normalizeProviderName(spec.getName(), type);
        } catch (IllegalArgumentException e) {
            throw new ChromaException("Failed to resolve " + type + " provider: " + e.getMessage(), e);
        }
    }

    private static <T> T requireFactory(T factory, String type) {
        if (factory == null) {
            throw new IllegalArgumentException(type + " factory must not be null");
        }
        return factory;
    }

    private void registerBuiltinProviders() {
        // Dense providers
        registerDense("default", new DenseFactory() {
            @Override
            public EmbeddingFunction create(Map<String, Object> config) throws EFException {
                return new DefaultEmbeddingFunction();
            }
        });
        registerDense("openai", new DenseFactory() {
            @Override
            public EmbeddingFunction create(Map<String, Object> config) throws EFException {
                return new OpenAIEmbeddingFunction(EmbeddingFunctionResolver.buildParams(config, OpenAIEmbeddingFunction.OPENAI_API_KEY_ENV));
            }
        });
        registerDense("cohere", new DenseFactory() {
            @Override
            public EmbeddingFunction create(Map<String, Object> config) throws EFException {
                return new CohereEmbeddingFunction(EmbeddingFunctionResolver.buildParams(config, CohereEmbeddingFunction.COHERE_API_KEY_ENV));
            }
        });

        DenseFactory hfFactory = new DenseFactory() {
            @Override
            public EmbeddingFunction create(Map<String, Object> config) throws EFException {
                return new HuggingFaceEmbeddingFunction(EmbeddingFunctionResolver.buildHuggingFaceParams(config));
            }
        };
        registerDense("huggingface", hfFactory);
        registerDense("hugging_face", hfFactory);
        registerDense("hf", hfFactory);

        registerDense("ollama", new DenseFactory() {
            @Override
            public EmbeddingFunction create(Map<String, Object> config) throws EFException {
                return new OllamaEmbeddingFunction(EmbeddingFunctionResolver.buildParams(config, null));
            }
        });

        // Gemini - guarded for optional SDK
        try {
            Class.forName("com.google.genai.Client");
            registerDense("google_genai", new DenseFactory() {
                @Override
                public EmbeddingFunction create(Map<String, Object> config) throws EFException {
                    return new tech.amikos.chromadb.embeddings.gemini.GeminiEmbeddingFunction(
                            EmbeddingFunctionResolver.buildParams(config, tech.amikos.chromadb.embeddings.gemini.GeminiEmbeddingFunction.GEMINI_API_KEY_ENV));
                }
            });
        } catch (NoClassDefFoundError ignored) {
        } catch (ClassNotFoundException ignored) {
        }

        // Bedrock - guarded for optional SDK
        try {
            Class.forName("software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient");
            registerDense("amazon_bedrock", new DenseFactory() {
                @Override
                public EmbeddingFunction create(Map<String, Object> config) throws EFException {
                    return new tech.amikos.chromadb.embeddings.bedrock.BedrockEmbeddingFunction(
                            EmbeddingFunctionResolver.buildParams(config, null));
                }
            });
        } catch (NoClassDefFoundError ignored) {
        } catch (ClassNotFoundException ignored) {
        }

        // Voyage
        registerDense("voyageai", new DenseFactory() {
            @Override
            public EmbeddingFunction create(Map<String, Object> config) throws EFException {
                return new VoyageEmbeddingFunction(EmbeddingFunctionResolver.buildParams(config, VoyageEmbeddingFunction.VOYAGE_API_KEY_ENV));
            }
        });

        // Sparse providers
        registerSparse("chroma_bm25", new SparseFactory() {
            @Override
            public SparseEmbeddingFunction create(Map<String, Object> config) {
                return new BM25EmbeddingFunction();
            }
        });
        registerSparse("bm25", new SparseFactory() {
            @Override
            public SparseEmbeddingFunction create(Map<String, Object> config) {
                return new BM25EmbeddingFunction();
            }
        });
        registerSparse("chromacloud_splade", new SparseFactory() {
            @Override
            public SparseEmbeddingFunction create(Map<String, Object> config) throws EFException {
                return new ChromaCloudSpladeEmbeddingFunction(EmbeddingFunctionResolver.buildParams(config, ChromaCloudSpladeEmbeddingFunction.CHROMA_API_KEY_ENV));
            }
        });
    }
}
