package tech.amikos.chromadb.v2;

import java.util.Objects;

/** Immutable database identifier. */
public final class Database {

    private static final Database DEFAULT = new Database("default_database");

    private final String name;

    private Database(String name) {
        this.name = validateName(name);
    }

    /**
     * Creates a database identifier.
     *
     * @throws NullPointerException     if {@code name} is {@code null}
     * @throws IllegalArgumentException if {@code name} is blank
     */
    public static Database of(String name) {
        return new Database(name);
    }

    public static Database defaultDatabase() {
        return DEFAULT;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Database)) return false;
        return name.equals(((Database) o).name);
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
