package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Immutable tenant identifier. */
public final class Tenant {

    private static final Tenant DEFAULT = new Tenant("default_tenant");

    private final String name;

    private Tenant(String name) {
        this.name = validateName(name);
    }

    /**
     * Creates a tenant identifier.
     *
     * @throws NullPointerException     if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public static Tenant of(String name) {
        return new Tenant(name);
    }

    public static Tenant defaultTenant() {
        return DEFAULT;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tenant)) return false;
        return name.equals(((Tenant) o).name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }

    private static String validateName(String name) {
        String value = Objects.requireNonNull(name, "name");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return normalized;
    }
}
