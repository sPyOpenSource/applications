# VM Start Failure Error Messages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add user-friendly error messages with technical details for all VM/simulator start failure points in GUI.java: CPU loading, program file loading, and execution start.

**Architecture:** All changes are in a single file (`GUI.java`). Add a `showError` helper method for consistent error formatting (user message + technical log), update catch blocks in `setCpu`, `tryLoadFile`, `performFileOpen`, and the debug actions to use it. Wrap each `loadXxx` method's exceptions with file-level context.

**Tech Stack:** Java (Swing), java.util.logging

## Global Constraints

- No raw stack traces in the GUI message area (only in console/log via Logger)
- User-facing messages in `[ERROR]` prefix format
- Technical details logged with `Level.SEVERE` to the Logger
- All changes confined to `GUI.java`

---

## File Structure

- **Modify:** `src/jCPU/GUI.java` (all changes in a single file)
  - New method: `showError(String, Exception)` - helper for consistent error display
  - Modified method: `setCpu(String)` - enhanced catch blocks with user-friendly messages
  - Modified method: `tryLoadFile(String)` - extension validation and error wrapping
  - Modified method: `performFileOpen()` - use `showError` in catch
  - Modified methods: `loadHex` `(String)`, `loadBin(String)`, `loadRawBin(String)`, `loadClass(String)`, `loadJar(String)`, `loadJll(String)` - wrap exceptions with file context
  - Modified actions: `actionDebugGo`, `actionDebugTrace`, `actionDebugStep` - null CPU check + showError

---

### Task 1: Add `showError` helper method

**Files:**
- Modify: `src/jCPU/GUI.java` (add method after line 293)

**Interfaces:**
- Consumes: existing `messages(String)` method at line 287, existing `Logger` field at line 58
- Produces: `showError(String userMessage, Exception ex)` - void, called by all error handling

- [ ] **Step 1: Add `showError` method to GUI.java**

Insert immediately after the existing `messages(String msg)` method (after line 293):

```java
private void showError(String userMessage, Exception ex) {
    messages("[ERROR] " + userMessage);
    if (ex != null) {
        Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, userMessage, ex);
        ex.printStackTrace(System.out);
    }
}
```

- [ ] **Step 2: Build test**

Run: `ant compile` (or `javac -cp lib/*:. src/jCPU/GUI.java`)  
Expected: Compiles without errors

- [ ] **Step 3: Commit**

```bash
git add src/jCPU/GUI.java
git commit -m "feat: add showError helper method for user-friendly error display"
```

---

### Task 2: Enhanced error handling in `setCpu(String)` (CPU class loading)

**Files:**
- Modify: `src/jCPU/GUI.java:186-226`

**Interfaces:**
- Consumes: `showError(String, Exception)` from Task 1
- Produces: User-friendly error messages for CPU loading failures

- [ ] **Step 1: Replace the catch block in `setCpu(String).process()`**

At lines 217-220, replace the single catch with specific catches. The replacement spans lines 195-220 (the try-catch block):

```java
try{
    Class c = Class.forName(name);
    setProgress("Loading class");
    iCPU newCpu = (iCPU)c.newInstance();
    info.reset.setValue(0);
    GUI.this.cpu = newCpu;
    setProgress("Reset cpu");
    reset();

    for (int i = 0 ; i < panels.size() ; i++){
        J51Panel p = panels.get(i);
        setProgress("Initialize  " + p.getTitle());
        p.setCpu(cpu);
    }

    cpu.addPerformanceListener(GUI.this);

    setProgress("Stop simulation");
    emulation(false);
    messages(cpu.toString());
    setProgress("Garbage collection");
    System.gc();
} catch (ClassNotFoundException ex) {
    showError("Failed to load " + name + " simulator: class not found. Check j51.conf.", ex);
} catch (InstantiationException | IllegalAccessException ex) {
    showError("Failed to instantiate " + name + ": " + ex.getMessage() +
              ". Ensure class implements iCPU and has public no-arg constructor.", ex);
} catch (Exception ex) {
    showError("Failed to initialize " + name + ": " + ex.getMessage(), ex);
}
```

