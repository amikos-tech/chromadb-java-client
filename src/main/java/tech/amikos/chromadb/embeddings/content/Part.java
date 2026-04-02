package tech.amikos.chromadb.embeddings.content;

/**
 * A single part of a {@link Content} object, representing one modality (text, image, audio, video).
 *
 * <p>For text parts, {@link #getText()} returns the text and {@link #getSource()} is null.
 * For binary parts (image, audio, video), {@link #getSource()} returns the binary source
 * and {@link #getText()} is null.</p>
 */
public final class Part {

    private final Modality modality;
    private final String text;
    private final BinarySource source;

    private Part(Modality modality, String text, BinarySource source) {
        this.modality = modality;
        this.text = text;
        this.source = source;
    }

    public static Part text(String text) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        return new Part(Modality.TEXT, text, null);
    }

    public static Part image(BinarySource source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        return new Part(Modality.IMAGE, null, source);
    }

    public static Part audio(BinarySource source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        return new Part(Modality.AUDIO, null, source);
    }

    public static Part video(BinarySource source) {
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        return new Part(Modality.VIDEO, null, source);
    }

    public Modality getModality() {
        return modality;
    }

    public String getText() {
        return text;
    }

    public BinarySource getSource() {
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Part)) return false;
        Part part = (Part) o;
        if (modality != part.modality) return false;
        if (text != null ? !text.equals(part.text) : part.text != null) return false;
        return source != null ? source.equals(part.source) : part.source == null;
    }

    @Override
    public int hashCode() {
        int result = modality.hashCode();
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + (source != null ? source.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        if (modality == Modality.TEXT) {
            return "Part{modality=TEXT, text='" + text + "'}";
        }
        return "Part{modality=" + modality + ", source=" + source + '}';
    }
}
