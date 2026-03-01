package tech.amikos.chromadb.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable authenticated identity details. */
public final class Identity {

    private final String userId;
    private final String tenant;
    private final List<String> databases;

    /**
     * Creates identity details.
     *
     * @param userId authenticated user identifier
     * @param tenant active tenant identifier
     * @param databases visible databases for the authenticated user; may be empty
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code userId}, {@code tenant}, or any database name is blank
     */
    public Identity(String userId, String tenant, List<String> databases) {
        this.userId = normalizeRequired("userId", userId);
        this.tenant = normalizeRequired("tenant", tenant);
        this.databases = normalizeDatabases(databases);
    }

    public String getUserId() {
        return userId;
    }

    public String getTenant() {
        return tenant;
    }

    public List<String> getDatabases() {
        return databases;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Identity)) return false;
        Identity identity = (Identity) o;
        return userId.equals(identity.userId)
                && tenant.equals(identity.tenant)
                && databases.equals(identity.databases);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, tenant, databases);
    }

    @Override
    public String toString() {
        return "Identity{"
                + "userId='" + userId + '\''
                + ", tenant='" + tenant + '\''
                + ", databases=" + databases
                + '}';
    }

    private static String normalizeRequired(String fieldName, String value) {
        String nonNullValue = Objects.requireNonNull(value, fieldName);
        String normalized = nonNullValue.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalized;
    }

    private static List<String> normalizeDatabases(List<String> databases) {
        Objects.requireNonNull(databases, "databases");
        List<String> normalized = new ArrayList<String>(databases.size());
        for (int i = 0; i < databases.size(); i++) {
            normalized.add(normalizeRequired("databases[" + i + "]", databases.get(i)));
        }
        return Collections.unmodifiableList(normalized);
    }
}
