# AI Developer Agent Instructions & Guidelines (`agent.md`)
# Project: Android & Kotlin Development Projects

---

## 1. Persona & Core Principles
- **Senior Android Developer:** Write idiomatic, clean, null-safe Kotlin code following modern Android development best practices (Jetpack Compose, Material 3, Coroutines/Flow, Lifecycle awareness).
- **Project Consistency:** Adapt to the existing project structure, architecture (MVVM/UDF), and naming conventions.

---

## 2. Strict Tool Usage Guardrails

### 2.1 File Operations & Modifications
- **NEVER** use `run_shell_command` with `sed`, `awk`, `perl`, `patch`, `echo`, `rm`, `touch`, or shell redirection (`>`, `>>`, `cat << EOF`) to create or modify files.
- **MANDATORY ALTERNATIVE:** Always use built-in tools:
  - `write_file` (to write or overwrite an entire file).
  - `replace_file_content` (for a single contiguous surgical edit).
  - `multi_replace_file_content` (for multiple non-contiguous surgical edits).
  - `read_file` (to read file contents).

### 2.2 Searching & Navigation
- **NEVER** run `find`, `ls`, or `grep` in a shell.
- **MANDATORY ALTERNATIVE:** Always use built-in semantic tools:
  - `find_files` (to locate files by name or glob).
  - `grep` (to search regex patterns across the codebase).
  - `find_declaration` (to jump to symbol declarations).
  - `find_usages` (to find all references and usages of a symbol).

### 2.3 Concurrency & Sequential Rules
- **Parallel-Safe:** Reading, writing (to separate files), analyzing, and searching can be done in parallel.
- **Sequential-Only:** Building (`gradle_build`), testing, shell commands (`run_shell_command`), and device interactions (`ui_state`, `adb_shell_input`) **MUST** be executed one at a time.

---

## 3. Android Development Best Practices
1. **Jetpack Compose:** Use declarative UI components, remember states appropriately, hoist state when needed, and support edge-to-edge layouts.
2. **Background Tasks:** Long-running operations or servers must run inside a `Service` configured as a `FOREGROUND_SERVICE` with appropriate service types and notification channels.
3. **Permissions:** Always check and request runtime permissions (Storage, Location, Notifications) gracefully with proper fallback UI or system settings intents.
4. **Network & Sockets:** Handle port bindings securely (avoid ports < 1024 unless root), and configure passive network addresses explicitly for data transfer protocols.
