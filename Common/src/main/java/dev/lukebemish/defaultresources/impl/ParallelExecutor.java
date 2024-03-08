/*
 * Copyright (C) 2024 Luke Bemish, and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

package dev.lukebemish.defaultresources.impl;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class ParallelExecutor {
    private ParallelExecutor() {}

    public static <T> void execute(Stream<T> stream, Consumer<T> task) {
        var threads = Math.max(4, Runtime.getRuntime().availableProcessors() - 4);
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        List<Callable<Void>> tasks = stream.map(t -> (Callable<Void>) () -> {
            task.accept(t);
            return null;
        }).toList();
        try {
            for (var f : exec.invokeAll(tasks)) {
                f.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            exec.shutdown();
        }
    }
}
