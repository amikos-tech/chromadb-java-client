package tech.amikos.chromadb.embeddings.bm25;

/**
 * Murmur3 x86 32-bit hash implementation.
 *
 * <p>This is an inline implementation (not Guava) to avoid pulling in a large dependency.
 * It matches the output of Python's {@code mmh3.hash(text, seed, signed=True)} and
 * Go's {@code github.com/spaolacci/murmur3}.</p>
 */
public final class Murmur3 {

    private static final int C1 = 0xcc9e2d51;
    private static final int C2 = 0x1b873593;

    private Murmur3() {
    }

    /**
     * Computes Murmur3 x86 32-bit hash of the given data with the specified seed.
     *
     * @param data the bytes to hash
     * @param seed the hash seed
     * @return signed 32-bit hash value
     */
    public static int hash32(byte[] data, int seed) {
        int h1 = seed;
        int len = data.length;
        int nblocks = len / 4;

        // body: process 4-byte blocks
        for (int i = 0; i < nblocks; i++) {
            int k1 = getBlock32(data, i * 4);
            k1 *= C1;
            k1 = Integer.rotateLeft(k1, 15);
            k1 *= C2;

            h1 ^= k1;
            h1 = Integer.rotateLeft(h1, 13);
            h1 = h1 * 5 + 0xe6546b64;
        }

        // tail: handle remaining bytes
        int tail = nblocks * 4;
        int k1 = 0;
        switch (len & 3) {
            case 3:
                k1 ^= (data[tail + 2] & 0xff) << 16;
                // fall through
            case 2:
                k1 ^= (data[tail + 1] & 0xff) << 8;
                // fall through
            case 1:
                k1 ^= (data[tail] & 0xff);
                k1 *= C1;
                k1 = Integer.rotateLeft(k1, 15);
                k1 *= C2;
                h1 ^= k1;
                break;
            default:
                break;
        }

        // finalization
        h1 ^= len;
        h1 = fmix32(h1);
        return h1;
    }

    private static int fmix32(int h) {
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h;
    }

    private static int getBlock32(byte[] data, int offset) {
        return (data[offset] & 0xff)
                | ((data[offset + 1] & 0xff) << 8)
                | ((data[offset + 2] & 0xff) << 16)
                | ((data[offset + 3] & 0xff) << 24);
    }
}
