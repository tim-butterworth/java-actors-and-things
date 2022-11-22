package org.example.rx;

import java.util.concurrent.*;

public class Main {
    public static void main(String[] args) {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        Future<String> result = scheduledExecutorService.submit(() -> {
            Thread.sleep(5000);
            return "Hi there";
        });

        new Thread(() -> {
            while(!result.isDone()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("Still waiting...");
            }

            try {
                System.out.println(result.get());
                scheduledExecutorService.shutdown();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }).start();

        CountDownLatch countDownLatch = new CountDownLatch(1);

        CompletableFuture<String> completableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(6000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            return "We did it!";
        });
        completableFuture
                .thenApply(String::toUpperCase)
                .thenCombineAsync(CompletableFuture.supplyAsync(() -> "other one"), (a, b) -> {
                    return a + "  " + b;
                })
                .thenAccept((s) -> {
                    System.out.println(s);
                    countDownLatch.countDown();
                });

        System.out.println("Doing some other stuff....");
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}