package tech.amikos.chromadb.embeddings;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.*;

import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.*;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.impl.transforms.clip.ClipByValue;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.shade.guava.primitives.Floats;
import tech.amikos.chromadb.EFException;
import tech.amikos.chromadb.Embedding;
import tech.amikos.chromadb.EmbeddingFunction;

import java.io.*;
import java.net.URL;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class DefaultEmbeddingFunction implements EmbeddingFunction {
    public static final String MODEL_NAME = "all-MiniLM-L6-v2";
    private static final String ARCHIVE_FILENAME = "onnx.tar.gz";
    private static final String MODEL_DOWNLOAD_URL = "https://chroma-onnx-models.s3.amazonaws.com/all-MiniLM-L6-v2/onnx.tar.gz";
    private static final String MODEL_SHA256_CHECKSUM = "913d7300ceae3b2dbc2c50d1de4baacab4be7b9380491c27fab7418616a16ec3";
    public static final Path MODEL_CACHE_DIR = Paths.get(System.getProperty("user.home"), ".cache", "chroma", "onnx_models", MODEL_NAME);
    private static final Path modelPath = Paths.get(MODEL_CACHE_DIR.toString(), "onnx");
    private static final Path modelFile = Paths.get(modelPath.toString(), "model.onnx");
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
        if (!validateModel()) {
            downloadAndSetupModel();
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
                Path entryPath = extractDir.resolve(entry.getName());
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

    private void downloadAndSetupModel() throws EFException {
        try (InputStream in = new URL(MODEL_DOWNLOAD_URL).openStream()) {
            if (!Files.exists(MODEL_CACHE_DIR)) {
                Files.createDirectories(MODEL_CACHE_DIR);
            }
            Path archivePath = Paths.get(MODEL_CACHE_DIR.toString(), ARCHIVE_FILENAME);
            if (!archivePath.toFile().exists()) {
                System.err.println("Model not found under " + archivePath + ". Downloading...");
                Files.copy(in, archivePath, StandardCopyOption.REPLACE_EXISTING);
            }
            if (!MODEL_SHA256_CHECKSUM.equals(getSHA256Checksum(archivePath.toString()))) {
                throw new RuntimeException("Checksum does not match. Delete the whole directory " + MODEL_CACHE_DIR + " and try again.");
            }
            extractTarGz(archivePath, MODEL_CACHE_DIR);
            archivePath.toFile().delete();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new EFException(e);
        }
    }


    /**
     * Check if the model is present at the expected location
     *
     * @return
     */
    private boolean validateModel() {
        return modelFile.toFile().exists() && modelFile.toFile().isFile();
    }

    @Override
    public Embedding embedQuery(String query) throws EFException {
        try {
            return Embedding.fromList(forward(Collections.singletonList(query)).get(0));
        } catch (OrtException e) {
            throw new EFException(e);
        }
    }

    @Override
    public List<Embedding> embedDocuments(List<String> documents) throws EFException {
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
}
