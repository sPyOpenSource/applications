package test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;

public final class FxTestSupport {
    private static boolean started;

    private FxTestSupport() {
    }

    public static synchronized void ensureStarted() {
        if (started) {
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyRunning) {
            latch.countDown();
        }

        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("JavaFX did not start within 10 seconds");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while starting JavaFX", e);
        }

        started = true;
    }

    public static synchronized void shutdown() {
        if (!started) {
            return;
        }
        Platform.exit();
        started = false;
    }
}
