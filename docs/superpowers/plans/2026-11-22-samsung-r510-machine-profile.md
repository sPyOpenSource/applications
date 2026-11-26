# Samsung R510 Machine Profile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the x86 simulator report a Samsung R510–like Core 2 Duo CPU identity through `CPUID` without changing any other machine behaviour.

**Architecture:** Single-file configuration change in `X86Amd64.cpuid()`, driven by a new JUnit test. The guest-facing execution path, RAM size, UEFI firmware, and all peripherals remain untouched.

**Tech Stack:** Java 17, JUnit 4.13.2, NetBeans Ant build (`Simulator/build.xml`).

## Global Constraints

- Keep `X86Core.DEFAULT_MEMORY_SIZE` at `512 * 1024 * 1024`.
- Keep the UEFI boot path and all peripherals unchanged.
- Keep `CPUID` feature bits unchanged: leaf 1 EDX = `(1 << 4) | (1 << 15)` only.
- Do not add a machine-selector UI, config file, or profile class.
- Do not modify `Simulator/build/built-jar.properties`, `assets/dist/assets.jar`, or `ifOS/dist/ifOS.jar`.

---

### Task 1: Report R510 CPU identity through CPUID

**Files:**
- Modify: `Simulator/src/jCPU/x86/X86Amd64.java:184-210`
- Test: `Simulator/test/jCPU/x86/X86Amd64Test.java` (add test after `testCpuidReportsOnlyImplementedFeatures`)

**Interfaces:**
- Consumes: `X86Core.setReg64(int, long)`, `X86Core.getReg64(int)`, register constants `REG_RAX`, `REG_RBX`, `REG_RCX`, `REG_RDX`; existing `X86Amd64Test.coreWith(int...)` helper.
- Produces: `CPUID` leaf 0 vendor `GenuineIntel`; leaf 1 EAX `0x00010676` (stepping 6, model 23, family 6). No new public methods.

- [ ] **Step 1: Write the failing test**

Add this test to `Simulator/test/jCPU/x86/X86Amd64Test.java` immediately after the existing `testCpuidReportsOnlyImplementedFeatures` method:

```java
    @Test
    public void testCpuidReportsR510MachineIdentity() throws Exception {
        // mov eax, 0 ; cpuid
        X86Core core = coreWith(0xB8, 0x00, 0x00, 0x00, 0x00, 0x0F, 0xA2);
        core.step();
        core.step();
        assertEquals(1, core.getReg64(X86Core.REG_RAX));
        assertEquals(0x756E6547L, core.getReg64(X86Core.REG_RBX)); // "Genu"
        assertEquals(0x6C65746EL, core.getReg64(X86Core.REG_RCX)); // "ntel"
        assertEquals(0x49656E69L, core.getReg64(X86Core.REG_RDX)); // "ineI"

        // mov eax, 1 ; cpuid
        X86Core core1 = coreWith(0xB8, 0x01, 0x00, 0x00, 0x00, 0x0F, 0xA2);
        core1.step();
        core1.step();
        // Stepping 6, model 23 (Penryn), family 6: Core 2 Duo class.
        assertEquals(0x10676L, core1.getReg64(X86Core.REG_RAX));
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run from `Simulator/`:

```bash
export PATH=/opt/homebrew/opt/openjdk/bin:$PATH
ant -q test-single -Dtest.includes=jCPU/x86/X86Amd64Test.java
```

Expected: `testCpuidReportsR510MachineIdentity` FAILS with `expected:<1> but was:<...>` / vendor and EAX mismatches. The existing tests in that class still pass.

- [ ] **Step 3: Implement the CPUID identity change**

In `Simulator/src/jCPU/x86/X86Amd64.java`, replace the `cpuid` method's comment and the `case 0:` and `case 1:` blocks with:

```java
    /**
     * CPUID reports a Samsung R510-era Core 2 Duo identity.  Only features
     * this executor implements are advertised: TSC and CMOV in leaf 1, long
     * mode in leaf 0x80000001.  No FPU, SSE or MSRs.
     */
    private void cpuid(int leaf, int subleaf) {
        int a = 0, b = 0, c = 0, d = 0;
        switch (leaf) {
            case 0:                                 // max leaf, vendor "GenuineIntel"
                a = 1;
                b = 0x756E6547;                     // "Genu"
                d = 0x49656E69;                     // "ineI"
                c = 0x6C65746E;                     // "ntel"
                break;
            case 1:
                a = 0x00010676;                     // family 6, model 23, stepping 6
                d = (1 << 4) | (1 << 15);           // TSC, CMOV
                break;
```

Leave the `case 0x80000000`, `case 0x80000001`, `default`, and final `setReg(...)` lines exactly as they are.

- [ ] **Step 4: Run the targeted test to verify it passes**

Run from `Simulator/`:

```bash
export PATH=/opt/homebrew/opt/openjdk/bin:$PATH
ant -q test-single -Dtest.includes=jCPU/x86/X86Amd64Test.java
```

Expected: all tests in `X86Amd64Test` PASS, including `testCpuidReportsR510MachineIdentity` and the unchanged feature-bit assertions.

- [ ] **Step 5: Run the full x86 suite**

Run from `Simulator/`:

```bash
export PATH=/opt/homebrew/opt/openjdk/bin:$PATH
java -Xmx2g -cp "build/classes:build/test/classes:/Users/xuyi/Source/Java/nasm/dist/nasm.jar:/Applications/Apache NetBeans.app/Contents/Resources/netbeans/platform/modules/ext/junit-4.13.2.jar:/Applications/Apache NetBeans.app/Contents/Resources/netbeans/platform/modules/ext/hamcrest-core-1.3.jar" org.junit.runner.JUnitCore $(cd build/test/classes && find jCPU/x86 -name '*Test.class' ! -name '*$*' | sed 's#/#.#g;s#\.class$##' | sort)
```

Expected: `Tests run: 79, Failures: 1` (78 baseline plus the new test). The only failure must be the pre-existing stale `jCPU.x86.X86CoreTest.testRegisterConstantsAndAccess` (`expected:<8> but was:<100>`).

- [ ] **Step 6: Verify jxcore still boots**

Run from `Simulator/`:

```bash
export PATH=/opt/homebrew/opt/openjdk/bin:$PATH
java -Xmx4g -cp "build/classes:build/test/classes" jCPU.x86.boot.UefiBootRunner /private/var/folders/ws/9cwgq9ps1f9cdq1f4zfby4z40000gn/T/opencode/iso6-mbr.img 8000000000 </dev/null > /private/var/folders/ws/9cwgq9ps1f9cdq1f4zfby4z40000gn/T/opencode/iso6-r510-profile.log 2>&1
grep -a -n 'Terminate DomainZero initial thread.\|panic\|PANIC' /private/var/folders/ws/9cwgq9ps1f9cdq1f4zfby4z40000gn/T/opencode/iso6-r510-profile.log
```

Expected: output contains `Terminate DomainZero initial thread.` and no `panic`/`PANIC` lines. A step-limit stop (`[runner] stopped after ... steps`) is acceptable.

- [ ] **Step 7: Commit**

```bash
git add Simulator/src/jCPU/x86/X86Amd64.java Simulator/test/jCPU/x86/X86Amd64Test.java
git commit -m "x86: report Samsung R510 Core 2 Duo identity via CPUID"
```
