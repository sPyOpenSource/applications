package name.bizna.jarmtest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import name.bizna.jarm.CPU;
import static org.junit.Assert.assertEquals;

public class JarmTest {
	
	private static void recursivelyBuildTestList(List<TestDirectory> tests, File cwd, String canonPath) {
		assert(cwd.isDirectory());
		if(TestDirectory.isValidTestDir(cwd)) tests.add(new TestDirectory(cwd, canonPath));
		for(File child : cwd.listFiles()) {
			if(child.isDirectory())
				recursivelyBuildTestList(tests, child, canonPath == null ? child.getName() : (canonPath + File.separator + child.getName()));
		}
	}
	
        @Test
	public void testJarm() {
		File baseDirectory = new File(System.getProperty("user.dir"));
		int threadCount = Runtime.getRuntime().availableProcessors();
		List<TestDirectory> tests = new ArrayList<>();
		recursivelyBuildTestList(tests, baseDirectory, null);
		int passed = 0, failed = 0;
		List<String> failures;
		if(threadCount == 1) {
			failures = new ArrayList<>();
			// Don't bother actually making a separate thread
			CPU cpu = new CPU();
			cpu.mapCoprocessor(7, new CP7(cpu));
			for(TestDirectory test : tests) {
				if(test.runTest(cpu, failures)) ++passed;
				else ++failed;
			}
		} else {
			TestThread threads[] = new TestThread[threadCount];
			AtomicInteger semaphore = new AtomicInteger(0);
			for(int n = 0; n < threadCount; ++n)
				threads[n] = new TestThread("TestThread-" + n, tests, semaphore);
			for(int n = 0; n < threadCount; ++n)
				threads[n].start();
			int totalFailCount = 0;
			for(int n = 0; n < threadCount; ++n) {
				while(true) {
					try {
						threads[n].join();
						break;
					} catch(InterruptedException e) {
						// try again
					}
				}
				totalFailCount += threads[n].getFailures().size();
				passed += threads[n].getNumPassed();
				failed += threads[n].getNumFailed();
			}
			failures = new ArrayList<>(totalFailCount);
			for(int n = 0; n < threadCount; ++n) {
				failures.addAll(threads[n].getFailures());
			}
		}
		System.out.println(String.format("%d dir%s passed, %d dir%s failed.", passed, passed == 1 ? "" : "s", failed, failed == 1 ? "" : "s"));
		assertEquals(failed, 0);
	}

}
