# AGENTS.md

This repo ships **HytaleDev**: an IntelliJ IDEA plugin that adds a Hytale project generator to the New Project wizard, plus a shared core library for template rendering and server tooling. This document defines code conventions, structure, and the expected development workflow.

## Non negotiables

1) Keep it modern: use the current JetBrains Platform APIs and the IntelliJ Platform Gradle Plugin (2.x). Avoid legacy wizard APIs unless there is no viable alternative.
2) Keep it minimal: ship the smallest working surface, then iterate. Prefer clear code over clever abstractions.
3) Keep core pure: `core` must not depend on IntelliJ classes. IntelliJ glue lives in `idea-plugin`.
4) Never block the UI thread: any IO, downloads, unzip, jar scanning, Gradle calls must run off the EDT.

## Repository structure

- `core`: Kotlin library
    - Template repositories and rendering
    - Manifest model and writer (matches `example-manifest.json`)
    - Server directory validation
    - Server jar inspector (jar is treated as a ZIP; heuristics only)
- `idea-plugin`: IntelliJ plugin
    - New Project wizard steps and UI
    - Settings UI and persistent state
    - Run configuration type and before run tasks
    - Actions such as “Inspect Server Jar”
- `cli` (optional): Thin wrapper over `core`
- `docs/`: design notes and investigation reports
- `templates/` (if present): template sources, otherwise bundled under plugin resources

## Language and style

### Kotlin
- Kotlin is the default language for all new code.
- Use idiomatic Kotlin:
    - Prefer `val` over `var`.
    - Prefer `when`, sealed types, and data classes for modeling state.
    - Prefer `Path` and `kotlin.io.path.*` over `File` APIs.
    - Avoid extension abuse; only use extensions when they clarify call sites.
- Nullability:
    - Do not use `!!` except in tests or when a prior check makes it provably safe; document the invariant.
- Exceptions:
    - Fail fast in core when invariants are broken; surface user friendly messages in the IDE layer.

### Formatting
- Follow IntelliJ Kotlin formatter defaults.
- Keep functions short. If a function is doing UI, IO, and parsing, it is doing too much.
- Naming:
    - Packages: `sh.harold.hytaledev.*`
    - Classes: `PascalCase`
    - Functions and properties: `camelCase`
    - Constants: `UPPER_SNAKE_CASE`

### Dependencies
- Add dependencies only if they reduce complexity. No “framework tours”.
- `core` dependencies must be JVM portable.
- IntelliJ specific dependencies are allowed only in `idea-plugin`.

## IntelliJ Platform conventions

### Services and state
- Use modern service access patterns:
    - `service<T>()` for application or project services.
    - `PersistentStateComponent` with `@State` for settings.
- Settings must be deterministic and migration safe:
    - Version settings objects.
    - Provide defaults for new fields.

### Threading and progress
- UI thread rules:
    - Never perform network IO, file system scans, unzip, jar inspection, or Gradle execution on the EDT.
- Use IntelliJ progress infrastructure:
    - Background tasks for slow operations.
    - Show concise progress text; keep logs in `idea.log` and user visible notifications minimal.

### Logging and notifications
- Use `com.intellij.openapi.diagnostic.Logger` in the IDE module.
- Notifications:
    - One notification per failure, actionable wording.
    - Offer “Open Settings”, “Open Folder”, “Copy Details” when appropriate.

### Wizard implementation
- Use the New Project Wizard APIs, with composable steps and validation.
- Validation should be immediate and local:
    - Block Create when required fields are invalid.
    - Provide inline errors near the field, not modal dialogs.

### Run configurations
- The wizard must generate a run configuration for the dev server.
- Before run tasks should be explicit and ordered:
    1) Build plugin jar
    2) Copy jar into server mods folder
    3) Validate server layout
    4) Launch server

## Template system conventions

### Template repositories
Support these providers:
- builtin: packaged with the plugin
- remoteZip: URL downloaded and cached
- localDir: templates on disk
- archiveZip: local ZIP file

Caching rules:
- Cache by URL and content hash.
- Provide a “Clear cache” action in settings.
- Do not silently re download on every wizard open.

### Template descriptor
Each template includes a descriptor JSON:
- Keep schema versioned.
- Properties must declare type, default, visibility, and validation.
- Rendering must be transparent:
    - If a file is generated, record its source template and applied substitutions in logs.
- Do not invent a DSL. JSON plus a proven templating engine is enough.

### Manifest generation
- `manifest.json` must be written from the wizard model and match the schema in `example-manifest.json` exactly.
- The generated project must compile without manual edits.

## Server jar investigation tooling

### Principles
- Jar inspection is heuristic:
    - Detect strings, class names, and resources that hint at plugin loading.
    - Never claim certainty if it is not provable from the jar.
- Output must be auditable:
    - Show what was found, where it was found, and why it matters.

### Implementation constraints
- Treat the jar as a ZIP filesystem.
- Avoid decompilation in v1. String scanning plus resource listing is sufficient.

## Testing workflow

### Manual testing
Primary loop:
- `./gradlew :idea-plugin:runIde`
- In the sandbox IDE:
    1) File → New → Project → Hytale
    2) Generate a Plugin project
    3) Confirm Gradle sync completes
    4) Confirm run configuration exists and launches

### Automated checks
Run before merging:
- `./gradlew test`
- `./gradlew :idea-plugin:verifyPluginProjectConfiguration`
- `./gradlew :idea-plugin:verifyPluginStructure`
- `./gradlew :idea-plugin:verifyPlugin`

### Test expectations
- Core module tests should cover:
    - Template descriptor parsing
    - Placeholder substitution
    - Manifest serialization
    - Server layout validation
- IDE module tests are optional in v1; prefer high value core tests first.

## PR and change hygiene

- Every change should either:
    - Improve v1 usability, or
    - Reduce future maintenance cost.
- Avoid broad refactors without user visible wins.
- Include a short checklist in PR descriptions:
    - Wizard path tested in `runIde`
    - Generated project builds
    - Manifest matches schema
    - No EDT blocking introduced

## “Modern” definition for this repo
Modern means: Kotlin first; IntelliJ Platform Gradle Plugin 2.x; New Project Wizard APIs; background tasks for IO; `Path` based filesystem code; versioned settings and descriptors; minimal dependencies; transparent behavior.