- [ ] **Step 2: Build test** - `cd Simulator && ant compile` or equivalent

- [ ] **Step 3: Commit**

```bash
git add src/jCPU/GUI.java
git commit -m "feat: add user-friendly error messages for CPU class loading failures"
```

---

### Task 3: Add format validation and error wrapping in `tryLoadFile(String)`

**Files:**
- Modify: `src/jCPU/GUI.java:700-717`

**Interfaces:**
- Consumes: `showError(String, Exception)` from Task 1
- Produces: Validates file extension, wraps load method exceptions with file context

- [ ] **Step 1: Replace `tryLoadFile(String)` with enhanced version**

Replace lines 700-717:

```java
private void tryLoadFile(String path) throws Exception {
    String lower = path.toLowerCase();
    if (lower.endsWith(".hex") || lower.endsWith(".json")) {
        loadHex(path);
    } else if (lower.endsWith(".bin") || !path.contains(".")) {
        try {
            loadBin(path);
        } catch (IOException ex) {
            messages("ELF load failed: " + ex.getMessage() + ". Trying raw binary...");
            loadRawBin(path);
        }
    } else if (lower.endsWith(".class")) {
        loadClass(path);
    } else if (lower.endsWith(".jar")) {
        loadJar(path);
    } else if (lower.endsWith(".jll")) {
        loadJll(path);
    } else {
        String ext = path.contains(".") ? path.substring(path.lastIndexOf(".")) : "(no extension)";
        throw new IllegalArgumentException("Unsupported file format: " + ext +
            ". Supported: .hex, .json, .bin, .class, .jar, .jll");
    }
}
```

- [ ] **Step 2: Build test**

- [ ] **Step 3: Commit**

```bash
git add src/jCPU/GUI.java
git commit -m "feat: add format validation and error wrapping in tryLoadFile"
```

---

### Task 4: Wrap load method exceptions with file-level context

**Files:**
- Modify: `src/jCPU/GUI.java` - methods at lines 496-679

**Interfaces:**
- Consumes: `IllegalArgumentException` type from standard library
- Produces: Each loadXxx method throws exceptions with file name in the message

- [ ] **Step 1: Wrap `loadHex` body with context exception**

Replace lines 496-579. Add try-catch around the existing body (keep all existing code inside):

```java
private void loadHex(String name) throws Exception
{
    try {
        BufferedReader rd;
        java.util.List<String> lines = new java.util.ArrayList<>();

        if(name.endsWith(".json")) {
            JSONObject object = parseJSONFile(name);
            Iterator<String> it = object.keys();
            java.util.List array = ((JSONArray)object.get("demo.main")).toList();
            for(Object o : array){
                lines.add(":" + (String)o);
            }
        } else {
            rd = new BufferedReader(new FileReader(name));
            String line;
            while((line = rd.readLine()) != null){
                lines.add(line);
            }
            rd.close();
        }

        int start = 0x10000;
        int end = 0;

        for (String line : lines){
            if (!line.startsWith(":")){
                throw new Exception(name + " is not a valid intel file");
            }

            int lenData = Hex.getByte(line, 1);
            int address = Hex.getWord(line, 3);
            int type    = Hex.getByte(line, 7);

            int chksum = lenData + address / 256 + address + type;

            for (int i = 0 ; i < lenData + 1; i++){
                chksum += Hex.getByte(line, 9 + i * 2);
            }
            chksum &= 0xff;

            if (chksum != 0){
                throw new Exception("Invalid chksum " + Hex.bin2byte(chksum) + " in " + line);
            }

            if (type == 1)
                break;
            if (type == 3)
                continue;

            if (type != 0)
                throw new Exception("Unsupported record type " + type);

            if (address < start)
                start = address;
            if (address + lenData - 1 > end)
                end = address + lenData - 1;
            for (int i = 0 ; i < lenData ; i++){
                cpu.code(address + i, Hex.getByte(line, 9 + i * 2));
            }
        }
        messages(" loaded at " + Hex.bin2word(start) + "-" + Hex.bin2word(end));

        int pos = name.indexOf('.');
        if (pos != -1){
            name = name.substring(0, pos) + ".map";
        }

        try{
            BufferedReader mapRd = new BufferedReader(new FileReader(name));
            String line;
            while ((line = mapRd.readLine()) != null){
                line = line.trim();
                if (line.startsWith("0C:")){
                    int address = Hex.getWord(line, 3);
                    String label = line.substring(7);
                    label = label.trim();
                    cpu.setCodeName(address, label);
                }
            }
            mapRd.close();
        } catch (Exception ex) {
            Logger.getLogger(GUI.class.getName()).log(Level.SEVERE, null, ex);
        }
    } catch (Exception ex) {
        throw new Exception("Failed to load " + name + " as Intel HEX: " + ex.getMessage(), ex);
    }
}
```

