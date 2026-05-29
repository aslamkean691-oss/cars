package com.example.cars.Hellper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * خيط خلفي مشترك لتحميل البيانات من Appwrite والشبكة — لا يُستدعى من الـ main thread.
 */
public final class LoadExecutor {

    private static final int POOL_SIZE = 4;

    private static final ThreadFactory FACTORY = new ThreadFactory() {
        private final AtomicInteger seq = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "autosoket-load-" + seq.getAndIncrement());
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        }
    };

    private static final Executor IO = Executors.newFixedThreadPool(POOL_SIZE, FACTORY);

    private LoadExecutor() {}

    public static Executor io() {
        return IO;
    }
}
