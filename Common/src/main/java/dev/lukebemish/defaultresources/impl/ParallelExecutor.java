/*
 * Copyright (C) 2024 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl;

import net.minecraft.util.Unit;

import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class ParallelExecutor {
    private ParallelExecutor() {}

    private static final AtomicInteger POOL_THREAD_COUNTER = new AtomicInteger(0);
    private static final ForkJoinPool POOL;

    static {
        final ClassLoader classLoader = ParallelExecutor.class.getClassLoader();
        POOL = new ForkJoinPool(
                Math.max(4, Runtime.getRuntime().availableProcessors() - 4),
                forkJoinPool -> {
                    final ForkJoinWorkerThread thread = new ForkJoinWorkerThread(forkJoinPool) {};
                    thread.setContextClassLoader(classLoader);
                    thread.setName(String.format("DefaultResources parallel executor: %s", POOL_THREAD_COUNTER.incrementAndGet()));
                    return thread;
                }, null, true
        );
    }

    public static <T> void execute(Stream<T> stream, Consumer<T> task) {
        POOL.invoke(new RunnableExecuteAction(() -> stream.parallel().forEach(task)));
    }

    private static final class RunnableExecuteAction extends ForkJoinTask<Unit> {
        final Runnable runnable;

        private RunnableExecuteAction(Runnable runnable) {
            this.runnable = Objects.requireNonNull(runnable);
        }

        public Unit getRawResult() {
            return Unit.INSTANCE;
        }

        public void setRawResult(Unit v) {
        }

        public boolean exec() {
            this.runnable.run();
            return true;
        }
    }
}
