package tech.amikos.chromadb;

public class Utils {
    public static class SemanticVersion implements Comparable<SemanticVersion> {
        private final int major;
        private final int minor;
        private final int patch;

        public SemanticVersion(String version) {
            String[] parts = version.split("\\.");
            this.major = Integer.parseInt(parts[0]);
            this.minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            this.patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        }

        @Override
        public int compareTo(SemanticVersion other) {
            if (this.major != other.major) {
                return Integer.compare(this.major, other.major);
            }
            if (this.minor != other.minor) {
                return Integer.compare(this.minor, other.minor);
            }
            return Integer.compare(this.patch, other.patch);
        }
    }
}