Note: The `name` variable is reused for the `.map` file path inside the existing code at line 560. The try-catch wraps the entire block—the `name` parameter is consumed before the try block modifies it, so `.map` loading dotsn't affect the error message.

- [ ] **Step 2: Build test**

- [ ] **Step 3: Commit**

```bash
git add src/jCPU/GUI.java
git commit -m "feat: wrap loadHex exceptions with file context"
```

---

### Task 5: Wrap `loadBin`, `loadRawBin`, `loadClass`, `loadJar`, `loadJll` exceptions

**Files:**
- Modify: `src/jCPU/GUI.java:581-679`

**Interfaces:**
- Consumes: `showerror` helper from Task 1
- Produces: Context-aware exceptions for each loader

- [ ] **Step 1: Update loader methods**

Add try-catch wrapping to each of the 5 methods (lines 581-679). Each follows the same pattern—wrap the existing body in a try-catch that adds file context.

Replace lines 581-597 (`loadBin`):

```java
private void loadBin(String path) throws Exception
{
    try {
        File file = new File(path);
        Elf elf = new Elf(file);
        Memory m = new Memory();
        for (ProgramHeader ph : elf.programHeaders){
            int size = (int) ph.segmentMemorySize;
            if (size <= 0){
                continue;
            }
            Chunk chunk = m.create(ph.virtualAddress, size);
            chunk.data = elf.getSegment(ph);
        }
        for(int i = 0; i < 0x10000; i++){
            cpu.code(i, m.read((int)(i + elf.header.entryPoint)));
        }
    } catch (Exception ex) {
        throw new IOException("Invalid ELF binary " + path + ": " + ex.getMessage(), ex);
    }
}
```

Replace lines 599-607 (`loadRawBin`):

```java
private void loadRawBin(String path) throws Exception
{
    try {
        File file = new File(path);
        FileInputStream fis = new FileInputStream(file);
        byte[] code = fis.readAllBytes();
        for(int i = 0; i < code.length; i++){
            cpu.code(i, code[i + 0x1000 * 0]);
            if(i == 0x10000 - 1) break;
        }
    } catch (Exception ex) {
        throw new IOException("Failed to load raw binary " + path + ": " + ex.getMessage(), ex);
    }
}
```

Replace lines 610-624 (`loadClass`):

```java
private void loadClass(String path) throws Exception
{
    try {
        File file = new File(path);
        InputStream is = new FileInputStream(file);
        ClassData data = new ClassData(new DataInputStream(is));
        ByteCode.cp = data.getConstantPool();
        for(MethodData method:data.getMethodData()){
            if("main".equals(method.getName())){
                byte[] code = method.getCode().getBytecode();
                for(int i = 0; i < code.length; i++){
                    cpu.code(i, code[i]);
                }
            }
        }
    } catch (Exception ex) {
        throw new Exception("Failed to load Java class " + path + ": " + ex.getMessage(), ex);
    }
}
```

Replace lines 626-661 (`loadJar`):

