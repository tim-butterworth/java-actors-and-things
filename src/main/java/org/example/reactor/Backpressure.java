package org.example.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class Backpressure {
    public static void main(String[] args) {
        Flux.interval(Duration.ofMillis(1))
                .onBackpressureDrop()
//                .onBackpressureBuffer(50000)
                .concatMap(x -> Mono.delay(Duration.ofMillis(1000)).thenReturn(x))
                .doOnNext(System.out::println)
                .blockLast();
    }
}
