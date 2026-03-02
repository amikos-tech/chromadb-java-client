package tech.amikos.chromadb.v2;

import java.util.Objects;
import java.util.regex.Pattern;

/** Customer-managed encryption key configuration. */
public final class Cmek {

    private static final Pattern GCP_RESOURCE_PATTERN = Pattern.compile(
            "^projects/[^/]+/locations/[^/]+/keyRings/[^/]+/cryptoKeys/[^/]+$"
    );

    private final CmekProvider provider;
    private final String resource;

    private Cmek(CmekProvider provider, String resource) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.resource = requireNonBlank("resource", resource);
        validatePattern(provider, this.resource);
    }

    public static Cmek gcpKms(String resourceName) {
        return new Cmek(CmekProvider.GCP, resourceName);
    }

    public CmekProvider getProvider() {
        return provider;
    }

    public String getResource() {
        return resource;
    }

    public void validatePattern() {
        validatePattern(provider, resource);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cmek)) return false;
        Cmek cmek = (Cmek) o;
        return provider == cmek.provider && Objects.equals(resource, cmek.resource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, resource);
    }

    @Override
    public String toString() {
        return "Cmek{" + "provider=" + provider + ", resource='" + resource + '\'' + '}';
    }

    private static void validatePattern(CmekProvider provider, String resource) {
        if (provider == CmekProvider.GCP) {
            if (!GCP_RESOURCE_PATTERN.matcher(resource).matches()) {
                throw new IllegalArgumentException(
                        "invalid GCP CMEK resource format: expected "
                                + "projects/{project}/locations/{location}/keyRings/{keyRing}/cryptoKeys/{key}"
                );
            }
            return;
        }
        throw new IllegalArgumentException("unsupported CMEK provider: " + provider);
    }

    private static String requireNonBlank(String fieldName, String value) {
        if (value == null) {
            throw new NullPointerException(fieldName);
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }
}
