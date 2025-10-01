package tech.amikos.chromadb.v2;

import com.google.gson.annotations.SerializedName;

public class UpdateTenantRequest {
    @SerializedName("resource_name")
    private final String resourceName;

    private UpdateTenantRequest(Builder builder) {
        this.resourceName = builder.resourceName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String resourceName;

        public Builder resourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public UpdateTenantRequest build() {
            return new UpdateTenantRequest(this);
        }
    }
}