package tech.amikos.chromadb.embeddings.content;

import java.util.Arrays;

/**
 * Immutable binary content source for multimodal embedding parts (images, audio, video).
 *
 * <p>Exactly one source field is set per instance. Use the static factory methods to create
 * instances from URLs, file paths, base64-encoded strings, or raw byte arrays.</p>
 */
public final class BinarySource {

    private final String url;
    private final String filePath;
    private final String base64Data;
    private final byte[] data;

    private BinarySource(String url, String filePath, String base64Data, byte[] data) {
        this.url = url;
        this.filePath = filePath;
        this.base64Data = base64Data;
        this.data = data;
    }

    public static BinarySource fromUrl(String url) {
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }
        return new BinarySource(url, null, null, null);
    }

    public static BinarySource fromFile(String path) {
        if (path == null) {
            throw new IllegalArgumentException("path must not be null");
        }
        return new BinarySource(null, path, null, null);
    }

    public static BinarySource fromBase64(String base64) {
        if (base64 == null) {
            throw new IllegalArgumentException("base64 must not be null");
        }
        return new BinarySource(null, null, base64, null);
    }

    public static BinarySource fromBytes(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        return new BinarySource(null, null, null, Arrays.copyOf(data, data.length));
    }

    public String getUrl() {
        return url;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getBase64Data() {
        return base64Data;
    }

    public byte[] getBytes() {
        return data == null ? null : Arrays.copyOf(data, data.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BinarySource)) return false;
        BinarySource that = (BinarySource) o;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        if (filePath != null ? !filePath.equals(that.filePath) : that.filePath != null) return false;
        if (base64Data != null ? !base64Data.equals(that.base64Data) : that.base64Data != null) return false;
        return Arrays.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (filePath != null ? filePath.hashCode() : 0);
        result = 31 * result + (base64Data != null ? base64Data.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(data);
        return result;
    }

    @Override
    public String toString() {
        if (url != null) return "BinarySource{url='" + url + "'}";
        if (filePath != null) return "BinarySource{filePath='" + filePath + "'}";
        if (base64Data != null) return "BinarySource{base64Data=<" + base64Data.length() + " chars>}";
        if (data != null) return "BinarySource{data=<" + data.length + " bytes>}";
        return "BinarySource{empty}";
    }
}