```java
private void loadJar(String path) throws Exception
{
    try {
        JarFile jar = new JarFile(path);
        Enumeration<JarEntry> entries = jar.entries();
        String main = null;
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if(name.equals("META-INF/MANIFEST.MF")){
                try (InputStream is = jar.getInputStream(entry)) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    while(reader.ready()){
                        String line = reader.readLine();
                        if(line.startsWith("Main-Class")){
                            main = line.split(":")[1].strip().replace(".", "/") + ".class";
                        }
                    }
                }
            }
            if (main != null){
                if(name.endsWith(main)){
                    ClassData data = new ClassData(new DataInputStream(jar.getInputStream(entry)));
                    ByteCode.cp = data.getConstantPool();
                    for(MethodData method:data.getMethodData()){
                        if("main".equals(method.getName())){
                            byte[] code = method.getCode().getBytecode();
                            for(int i = 0; i < code.length; i++){
                                cpu.code(i, code[i]);
                            }
                        }
                    }
                    break;
                }
            }
        }
    } catch (Exception ex) {
        throw new Exception("Failed to load JAR " + name + ": " + ex.getMessage(), ex);
    }
}
```

Replace lines 663-679 (`loadJll`):

```java
private void loadJll(String path) throws Exception
{
    try {
        ExtendedDataInputStream stream = new ExtendedDataInputStream(new FileInputStream(path));
        CodeFile file = new CodeFile(null, null);
        java.util.ArrayList<CompiledClass> allClasses = file.read(stream);
        file.size();
        mainloop: for(CompiledClass clazz:allClasses){
            for(CompiledMethod method:clazz.getMethods()){
                byte[] code = method.getCode();
                if(code == null) continue;
                for(int i = 0; i < code.length; i++){
                    cpu.code(i, code[i]);
                }
                break mainloop;
            }
        }
    } catch (Exception ex) {
        throw new Exception("Failed to load JLL file " + path + ": " + ex.getMessage(), ex);
    }
}
```

- [ ] **Step 2: Build test**

- [ ] **Step 3: Commit**

```bash
git add src/jCPU/GUI.java
git commit -m "feat: wrap final loader exceptions with file-level context"
```

---

### Task 6: Update `performFileOpen()` and import actions to use `showError`

**Files:**
- Modify: `src/jCPU/GUI.java:681-697` (`performFileOpen`)
- Modify: `src/jCPU/GUI.java:757-771` (`importBin`)
- Modify: `src/jCPU/GUI.java:780-797` (`importClass`)
- Modify: `src/jCPU/GUI.java:805-819` (`importJar`)
- Modify: `src/jCPU/GUI.java:829-844` (`importJll`)

**Interfaces:**
- Consumes: `showError(String, Exception)` from Task 1
- Produces: Friendly error messages on file load failures

- [ ] **Step 1: Update `performFileOpen()` at lines 681-697**

```java
private void performFileOpen()
{
    try{
        if (fc == null)
        {
            fc = new JFileChooser();
            fc.setCurrentDirectory(new File("."));
        }
        if (fc.showOpenDialog(GUI.this) == JFileChooser.APPROVE_OPTION)
        {
            String path = fc.getSelectedFile().getCanonicalPath();
            tryLoadFile(path);
            updatePanel(true);
        }
    } catch (FileNotFoundException ex) {
        showError("File not found: " + ex.getMessage(), ex);
    } catch (IllegalArgumentException ex) {
        showError(ex.getMessage(), ex);
    } catch (Exception ex) {
        showError("Failed to load file: " + ex.getMessage(), ex);
    }
}
```

- [ ] **Step 2: Update `importBin` action at lines 757-771**

Replace the catch block:

```java
} catch (FileNotFoundException ex) {
    showError("File not found", ex);
} catch (Exception ex) {
    showError("Failed to load ELF binary: " + ex.getMessage(), ex);
}
```

- [ ] **Step 3: Update `importClass` action at line 793-794**

Replace catch:

```java
} catch (FileNotFoundException ex) {
    showError("File not found", ex);
} catch (Exception ex) {
    showError("Failed to load Java class: " + ex.getMessage(), ex);
}
```

- [ ] **Step 4: Update `importJar` at lines 817-818**

Replace catch:

```java
} catch (FileNotFoundException ex) {
    showError("File not found", ex);
} catch (Exception ex) {
    showError("Failed to load JAR: " + ex.getMessage(), ex);
}
```

