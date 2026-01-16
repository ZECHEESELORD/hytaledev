Implement an IntelliJ IDEA plugin named “HytaleDev” that adds a “Hytale” entry to the New Project wizard and generates a ready to run Hytale plugin project from templates, modeled after MinecraftDev’s creator and UI.

1) Repo layout (multi module; keep v1 deliverable in :idea-plugin, but share logic in :core)
   1.1) Root
       settings.gradle.kts
       build.gradle.kts
       gradle.properties
       README.md
       docs/
           wizard-ui-spec.md
           template-format.md
           serverjar-investigation.md
   1.2) core (pure Kotlin library; no IntelliJ deps)
   src/main/kotlin/...
       templating/
           TemplateRepository.kt
           TemplateProvider.kt (builtin, remoteZip, localDir, archiveZip)
           TemplateCache.kt
           TemplateDescriptor.kt
           PropertyModel.kt
           VelocityRenderer.kt
           ProjectGenerator.kt
           Finalizer.kt
   manifest/
       HytaleManifest.kt (matches example-manifest.json schema exactly)
       ManifestWriter.kt
   server/
       ServerLayout.kt
       ServerValidator.kt
       ServerJarInspector.kt (library; reads jar as zipfs; string scan helpers)
   1.3) idea-plugin (IntelliJ Platform plugin; Kotlin)
   src/main/resources/META-INF/plugin.xml
   src/main/resources/icons/hytale.svg
   src/main/resources/templates/builtin/... (bundled templates)
   src/main/kotlin/...
       wizard/
           HytaleNewProjectWizard.kt
           steps/
           RootStep.kt
           BaseAndGitStep.kt
           GroupSelectorStep.kt
           TemplateSelectorStep.kt
           BuildSystemStep.kt
           JdkStep.kt
           ManifestStep.kt
           ServerConfigStep.kt
           SummaryCommentStep.kt
   model/
       WizardState.kt (PropertyGraph backed; mirrors TemplateDescriptor properties)
   settings/
       HytaleDevSettingsConfigurable.kt
       TemplateRepositoriesPanel.kt
       HytaleDevSettingsState.kt (PersistentStateComponent)
   run/
       HytaleServerRunConfigurationType.kt
       HytaleServerRunConfiguration.kt
       HytaleBeforeRunTaskProvider.kt (build, copy jar, validate server)
   actions/
       InspectServerJarAction.kt (opens file chooser; runs ServerJarInspector; shows report)
       OpenTemplateRepoAction.kt (optional)
   1.4) cli (v2 placeholder, optional in v1; thin wrapper over :core)
   src/main/kotlin/...
       Main.kt (hytaledev init|doctor|inspect-jar; keep minimal)

2) New Project wizard behavior (match MinecraftDev’s feel and layout)
   2.1) Wizard entry
   Left sidebar shows “Hytale” under Generators.
   Implement as a framework generator using the New Project Wizard API (not legacy).
   2.2) Single screen, stacked sections (like the MinecraftDev screenshot)
   Section A: Name, Location, Create Git repository (native fields).
   Section B: Groups selector (segmented buttons): Plugin, Mod, Proxy.
   v1 implements only Plugin end to end; Mod and Proxy are visible but disabled with a hint “coming soon”.
   Section C: Templates selector (segmented buttons or chips) populated from template repositories:
   Builtin templates always available; remote/local/archive are user configurable in Settings.
   Section D: Build System and Language:
   Build system: Gradle or Maven (v1 implement Gradle; Maven option visible but disabled with hint).
   Language: Kotlin or Java (v1 implement Kotlin; Java optional if template supports it).
   Section E: JDK selector:
   Enforce minimum JDK version required by the Hytale server toolchain; validate and block Create if too low.
   Section F: Hytale Plugin Manifest (maps 1:1 to example-manifest.json)
   Required:
   Group; Name; Version; Main; Description.
   Optional:
   Website; Authors (list); ServerVersion; DisabledByDefault; IncludesAssetPack.
   Dependencies, OptionalDependencies, LoadBefore as editable maps.
   SubPlugins editor (advanced foldout; v1 can support single subplugin entry; full list support is fine if easy).
   Defaults and derivations:
   Name defaults from Project Name.
   Group defaults from Build System groupId.
   Main class defaults to “<groupId>.<projectNameNormalized>.<MainClassName>”, validated as a class FQN.
   Version defaults to 1.0.0.
   ServerVersion defaults to “*”.
   Section G: Server config (v1 minimal; no downloading)
   Mode: “Use existing server directory”.
   Fields:
   Server directory picker.
   Assets.zip path picker (auto fill if found under the server directory).
   A “Validate” button that checks required files exist and prints concise errors.
   Store last used values in settings so the next wizard run is faster.
   Section H: Summary / hints
   A comment step that explains where the plugin jar will be output and how Run works.

