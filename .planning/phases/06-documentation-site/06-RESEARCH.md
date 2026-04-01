# Phase 6: Documentation Site - Research

**Researched:** 2026-03-24
**Domain:** MkDocs Material documentation site, GitHub Pages, Javadoc integration
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- **D-01:** Use MkDocs with Material for MkDocs theme — same stack as chroma-go
- **D-02:** Host on GitHub Pages, deployed via GitHub Actions on push to main
- **D-03:** Custom domain `java.chromadb.dev` via Cloudflare DNS (CNAME to GitHub Pages)
- **D-04:** Site lives in `docs/` directory at repo root (matching chroma-go layout: `docs/mkdocs.yml`, `docs/docs/`, `docs/overrides/`)
- **D-05:** Hybrid organization — use-case guides as primary navigation, with a separate auto-generated Javadoc API reference section
- **D-06:** Mirror chroma-go page structure plus Java-specific extras:
  - Core pages: Client, Auth, Records, Filtering, Search, Embeddings, Cloud Features, Logging
  - Java extras: Schema/CMEK, ID Generators, Transport Options, Migration from v1
- **D-07:** Javadoc generated as part of build, hosted at `/javadoc/` or `/api/` path — guide pages link into it for detailed class/method docs
- **D-08:** Separate examples section (like chroma-go's `go-examples/`) with complete, runnable code walkthroughs organized by topic
- **D-09:** Use `pymdownx.snippets` to include code from actual `.java` files in a snippets/examples directory — examples are compilable and testable in CI
- **D-10:** v2 API only in main documentation — no v1 examples in guide pages
- **D-11:** A single "Migration from v1" page covers the v1→v2 transition
- **D-12:** Match chroma-go aesthetic: black primary palette, Roboto/Roboto Mono fonts, same logo placement and Material theme features
- **D-13:** Include Google Analytics + cookie consent banner, matching chroma-go's pattern
- **D-14:** Material theme features enabled: code copy, code annotate, navigation instant, navigation tracking, navigation indexes

### Claude's Discretion

- CI deployment workflow specifics (GitHub Actions workflow file)
- MkDocs plugin selection beyond search and tags
- Exact nav hierarchy ordering within the hybrid structure
- Snippet file organization (flat vs topic-grouped directories)
- Whether to include EthicalAds integration like chroma-go
- Exact Javadoc integration approach (iframe, separate deploy, or linked external)

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope
</user_constraints>

---

## Summary

This phase builds the documentation site for the chromadb-java-client using MkDocs with Material for MkDocs 9.7.6 (current stable) — the same stack as the chroma-go reference implementation. The site deploys to `java.chromadb.dev` via GitHub Pages with a GitHub Actions workflow that triggers on push to main.

The core deliverables are: (1) the MkDocs site scaffold matching chroma-go's `docs/` layout, (2) guide pages for all API surfaces populated from README.md content, (3) a separate examples section mirroring `go-examples/`, (4) Javadoc API reference co-deployed under `/api/`, and (5) the CI workflow and DNS configuration for `java.chromadb.dev`.

**Important ecosystem note:** MkDocs 2.0 (in pre-release as of Feb 2026) breaks compatibility with Material for MkDocs. The Material team is building Zensical as a replacement, but v9.x remains stable and is the correct choice now. MkDocs 9.7.6 is in maintenance mode but receives bug fixes through at least November 2026 — it is the right choice for this phase.

**Primary recommendation:** Use `mkdocs-material==9.7.6` pinned in a `docs/requirements.txt`. Deploy with `mkdocs gh-deploy --force` from the `docs/` directory in the GitHub Actions workflow. Deploy Javadoc separately by copying `target/site/apidocs/` into the `gh-pages` branch under `/api/` using `peaceiris/actions-gh-pages`.

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| mkdocs-material | 9.7.6 | Full documentation framework (theme + plugins) | Reference implementation (chroma-go) uses it; most widely adopted MkDocs theme |
| pymdownx (bundled) | bundled with material | Syntax highlighting, snippets, tabs, superfences | Ships with mkdocs-material; all extensions used by chroma-go config |
| maven-javadoc-plugin | 3.11.2 | Generate Javadoc HTML from source | Standard Maven plugin; existing pom.xml already has 2.9.1, upgrade needed |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| mkdocs-git-revision-date-localized | latest | "Last updated" timestamps per page | Optional — adds freshness signal to docs pages |
| peaceiris/actions-gh-pages | v4 | Deploy files to gh-pages branch subdirectory | When co-deploying Javadoc alongside MkDocs output |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| mkdocs-material 9.7.6 | Zensical (mkdocs-material team's next-gen) | Zensical not released; not production-ready |
| mkdocs-material 9.7.6 | MkDocs 2.0 + new theme | MkDocs 2.0 drops plugins and breaks Material; TOML config, no release date |
| gh-deploy built-in | peaceiris/actions-gh-pages | `gh-deploy --force` works for MkDocs root; peaceiris needed only for Javadoc subfolder co-deploy |

**Installation (docs/requirements.txt):**

```
mkdocs-material==9.7.6
```

**GitHub Actions install:**

```bash
pip install -r docs/requirements.txt
```

**Javadoc generation (Maven):**

```bash
mvn compile javadoc:javadoc
# Output: target/site/apidocs/
```

**Version verification:** Confirmed 9.7.6 is latest via `pip index versions mkdocs-material` (2026-03-24).

## Architecture Patterns

### Recommended Project Structure

Matches chroma-go layout (D-04):

```
docs/
├── mkdocs.yml              # MkDocs config (site root)
├── requirements.txt        # pip dependencies (mkdocs-material==9.7.6)
├── CNAME                   # java.chromadb.dev (for gh-pages branch)
├── docs/                   # Markdown source pages
│   ├── index.md            # Homepage / overview
│   ├── client.md           # Client setup, builder options
│   ├── auth.md             # BasicAuth, TokenAuth, ChromaTokenAuth
│   ├── records.md          # add/get/update/upsert/delete operations
│   ├── filtering.md        # Where and WhereDocument DSL
│   ├── search.md           # Search API, Knn, Rrf, SearchResult
│   ├── embeddings.md       # All embedding providers
│   ├── cloud-features.md   # Fork, indexingStatus, Cloud vs Self-Hosted table
│   ├── schema.md           # Schema, CMEK, HnswIndexConfig, SpannIndexConfig
│   ├── id-generators.md    # UuidIdGenerator, UlidIdGenerator, Sha256IdGenerator
│   ├── transport.md        # SSL, timeouts, custom OkHttpClient
│   ├── logging.md          # ChromaLogger, ChromaLoggers
│   ├── migration.md        # v1 → v2 migration guide
│   └── assets/
│       ├── images/         # logo.png, favicon.png
│       ├── snippets/       # .java snippet files for pymdownx.snippets
│       └── stylesheets/    # Custom CSS (ethicalads if used)
├── java-examples/          # Runnable example walkthroughs (mirrors go-examples/)
│   ├── quickstart/
│   ├── auth/
│   ├── collections/
│   ├── querying/
│   ├── search/
│   ├── embeddings/
│   └── cloud/
└── overrides/
    └── main.html           # Custom template overrides (analytics JS injection)
```

### Pattern 1: chroma-go mkdocs.yml Template

The chroma-go `mkdocs.yml` is the exact reference (fetched from `raw.githubusercontent.com`). Adapt it for the Java client:

```yaml
# Source: https://github.com/amikos-tech/chroma-go/blob/main/docs/mkdocs.yml
site_name: ChromaDB Java Client
site_url: https://java.chromadb.dev
repo_url: https://github.com/amikos-tech/chromadb-java-client
copyright: "Amikos Tech OOD, 2024 (core ChromaDB contributors)"
theme:
  name: material
  custom_dir: overrides
  palette:
    primary: black
  logo: assets/images/logo.png
  favicon: assets/images/favicon.png
  font:
    text: Roboto
    code: Roboto Mono
  features:
    - content.code.annotate
    - content.code.copy
    - navigation.instant
    - navigation.instant.progress
    - navigation.tracking
    - navigation.indexes
extra:
  homepage: https://www.trychroma.com
  social:
    - icon: fontawesome/brands/github
      link: https://github.com/chroma-core/chroma
    - icon: fontawesome/brands/github
      link: https://github.com/amikos-tech
  analytics:
    provider: google
    property: G-XXXXXXXXXX   # replace with actual property
  consent:
    title: Cookie consent
    description: "We use cookies for analytics purposes. By continuing to use this website, you agree to their use."
markdown_extensions:
  - abbr
  - admonition
  - attr_list
  - md_in_html
  - toc:
      permalink: true
      title: On this page
      toc_depth: 3
  - tables
  - pymdownx.highlight:
      anchor_linenums: true
      line_spans: __span
      pygments_lang_class: true
  - pymdownx.inlinehilite
  - pymdownx.snippets:
      base_path: assets/snippets/
  - pymdownx.superfences
  - pymdownx.tabbed:
      alternate_style: true
  - pymdownx.tasklist:
      custom_checkbox: true
plugins:
  - tags
  - search
```

### Pattern 2: pymdownx.snippets — Including Java Code

The snippets extension embeds content from files. `base_path` is relative to the `docs/` subdirectory (not the `docs/mkdocs.yml` config file location). Place `.java` snippet files at `docs/assets/snippets/`.

**Named sections** (pymdownx 9.7+): Mark regions within a `.java` file using comment markers, then include only that region:

```java
// In docs/assets/snippets/QuickstartExample.java
// --8<-- [start:create-client]
Client client = ChromaClient.builder()
        .baseUrl(System.getenv("CHROMA_URL"))
        .build();
// --8<-- [end:create-client]
```

In a Markdown page:
````markdown
```java
--8<-- "QuickstartExample.java:create-client"
```
````

**Full file inclusion:**
````markdown
```java
--8<-- "QuickstartExample.java"
```
````

**Source:** https://facelessuser.github.io/pymdown-extensions/extensions/snippets/

### Pattern 3: GitHub Actions Deployment Workflow

**Recommended approach for Javadoc + MkDocs co-deployment:**

The `mkdocs gh-deploy --force` command (run from `docs/` dir) writes to the root of `gh-pages`. Javadoc is separately copied into `gh-pages/api/` using `peaceiris/actions-gh-pages` with `destination_dir: api` and `keep_files: true`.

```yaml
# .github/workflows/docs.yml
name: docs
on:
  push:
    branches:
      - main
  workflow_dispatch:

permissions:
  contents: write

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0   # needed for git-revision-date plugin if used

      - name: Configure Git Credentials
        run: |
          git config user.name github-actions[bot]
          git config user.email 41898282+github-actions[bot]@users.noreply.github.com

      - uses: actions/setup-python@v5
        with:
          python-version: 3.x

      - run: echo "cache_id=$(date --utc '+%V')" >> $GITHUB_ENV

      - uses: actions/cache@v4
        with:
          key: mkdocs-material-${{ env.cache_id }}
          path: ~/.cache
          restore-keys: |
            mkdocs-material-

      - run: pip install -r docs/requirements.txt

      - name: Deploy MkDocs
        run: mkdocs gh-deploy --force --config-file docs/mkdocs.yml

      - uses: actions/setup-java@v4
        with:
          java-version: '8'
          distribution: 'temurin'
          cache: maven

      - name: Generate Javadoc
        run: mvn --no-transfer-progress compile javadoc:javadoc

      - name: Deploy Javadoc to /api/
        uses: peaceiris/actions-gh-pages@v4
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./target/site/apidocs
          destination_dir: api
          keep_files: true
```

**Source:** Official MkDocs Material docs + DEV article on Java library documentation (cicirello.github.io pattern)

### Pattern 4: CNAME File for Custom Domain

Place `docs/CNAME` (committed to repo) with content:

```
java.chromadb.dev
```

`mkdocs gh-deploy` copies all files from `docs/` into the built site, so `docs/CNAME` ends up at the root of `gh-pages`. Alternatively, place CNAME in `docs/docs/CNAME` and add it to `mkdocs.yml` under `extra_files`.

The correct location for MkDocs is `docs/docs/CNAME` (inside the docs source dir), or set `extra_files: [CNAME]` in `mkdocs.yml` pointing to a file at the `docs/` root.

**Cloudflare DNS:** Create a CNAME record `java` → `amikos-tech.github.io` with proxy **disabled** (DNS-only, grey cloud). GitHub Pages must provision the Let's Encrypt cert directly.

### Anti-Patterns to Avoid

- **EthicalAds without the JS/CSS files:** chroma-go references `javascripts/ethicalads-init.js` and `stylesheets/ethicalads.css`. These files must exist or MkDocs build will fail. Either include them or omit EthicalAds from the config.
- **Running `mkdocs gh-deploy` from repo root when mkdocs.yml is in `docs/`:** Always pass `--config-file docs/mkdocs.yml` or `cd docs && mkdocs gh-deploy`.
- **Snippets `base_path` mismatch:** `base_path` is evaluated relative to the MkDocs `docs_dir` (the `docs/docs/` folder), not relative to `mkdocs.yml`. Test with `mkdocs build` locally first.
- **Using `peaceiris/actions-gh-pages` without `keep_files: true`:** Without this flag, the action wipes the entire `gh-pages` branch, destroying the MkDocs output deployed earlier in the same workflow.
- **Pinning `mkdocs-material` without a `requirements.txt`:** Using `pip install mkdocs-material` without a pinned version causes non-reproducible builds.
- **maven-javadoc-plugin 2.9.1 (current in pom.xml):** This is 10+ years old. Upgrade to 3.11.2 for Java 8 compatibility and proper HTML output. The current version may not generate correct HTML5 output.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Code inclusion in docs | Copy-paste code blocks manually | `pymdownx.snippets` | Snippets keep docs in sync with compilable source; manual blocks go stale |
| Syntax highlighting | Custom CSS/JS | `pymdownx.highlight` + Pygments | Pygments supports Java natively; Material theme wires it all up |
| Tab groups in docs | HTML `<details>` or JS toggles | `pymdownx.tabbed` | Built-in, accessible, mobile-friendly |
| Docs search | Custom search index | MkDocs `search` plugin | Built-in plugin with lunr.js; zero config |
| gh-pages deployment | Custom git push script | `mkdocs gh-deploy` / `peaceiris/actions-gh-pages` | Both handle branch creation, force-push, commit authorship |
| Custom domain config | Manual gh-pages settings | `CNAME` file in site output | GitHub reads CNAME file on each deploy; manual settings reset on force-push |

**Key insight:** pymdownx.snippets with named section markers (`--8<-- [start:x]` / `--8<-- [end:x]`) is the correct way to include compilable Java snippets into docs. The snippet files live in `docs/assets/snippets/` and can be compiled/tested independently.

## Common Pitfalls

### Pitfall 1: MkDocs 2.0 Confusion

**What goes wrong:** Developer sees MkDocs 2.0 pre-release and attempts to use it; Material for MkDocs is incompatible with MkDocs 2.0.
**Why it happens:** MkDocs 2.0 announcement (Feb 18, 2026) is prominent; version 2.0 drops plugins and changes YAML→TOML config format.
**How to avoid:** Pin `mkdocs-material==9.7.6` in `requirements.txt`. MkDocs 1.x is installed as a transitive dependency of mkdocs-material and will be the correct version.
**Warning signs:** Error messages about "plugins not found" or TOML parse errors when building.

### Pitfall 2: Javadoc Deployment Wiping MkDocs Output

**What goes wrong:** Using `peaceiris/actions-gh-pages` for Javadoc without `keep_files: true` deletes the MkDocs HTML from `gh-pages` branch.
**Why it happens:** `peaceiris/actions-gh-pages` replaces the entire publish branch by default.
**How to avoid:** Always set `keep_files: true` when using `destination_dir` to deploy to a subfolder alongside other content.
**Warning signs:** After workflow run, the site root shows only Javadoc, guide pages return 404.

### Pitfall 3: CNAME File Lost on Redeploy

**What goes wrong:** Custom domain `java.chromadb.dev` breaks after each docs deployment because GitHub's CNAME setting is overwritten.
**Why it happens:** `mkdocs gh-deploy --force` rebuilds the `gh-pages` branch from scratch; if CNAME isn't in the site output, it gets deleted.
**How to avoid:** Place `CNAME` (containing `java.chromadb.dev`) inside `docs/docs/` so MkDocs copies it into the site output, or use `extra_files` in mkdocs.yml.
**Warning signs:** Custom domain disappears from GitHub Pages settings after each deploy.

### Pitfall 4: Snippets Base Path Resolution

**What goes wrong:** `--8<-- "MyExample.java"` fails to find the file at build time.
**Why it happens:** `base_path` in `pymdownx.snippets` config is evaluated relative to the MkDocs `docs_dir` (typically `docs/docs/`), not relative to `mkdocs.yml`.
**How to avoid:** Set `base_path: assets/snippets/` in mkdocs.yml, and place snippet files at `docs/docs/assets/snippets/`. Test locally with `mkdocs build --config-file docs/mkdocs.yml`.
**Warning signs:** `FileNotFoundError` or `SnippetMissing` error during `mkdocs build`.

### Pitfall 5: Cloudflare Proxy Blocks SSL Provisioning

**What goes wrong:** `java.chromadb.dev` shows Cloudflare error page; HTTPS doesn't work.
**Why it happens:** GitHub Pages provisions Let's Encrypt certificates by validating the domain directly. Cloudflare proxy (orange cloud) intercepts the ACME challenge.
**How to avoid:** Set the CNAME DNS record to DNS-only mode (grey cloud / proxy disabled) in Cloudflare.
**Warning signs:** GitHub Pages settings show "Not yet" for HTTPS; domain shows Cloudflare 525 error.

### Pitfall 6: maven-javadoc-plugin Version 2.9.1

**What goes wrong:** Javadoc HTML generation produces malformed or unstyled output; may fail on newer Java.
**Why it happens:** The current pom.xml has version 2.9.1 (released ~2013). Version 3.x is required for modern HTML output.
**How to avoid:** Upgrade to `maven-javadoc-plugin` 3.11.2 in pom.xml for the `javadoc:javadoc` goal (keep `attach-javadocs` execution for Maven Central deployment).
**Warning signs:** Javadoc HTML missing navigation frames or showing render errors.

## Code Examples

Verified patterns from official sources:

### GitHub Actions Docs Workflow (complete)

```yaml
# Source: https://squidfunk.github.io/mkdocs-material/publishing-your-site/
name: docs
on:
  push:
    branches:
      - main
  workflow_dispatch:

permissions:
  contents: write

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Configure Git Credentials
        run: |
          git config user.name github-actions[bot]
          git config user.email 41898282+github-actions[bot]@users.noreply.github.com

      - uses: actions/setup-python@v5
        with:
          python-version: 3.x

      - run: echo "cache_id=$(date --utc '+%V')" >> $GITHUB_ENV

      - uses: actions/cache@v4
        with:
          key: mkdocs-material-${{ env.cache_id }}
          path: ~/.cache
          restore-keys: |
            mkdocs-material-

      - run: pip install -r docs/requirements.txt

      - name: Deploy MkDocs site
        run: mkdocs gh-deploy --force --config-file docs/mkdocs.yml

      - uses: actions/setup-java@v4
        with:
          java-version: '8'
          distribution: 'temurin'
          cache: maven

      - name: Generate Javadoc
        run: mvn --no-transfer-progress compile javadoc:javadoc

      - name: Deploy Javadoc to /api/
        uses: peaceiris/actions-gh-pages@v4
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./target/site/apidocs
          destination_dir: api
          keep_files: true
```

### Snippet Named Section Marker (in Java snippet file)

```java
// Source: https://facelessuser.github.io/pymdown-extensions/extensions/snippets/
// docs/docs/assets/snippets/QuickstartExample.java

// --8<-- [start:build-client]
Client client = ChromaClient.builder()
        .baseUrl(System.getenv("CHROMA_URL"))
        .build();
// --8<-- [end:build-client]

// --8<-- [start:add-records]
collection.add()
        .documents("Hello, world.")
        .ids("id-1")
        .execute();
// --8<-- [end:add-records]
```

### Snippet Inclusion in Markdown

````markdown
<!-- Source: https://facelessuser.github.io/pymdown-extensions/extensions/snippets/ -->
```java
--8<-- "QuickstartExample.java:build-client"
```
````

### maven-javadoc-plugin 3.x in pom.xml

```xml
<!-- Source: https://maven.apache.org/plugins/maven-javadoc-plugin/usage.html -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-javadoc-plugin</artifactId>
    <version>3.11.2</version>
    <executions>
        <execution>
            <id>attach-javadocs</id>
            <goals>
                <goal>jar</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <source>8</source>
        <doclint>none</doclint>
    </configuration>
</plugin>
```

### CNAME File Location

```
# Placed at: docs/docs/CNAME
java.chromadb.dev
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| MkDocs 1.x plugins ecosystem | MkDocs-material 9.7.x (stable / maintenance mode) | Feb 2026: MkDocs 2.0 pre-release incompatible with Material | Pin to 9.7.6; do not upgrade to MkDocs 2.0 |
| `--8<-- filename.ext` (whole file) | Named sections `--8<-- [start:x]` / `--8<-- [end:x]` | pymdownx 9.7 | Can include sub-sections of Java files, not just full files |
| maven-javadoc-plugin 2.9.1 | 3.11.2 | 2.9.1 released ~2013, 3.x is current | Must upgrade for modern HTML output and Java 8+ proper support |
| Manual `git push` to gh-pages | `mkdocs gh-deploy --force` + `peaceiris/actions-gh-pages@v4` | GitHub Actions ecosystem matured | Standard pattern; co-deploy javadoc via `destination_dir` + `keep_files: true` |

**Deprecated/outdated:**

- `maven-javadoc-plugin 2.9.1`: Current pom.xml version is ancient; upgrade to 3.11.2.
- `actions/setup-java@v3` with `adopt` distribution: Project already uses `@v4` with `temurin` — consistent.
- EthicalAds: chroma-go includes it; adding it to Java docs is Claude's discretion — skip unless user confirms the ad placement account exists.

## Open Questions

1. **Google Analytics Property ID**
   - What we know: chroma-go uses `G-NNN722BJKE`
   - What's unclear: Does the Java client get the same GA property or a separate one?
   - Recommendation: Use a placeholder `G-XXXXXXXXXX` in the scaffold; leave a TODO comment for the user to fill in.

2. **EthicalAds Integration**
   - What we know: chroma-go includes `ethicalads-init.js` and `ethicalads.css`
   - What's unclear: Does the user want EthicalAds on the Java docs site?
   - Recommendation: Omit EthicalAds from the initial scaffold (Claude's discretion per context). Adding it later requires account enrollment.

3. **Javadoc at `/api/` or `/javadoc/`**
   - What we know: D-07 says "hosted at `/javadoc/` or `/api/` path"
   - What's unclear: Which path the user prefers
   - Recommendation: Use `/api/` (shorter, conventional for API reference). Plan task should note this choice can be changed by editing `destination_dir` in the workflow.

4. **Phase 7 Working Examples — Timing Dependency**
   - What we know: D-08 says the examples section will contain runnable walkthroughs; Phase 7 creates working examples
   - What's unclear: Phase 6 runs before Phase 7. Should the examples section scaffold exist in Phase 6 with placeholder pages, or wait for Phase 7 to fill it?
   - Recommendation: Create the `java-examples/` directory and nav entries with stub `index.md` pages in Phase 6. Phase 7 fills the content.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Maven (JUnit 4.13.2) — existing project test infra |
| Config file | `pom.xml` |
| Quick run command | `mvn test -Dtest=none -pl .` (no new Java tests in this phase) |
| Full suite command | `mvn test` |
| MkDocs build check | `mkdocs build --strict --config-file docs/mkdocs.yml` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DOCS-BUILD | MkDocs site builds without errors | smoke | `mkdocs build --strict --config-file docs/mkdocs.yml` | No — Wave 0 |
| DOCS-SNIPPETS | All snippet includes resolve (no missing files) | smoke | `mkdocs build --strict --config-file docs/mkdocs.yml` (strict mode fails on missing snippets) | No — Wave 0 |
| DOCS-JAVADOC | Javadoc generation succeeds | smoke | `mvn compile javadoc:javadoc -q` | N/A (pom.xml exists) |
| DOCS-WORKFLOW | GitHub Actions workflow is syntactically valid YAML | lint | `python3 -c "import yaml; yaml.safe_load(open('.github/workflows/docs.yml'))"` | No — Wave 0 |

### Sampling Rate

- **Per task commit:** `mkdocs build --strict --config-file docs/mkdocs.yml` (catches broken pages and snippet errors)
- **Per wave merge:** `mkdocs build --strict --config-file docs/mkdocs.yml && mvn compile javadoc:javadoc -q`
- **Phase gate:** All build checks pass before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `docs/requirements.txt` — pip install target for CI
- [ ] `docs/mkdocs.yml` — MkDocs configuration
- [ ] `docs/docs/index.md` — homepage stub needed for any `mkdocs build` to succeed
- [ ] `docs/docs/assets/snippets/` — directory must exist; snippets plugin errors if base_path doesn't resolve

*(These are scaffold files, not test files — they must exist before any build validation command can run.)*

## Sources

### Primary (HIGH confidence)

- Context7 not applicable (MkDocs is a Python/CLI tool, not a library with Context7 entry)
- `https://raw.githubusercontent.com/amikos-tech/chroma-go/main/docs/mkdocs.yml` — exact chroma-go config fetched verbatim
- `https://github.com/amikos-tech/chroma-go/tree/main/docs/docs` — guide page listing confirmed
- `https://squidfunk.github.io/mkdocs-material/publishing-your-site/` — official GitHub Actions workflow for MkDocs Material deployment
- `https://facelessuser.github.io/pymdown-extensions/extensions/snippets/` — snippets base_path and named sections API

### Secondary (MEDIUM confidence)

- `pip index versions mkdocs-material` (2026-03-24) — confirmed 9.7.6 is latest
- `https://squidfunk.github.io/mkdocs-material/blog/2026/02/18/mkdocs-2.0/` — MkDocs 2.0 incompatibility with Material confirmed
- `https://dev.to/cicirello/deploy-a-documentation-website-for-a-java-library-using-github-actions-197n` — Javadoc subdirectory deployment pattern (`rm -rf gh-pages/api && cp -rf target/site/apidocs/. gh-pages/api`)
- `https://maven.apache.org/plugins/maven-javadoc-plugin/` — version 3.11.2 confirmed as current

### Tertiary (LOW confidence)

- `peaceiris/actions-gh-pages@v4` `keep_files: true` behavior — cross-referenced from search results but not directly from official peaceiris docs. Verify `keep_files` parameter name in the action's README before coding.

## Metadata

**Confidence breakdown:**

- Standard stack: HIGH — pip confirmed 9.7.6; chroma-go config fetched verbatim from GitHub
- Architecture: HIGH — directly mirrors chroma-go layout per D-04; patterns verified from official MkDocs Material docs
- Pitfalls: HIGH for items 1-5 (MEDIUM for Pitfall 6 — javadoc version issue is well-known but not verified against latest plugin docs)
- Validation: MEDIUM — build smoke checks are correct; peaceiris `keep_files` parameter needs runtime verification

**Research date:** 2026-03-24
**Valid until:** 2026-06-24 (stable ecosystem; MkDocs 9.7.x in maintenance mode until Nov 2026; only risk is peaceiris action update)
