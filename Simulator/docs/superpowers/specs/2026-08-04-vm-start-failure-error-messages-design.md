# VM Start Failure Error Messages Design

**Date:** 2026-08-04
**Project:** armOS Simulator Suite
**Scope:** GUI.java - comprehensive error handling for VM/simulator start failures

---

## Problem Statement

When a VM/simulator fails to start, users currently see raw Java exceptions (stack traces) in the message area. These are not actionable for most users and don't clearly indicate what went wrong or how to fix it.

---

## Failure Points & Required Error Messages

### 1. CPU/Simulator Class Loading (`setCpu(String)`)

| Failure | User Message | Technical Details |
|---------|--------------|-------------------|
| ClassNotFoundException | "Failed to load **{cpuName}** simulator: class `{className}` not found. Check `j51.conf` for valid CPU class names." | Class name, stack trace |
| InstantiationException / IllegalAccessException | "Failed to instantiate **{cpuName}**: {reason}. Ensure class implements `iCPU` and has a public no-arg constructor." | Exception type, message, stack trace |
| Reset failure | "Failed to reset **{cpuName}**: {exception message}" | Exception, stack trace |
| Panel initialization failure | "Failed to initialize **{panelTitle}** for **{cpuName}**: {reason}" | Panel class, exception, stack trace |

### 2. Program File Loading (`performFileOpen` / `tryLoadFile`)

| Failure | User Message | Technical Details |
|---------|--------------|-------------------|
| File not found | "File not found: `{path}`" | Path, FileNotFoundException |
| Unsupported extension | "Unsupported file format: `.{ext}`. Supported formats: .hex, .json (Intel HEX), .bin (ELF/raw), .class, .jar, .jll" | Extension, path |
| Intel HEX parse error | "Invalid Intel HEX file `{file}`: {specific error}" | Line number, error type (checksum, record type, format) |
| ELF parse error | "Invalid ELF binary `{file}`: {reason}. Falling back to raw binary load..." | ELF parsing exception |
| Raw binary load error | "Failed to load raw binary `{file}`: {reason}" | IOException, file size |
| Java .class load error | "Failed to load Java class `{file}`: {reason}" | ClassData parsing exception |
| Java .jar load error | "Failed to load Java JAR `{file}`: {reason}" | JarFile/Manifest parsing exception |
| JLL load error | "Failed to load JLL file `{file}`: {reason}" | CodeFile parsing exception |

### 3. Execution Start (`actionDebugGo`, `actionDebugTrace`, `actionDebugStep`)

| Failure | User Message | Technical Details |
|---------|--------------|-------------------|
| CPU not initialized | "No simulator loaded. Select a CPU from the CPU menu first." | N/A |
| Execution exception | "Failed to start execution: {exception message}" | Exception type, message, stack trace |

---

## Implementation Design

### 1. Error Display Helper Method

Add to `GUI.java`:

```java
private void showError(String userMessage, Exception ex) {
    // User-friendly message in GUI message area
    messages("[ERROR] " + userMessage);
    
    // Technical details to console/log
    if (ex != null) {
        Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, userMessage, ex);
        ex.printStackTrace(System.out);
    }
}
```

### 2. Enhanced `setCpu(String)` Error Handling

Wrap each step with specific catch blocks:

```java
try {
    Class c = Class.forName(name);
    iCPU newCpu = (iCPU)c.newInstance();
    // ... rest of initialization
} catch (ClassNotFoundException ex) {
    showError("Failed to load " + name + " simulator: class not found. Check j51.conf.", ex);
} catch (InstantiationException | IllegalAccessException ex) {
    showError("Failed to instantiate " + name + ": " + ex.getMessage() + 
              ". Ensure class implements iCPU and has public no-arg constructor.", ex);
} catch (Exception ex) {
    showError("Failed to initialize " + name + ": " + ex.getMessage(), ex);
}
```

### 3. Enhanced `tryLoadFile(String)` with Format Validation

```java
private void tryLoadFile(String path) throws Exception {
    String ext = getFileExtension(path).toLowerCase();
    
    if (ext.equals("hex") || ext.equals("json")) {
        loadHex(path);
    } else if (ext.equals("bin") || ext.isEmpty()) {
        try { loadBin(path); } catch (IOException ex) { loadRawBin(path); }
    } else if (ext.equals("class")) {
        loadClass(path);
    } else if (ext.equals("jar")) {
        loadJar(path);
    } else if (ext.equals("jll")) {
        loadJll(path);
    } else {
        throw new IllegalArgumentException("Unsupported file format: ." + ext);
    }
}
```

### 4. Specific Load Method Error Wrapping

Each `loadXxx` method catches its specific exceptions and re-throws with context:

```java
private void loadHex(String name) throws Exception {
    try {
        // ... existing logic
    } catch (Exception ex) {
        throw new Exception("Invalid Intel HEX file " + name + ": " + ex.getMessage(), ex);
    }
}
```

### 5. Execution Start Error Handling

Update `actionDebugGo`, `actionDebugTrace`, `actionDebugStep`:

```java
actionDebugGo = new AbstractAction("Go") {
    @Override
    public void actionPerformed(ActionEvent e) {
        if (cpu == null) {
            showError("No simulator loaded. Select a CPU from the CPU menu first.", null);
            return;
        }
        // ... existing logic with showError in catch
    }
};
```

---

## Error Message Format

```
[ERROR] User-friendly message describing what failed and why
       Technical: {ExceptionClass}: {exceptionMessage}
       At: {MethodName}:{LineNumber}
```

Example:
```
[ERROR] Failed to load ARM simulator: class jCPU.arm.CPU not found. Check j51.conf.
       Technical: ClassNotFoundException: jCPU.arm.CPU
       At: GUI.setCpu:196
```

---

## Testing Scenarios

1. **Missing CPU class** - Rename a CPU class in j51.conf, select it
2. **Bad CPU class** - Add non-iCPU class to j51.conf, select it
3. **Missing file** - Try to load non-existent file
4. **Bad HEX file** - Corrupt checksum in .hex file
5. **Bad ELF** - Load non-ELF file as .bin
6. **Unsupported format** - Try to load .txt file
7. **No CPU selected** - Click Go/Step without selecting CPU

---

## Acceptance Criteria

- [ ] All catch blocks in `setCpu(String)` show user-friendly messages
- [ ] `tryLoadFile` validates extension and shows clear error for unsupported formats
- [ ] Each `loadXxx` method wraps exceptions with file context
- [ ] Execution actions check for null CPU and show helpful message
- [ ] Technical details logged to console for debugging
- [ ] No raw stack traces shown in GUI message area (only in console/log)