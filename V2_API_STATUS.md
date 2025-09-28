# V2 API Implementation Status

## Overview
This directory contains an experimental implementation of the ChromaDB v2 API client.

**⚠️ Important:** The v2 API does not yet exist in ChromaDB. This implementation is based on anticipated v2 API design and is provided for experimental/preview purposes only.

## Current Status

### What's Implemented
- Basic client structure (`ServerClient`, `CloudClient`)
- Authentication providers (Basic, Token, ChromaToken)
- Model classes for v2 operations
- Collection operations interface
- Query builder pattern

### Known Issues
1. **API Endpoints:** Currently modified to use `/api/v1` endpoints as a temporary workaround
2. **Tenant/Database Support:** v2 expects multi-tenancy which v1 doesn't support
3. **Response Models:** Field names and structure differ between v1 and v2
4. **Tests:** Most tests will fail against current ChromaDB versions

## CI/CD Configuration

The repository includes GitHub Actions workflows for v2 API testing:
- `.github/workflows/v2-api-tests.yml` - Main test workflow
- `.github/workflows/v2-api-pr-validation.yml` - PR validation
- `.github/workflows/v2-api-nightly.yml` - Nightly compatibility tests
- `.github/workflows/v2-api-release.yml` - Release workflow

**Note:** These workflows are currently expected to fail until ChromaDB implements the actual v2 API.

## Usage

### For Experimental Development Only
```java
// This code will not work with current ChromaDB versions
ServerClient client = ServerClient.builder()
    .baseUrl("http://localhost:8000")
    .auth(AuthProvider.none())
    .build();

// Operations will fail or behave unexpectedly
Collection collection = client.createCollection("my_collection");
```

## Recommendations

1. **Do not use in production** - This is experimental code
2. **Use v1 Client** - For all actual ChromaDB operations, use the stable v1 client in `tech.amikos.chromadb.Client`
3. **Monitor ChromaDB releases** - Watch for official v2 API announcements

## Future Work

When ChromaDB releases the actual v2 API:
1. Update all endpoints from the temporary v1 paths
2. Align model classes with actual v2 response structures
3. Implement proper tenant/database handling
4. Update tests to match real v2 behavior
5. Remove this warning documentation

## Contributing

If you're interested in the v2 API development:
- Check ChromaDB's official repository for v2 API proposals
- Test against ChromaDB development branches if available
- Report issues specific to v2 API design (not current failures)