- [ ] **Step 5: Update `importJll` at lines 841-842**

Replace catch:

```java
} catch (FileNotFoundException ex) {
    showError("File not found", ex);
} catch (Exception ex) {
    showError("Failed to load JLL: " + ex.getMessage(), ex);
}
```

- [ ] **Step 6: Build test**

- [ ] **Step 7: Commit**

```bash
git add src/jCPU/GUI.java
git commit -m "feat: add user-friendly error messages for all file load actions"
```

---

### Task 7: Add null CPU check and execution error handling to debug actions

**Files:**
- Modify: `src/jCPU/GUI.java:1187-1230` (`actionDebugTrace`, `actionDebugGo`, `actionDebugStep`)

**Interfaces:**
- Consumes: `showError(String, Exception)` from Task 1
- Produces: Guard rail null check before execution, user-friendly execution error messages

- [ ] **Step 1: Update `actionDebugTrace` (lines 1187-1201)**

Replace:

```java
actionDebugTrace = new AbstractAction("Step into")
{
    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (cpu == null) {
            showError("No simulator loaded. Select a CPU from the CPU menu first.", null);
            return;
        }
        try
        {
            cpu.step();
            updatePanel(false);
        } catch (Exception ex) {
            showError("Failed to execute step: " + ex.getMessage(), ex);
        }
    }
};
```

- [ ] **Step 2: Update `actionDebugGo` (lines 1203-1230)**

Replace:

```java
actionDebugGo = new AbstractAction("Go")
{
    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (cpu == null) {
            showError("No simulator loaded. Select a CPU from the CPU menu first.", null);
            return;
        }
        thread = new Thread(() -> {
            messages("Simulating ....");
            try
            {
                cpu.go(-1);
            } catch (InterruptedException ex) {
                // Normal stop, not an error
            } catch (Exception ex) {
                GUI.this.showError("Simulation failed: " + ex.getMessage(), ex);
            }


            SwingUtilities.invokeLater(() -> {
                emulation(false);
            });
        });

        emulation(true);

        thread.start();
    }


};
```

Note: `GUI.this.showError(...)` is needed inside the anonymous thread lambda because `showError` is an instance method of `GUI`.

- [ ] **Step 3: Update `actionDebugStep` (lines 1232-1258)**

Replace:

```java
actionDebugStep = new AbstractAction("Step over")
{
    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (cpu == null) {
            showError("No simulator loaded. Select a CPU from the CPU menu first.", null);
            return;
        }
        thread = new Thread(() -> {
            messages("Emulating ....");
            try
            {
                cpu.pass();
            } catch (InterruptedException ex) {
                // Normal stop, not an error
            } catch (Exception ex) {
                GUI.this.showError("Step over failed: " + ex.getMessage(), ex);
            }

            SwingUtilities.invokeLater(() -> {
                emulation(false);
            });
        });

        emulation(true);

        thread.start();
    }


};
```

- [ ] **Step 4: Build test**

- [ ] **Step 5: Commit**

```bash
git add src/jCPU/GUI.java
git commit -m "feat: add null CPU check and user-friendly execution error messages"
```

---

### Task 8: Final verification

- [ ] **Step 1: Full build** - ensure project compiles
- [ ] **Step 2: Manual verification of error message format**
  - Messages appear in the GUI messages area with `[ERROR]` prefix
  - No raw stack traces appear in messages area
  - Technical details appear in console/log output
- [ ] **Step 3: Test each scenario from the spec**
  1. Missing CPU class — Rename a CPU class in `j51.conf`, select it, verify message
  2. Bad CPU class — Add non-iCPU class to `j51.conf`, run, verify message
  3. Missing file — Try to load non-existent file through Load menu
  4. Bad HEX file — Corrupt checksum in .hex file
  5. Bad ELF — Load non-ELF file as .bin through Import menu
  6. Unsupported format — Try to load .txt file
  7. No CPU selected — Click Go/Step before selecting a CPU
- [ ] **Step 4: Commit any final fixes**

```bash
git add src/jCPU/GUI.java
git commit -m "chore: final verification and fixes for VM start error messages"
```