3) Template system (model after MinecraftDev creator; implement only what v1 needs)
   3.1) Template repository model
   Providers:
   builtin: reads templates from plugin resources
   remoteZip: downloads ZIP from URL into cache; supports a $version placeholder in URL
   localDir: reads a flat directory
   archiveZip: reads templates from a local ZIP file
   Settings UI: add, remove, reorder repositories; per repo provider config; clear cache button.
   3.2) Descriptor file
   Each template root contains a descriptor JSON:
   filename: <anything>.hydev.template.json
   fields:
   version (int); id (string); label (string); group (string)
   properties: list of property definitions (name, type, label, default, remember, editable, visible, validator, options)
   files: list of template file mappings (template path, destination path, openInEditor, reformat, condition)
   finalizers: list of post create actions (import_gradle_project, run_gradle_tasks, git_add_all, create_run_configuration, copy_plugin_to_server)
   Render engine:
   Use Apache Velocity for templates.
   Destination paths and conditions are also Velocity evaluated.
   Property types required for v1:
   string; boolean; inline_string_list; class_fqn; build_system_properties (groupId/artifactId/version); jdk.
   3.3) Builtin templates (ship at least one)
   templates/builtin/hytale-plugin-basic/
   <descriptor>.hydev.template.json
   gradle wrapper + build.gradle.kts + settings.gradle.kts
   src/main/kotlin/... main class template
   src/main/resources/manifest.json (generated from wizard fields)
   .gitignore
   README.md
   Template must produce a project that builds a jar containing manifest.json and a stub main class matching Manifest.Main.

4) Project generation pipeline (what happens when the user clicks Create)
   4.1) Collect wizard state into TemplateDescriptor property map.
   4.2) Resolve selected template from repositories; unzip/read into memory.
   4.3) Generate project files:
   Render each file entry; write to destination; create directories; set permissions if needed.
   Generate manifest.json exactly matching the example schema and values chosen in the wizard.
   Open key files in editor: build.gradle.kts; main class; manifest.json.
   4.4) Finalizers (sequential)
   Initialize git if selected.
   Import Gradle project and trigger sync.
   Run Gradle tasks: “build” (optional in v1; can be manual).
   Create a Run configuration “Hytale Server (dev)” that launches the server jar with assets (from ServerConfigStep).
   Add a Before Run task:
   Gradle build; then copy built plugin jar into <serverDir>/mods (create directory if missing).

5) “Investigate the server jar” deliverables (explicit, even if used manually in v1)
   5.1) Provide docs/serverjar-investigation.md with concrete instructions:
   1. Obtain HytaleServer.jar and Assets.zip.
   2. Use ServerJarInspector to scan for:
   plugin loading entry points; expected manifest file name; plugins directory names; required args (assets); minimum Java version hints.
   3. Record findings in docs/serverjar-report.md (committed).
   5.2) Implement ServerJarInspector (core + IDEA action)
   Input: path to HytaleServer.jar.
   Output: a structured report shown in IDE:
   detected strings and class names relevant to plugin loading; likely plugin directory; any manifest filename references; any “--assets” references.
   Keep it heuristic and transparent; do not hardcode claims without evidence in output.

6) Non goals for v1 (explicitly do not implement)
   6.1) No mod templates beyond placeholders in UI.
   6.2) No automatic server downloading or OAuth workflows.
   6.3) No bytecode tooling, inspections, or decompiler integrations.

7) Acceptance criteria (v1 “usable”)
   7.1) After installing the plugin, File → New → Project shows “Hytale”.
   7.2) Selecting Hytale → Plugin → Basic Template generates a Gradle project that syncs successfully.
   7.3) The generated jar contains manifest.json matching the example schema.
   7.4) The wizard creates a Run configuration that can be executed to start the server (assuming user provided a valid server directory and assets).
   7.5) Before Run builds and copies the plugin jar into the server mods folder.

Use the attached example-manifest.json as the authoritative manifest schema and field set.
