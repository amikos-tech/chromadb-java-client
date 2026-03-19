package tech.amikos.chromadb.embeddings;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.*;

import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.compress.archivers.tar.*;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.transforms.clip.ClipByValue;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.shade.guava.primitives.Floats;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.v2.ChromaException;

import java.io.*;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DefaultEmbeddingFunction implements EmbeddingFunction {
    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(DefaultEmbeddingFunction.class.getName());
    public static final String MODEL_NAME = "all-MiniLM-L6-v2";
    private static final String ARCHIVE_FILENAME = "onnx.tar.gz";

    /** Download URL for the ONNX model. Package-private to allow test overrides from the same package. */
    static volatile String modelDownloadUrl = "https://chroma-onnx-models.s3.amazonaws.com/all-MiniLM-L6-v2/onnx.tar.gz";

    private static final String MODEL_SHA256_CHECKSUM = "913d7300ceae3b2dbc2c50d1de4baacab4be7b9380491c27fab7418616a16ec3";
    public static final Path MODEL_CACHE_DIR = Paths.get(System.getProperty("user.home"), ".cache", "chroma", "onnx_models", MODEL_NAME);
    private static final Path modelPath = Paths.get(MODEL_CACHE_DIR.toString(), "onnx");
    private static final Path modelFile = Paths.get(modelPath.toString(), "model.onnx");

    /** Default timeout in seconds for ONNX model download. */
    public static final int DEFAULT_DOWNLOAD_TIMEOUT_SECONDS = 300;

    private static final Object MODEL_INIT_LOCK = new Object();

    private final HuggingFaceTokenizer tokenizer;
    private final OrtEnvironment env;
    final OrtSession session;

    public static float[][] normalize(float[][] v) {
        int rows = v.length;
        int cols = v[0].length;
        float[] norm = new float[rows];

        // Step 1: Compute the L2 norm of each row
        for (int i = 0; i < rows; i++) {
            float sum = 0;
            for (int j = 0; j < cols; j++) {
                sum += v[i][j] * v[i][j];
            }
            norm[i] = (float) Math.sqrt(sum);
        }

        // Step 2: Handle zero norms
        for (int i = 0; i < rows; i++) {
            if (norm[i] == 0) {
                norm[i] = 1e-12f;
            }
        }

        // Step 3: Normalize each row
        float[][] normalized = new float[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                normalized[i][j] = v[i][j] / norm[i];
            }
        }
        return normalized;
    }

    public DefaultEmbeddingFunction() throws EFException {
        this(DEFAULT_DOWNLOAD_TIMEOUT_SECONDS);
    }

    public DefaultEmbeddingFunction(int downloadTimeoutSeconds) throws EFException {
        try {
            ensureModelDownloaded(downloadTimeoutSeconds);
        } catch (ChromaException e) {
            throw new EFException(e);
        }

        Map<String, String> tokenizerConfig = Collections.unmodifiableMap(new HashMap<String, String>() {{
            put("padding", "MAX_LENGTH");
            put("maxLength", "256");
        }});

        try {
            tokenizer = HuggingFaceTokenizer.newInstance(modelPath, tokenizerConfig);
            this.env = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            this.session = env.createSession(modelFile.toString(), options);
        } catch (OrtException | IOException e) {
            throw new EFException(e);
        }
    }

    public List<List<Float>> forward(List<String> documents) throws OrtException {
        Encoding[] e = tokenizer.batchEncode(documents, true, false);
        ArrayList<Long> inputIds = new ArrayList<>();
        ArrayList<Long> attentionMask = new ArrayList<>();
        ArrayList<Long> tokenIdtypes = new ArrayList<>();
        int maxIds = 0;
        for (Encoding encoding : e) {
            maxIds = Math.max(maxIds, encoding.getIds().length);
            inputIds.addAll(Arrays.asList(Arrays.stream(encoding.getIds()).boxed().toArray(Long[]::new)));
            attentionMask.addAll(Arrays.asList(Arrays.stream(encoding.getAttentionMask()).boxed().toArray(Long[]::new)));
            tokenIdtypes.addAll(Arrays.asList(Arrays.stream(encoding.getTypeIds()).boxed().toArray(Long[]::new)));
        }
        long[] inputShape = new long[]{e.length, maxIds};
        OnnxTensor inputTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(inputIds.stream().mapToLong(i -> i).toArray()), inputShape);
        OnnxTensor attentionTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(attentionMask.stream().mapToLong(i -> i).toArray()), inputShape);
        OnnxTensor _tokenIdtypes = OnnxTensor.createTensor(env, LongBuffer.wrap(tokenIdtypes.stream().mapToLong(i -> i).toArray()), inputShape);
        // Inputs for all-MiniLM-L6-v2 model
        Map<String, ? extends OnnxTensorLike> inputs = Collections.unmodifiableMap(new HashMap<String, OnnxTensorLike>() {{
            put("input_ids", inputTensor);
            put("attention_mask", attentionTensor);
            put("token_type_ids", _tokenIdtypes);
        }});
        INDArray lastHiddenState = null;
        try (OrtSession.Result results = session.run(inputs)) {
            lastHiddenState = Nd4j.create((float[][][]) results.get(0).getValue());

        }
        INDArray attMask = Nd4j.create(attentionMask.stream().mapToDouble(i -> i).toArray(), inputShape, 'c');
        INDArray expandedMask = Nd4j.expandDims(attMask, 2).broadcast(lastHiddenState.shape());
        INDArray summed = lastHiddenState.mul(expandedMask).sum(1);
        INDArray[] clippedSumMask = Nd4j.getExecutioner().exec(
                new ClipByValue(expandedMask.sum(1), 1e-9, Double.MAX_VALUE)
        );
        INDArray embeddings = summed.div(clippedSumMask[0]);
        float[][] embeddingsArray = normalize(embeddings.toFloatMatrix());
        List<List<Float>> embeddingsList = new ArrayList<>();
        for (float[] embedding : embeddingsArray) {
            embeddingsList.add(Floats.asList(embedding));
        }
        return embeddingsList;
    }

    private static String getSHA256Checksum(String filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] byteArray = new byte[1024];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static void extractTarGz(Path tarGzPath, Path extractDir) throws IOException {
        try (InputStream fileIn = Files.newInputStream(tarGzPath);
             GZIPInputStream gzipIn = new GZIPInputStream(fileIn);
             TarArchiveInputStream tarIn = new TarArchiveInputStream(gzipIn)) {

            TarArchiveEntry entry;
            while ((entry = tarIn.getNextTarEntry()) != null) {
                Path entryPath = extractDir.resolve(entry.getName()).normalize();
                if (!entryPath.startsWith(extractDir)) {
                    throw new IOException("Tar entry escapes extraction directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (OutputStream out = Files.newOutputStream(entryPath)) {
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = tarIn.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                    }
                }
            }
        }
    }

    /** Check if the model is present at the expected location. Stateless file-existence check -- no static boolean flag. */
    private static boolean validateModel() {
        return modelFile.toFile().exists() && modelFile.toFile().isFile();
    }

    /** Thread-safe lazy model download using double-checked locking with stateless file-existence gate. */
    private static void ensureModelDownloaded(int timeoutSeconds) {
        if (validateModel()) {
            return;
        }
        synchronized (MODEL_INIT_LOCK) {
            if (validateModel()) {
                return;  // another thread downloaded while we waited
            }
            downloadModel(timeoutSeconds);
        }
    }

    /**
     * Downloads the ONNX model archive using OkHttp with a configurable read timeout.
     * Retryable failures (timeout, connection error, any unsuccessful HTTP status other than 403/404) are retried once.
     * Non-retryable failures (HTTP 404, 403, checksum mismatch) fail fast.
     * All download failures throw {@link ChromaException} with an actionable message.
     */
    private static void downloadModel(int timeoutSeconds) {
        OkHttpClient downloadClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build();

        try {
            try {
                Files.createDirectories(MODEL_CACHE_DIR);
            } catch (IOException e) {
                throw new ChromaException(
                    "DefaultEmbeddingFunction: failed to create model cache directory "
                    + MODEL_CACHE_DIR + ": " + e.getMessage(), e);
            }
            Path archivePath = Paths.get(MODEL_CACHE_DIR.toString(), ARCHIVE_FILENAME);

            // First attempt
            try {
                attemptDownload(downloadClient, archivePath);
                verifyAndExtract(archivePath);
                return;
            } catch (RetryableDownloadException e) {
                LOG.warning("DefaultEmbeddingFunction: download attempt 1 failed (" + e.getMessage() + "), retrying...");
            } catch (NonRetryableDownloadException e) {
                throw new ChromaException(e.getMessage(), e);
            }

            // Second attempt (final)
            try {
                attemptDownload(downloadClient, archivePath);
                verifyAndExtract(archivePath);
            } catch (RetryableDownloadException e) {
                throw new ChromaException(
                    "DefaultEmbeddingFunction: model download failed after 2 attempts. "
                    + "Check network connectivity. Download URL: " + modelDownloadUrl, e);
            } catch (NonRetryableDownloadException e) {
                throw new ChromaException(e.getMessage(), e);
            }
        } finally {
            downloadClient.dispatcher().executorService().shutdown();
            downloadClient.connectionPool().evictAll();
        }
    }

    private static void attemptDownload(OkHttpClient client, Path archivePath)
            throws RetryableDownloadException, NonRetryableDownloadException {
        Request request = new Request.Builder().url(modelDownloadUrl).get().build();
        try {
            LOG.info("Model not found. Downloading from " + modelDownloadUrl + "...");
            Response response = client.newCall(request).execute();
            try {
                int code = response.code();
                if (code == 404 || code == 403) {
                    throw new NonRetryableDownloadException(
                        "DefaultEmbeddingFunction: model download failed with HTTP " + code
                        + " at " + modelDownloadUrl + ". This is a non-retryable error.");
                }
                if (!response.isSuccessful()) {
                    throw new RetryableDownloadException(
                        "DefaultEmbeddingFunction: model download failed with HTTP " + code);
                }
                if (response.body() == null) {
                    throw new RetryableDownloadException(
                        "DefaultEmbeddingFunction: model download returned empty body (HTTP " + code + ")");
                }
                try (InputStream in = response.body().byteStream()) {
                    Files.copy(in, archivePath, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                response.close();
            }
        } catch (java.net.SocketTimeoutException e) {
            throw new RetryableDownloadException("Download timed out: " + e.getMessage(), e);
        } catch (java.net.ConnectException e) {
            throw new RetryableDownloadException("Connection failed: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RetryableDownloadException("Download I/O error: " + e.getMessage(), e);
        }
    }

    private static void verifyAndExtract(Path archivePath) throws NonRetryableDownloadException {
        try {
            if (!MODEL_SHA256_CHECKSUM.equals(getSHA256Checksum(archivePath.toString()))) {
                // Delete corrupt archive
                if (!archivePath.toFile().delete()) {
                    LOG.warning("Failed to delete corrupt archive at " + archivePath);
                }
                throw new NonRetryableDownloadException(
                    "DefaultEmbeddingFunction: downloaded model checksum does not match. "
                    + "Expected: " + MODEL_SHA256_CHECKSUM + ". "
                    + "Delete " + MODEL_CACHE_DIR + " and try again.");
            }
            extractTarGz(archivePath, MODEL_CACHE_DIR);
            if (!archivePath.toFile().delete()) {
                LOG.warning("Failed to delete archive at " + archivePath);
            }
        } catch (NonRetryableDownloadException e) {
            throw e;
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new NonRetryableDownloadException("Failed to verify/extract model: " + e.getMessage(), e);
        }
    }

    @Override
    public Embedding embedQuery(String query) throws EFException {
        if (query == null) {
            throw new ChromaException("DefaultEmbeddingFunction: query must not be null");
        }
        try {
            return Embedding.fromList(forward(Collections.singletonList(query)).get(0));
        } catch (OrtException e) {
            throw new EFException(e);
        }
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws EFException {
        if (documents == null) {
            throw new ChromaException("DefaultEmbeddingFunction: documents must not be null");
        }
        if (documents.isEmpty()) {
            throw new ChromaException("DefaultEmbeddingFunction: documents must not be empty");
        }
        try {
            return forward(documents).stream().map(Embedding::new).collect(Collectors.toList());
        } catch (OrtException e) {
            throw new EFException(e);
        }
    }

    @Override
    public List<Embedding> embedDocuments(String[] documents) throws EFException {
        return embedDocuments(Arrays.asList(documents));
    }

    @Override
    public List<Embedding> embedQueries(List<String> queries) throws EFException {
        return embedDocuments(queries);
    }

    @Override
    public List<Embedding> embedQueries(String[] queries) throws EFException {
        return embedQueries(Arrays.asList(queries));
    }

    // --- Inner exception classes for download retry classification ---

    private static class RetryableDownloadException extends Exception {
        RetryableDownloadException(String message) { super(message); }
        RetryableDownloadException(String message, Throwable cause) { super(message, cause); }
    }

    private static class NonRetryableDownloadException extends Exception {
        NonRetryableDownloadException(String message) { super(message); }
        NonRetryableDownloadException(String message, Throwable cause) { super(message, cause); }
    }
}
