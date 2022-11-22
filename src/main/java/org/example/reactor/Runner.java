package org.example.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Runner {

    private static final String STEEL = "steel";
    private static final String GRAVEL = "gravel";
    private static final String COAL = "coal";
    private static final String IRON = "iron";
    private static final String COCONUT = "coconut";

    public static void main(String[] args) throws InterruptedException {
        Mono<Integer> mono = Mono.just(1);

        mono.subscribe(v -> System.out.println(v));

        ExecutorService executor = Executors.newFixedThreadPool(3);
        ExecutorService executor1 = Executors.newFixedThreadPool(1);

        Sinks.Many<String> gravelSink = Sinks.many().multicast().directAllOrNothing();

        gravelSink.asFlux()
                .onBackpressureError()
                .buffer(12)
                .concatMap(truckLoadOfGravel -> Mono.delay(Duration.ofSeconds(7)).thenReturn(truckLoadOfGravel))
                .subscribe(
                        truckLoadOfGravel -> System.out.println(truckLoadOfGravel),
                        e -> {
                            System.out.println("Outgoing gravel error");
                            System.out.println("Dumping gravel in the neighbors yard....");
                            e.printStackTrace();
                        });

        Flux<String> coalMine = coalMine();
        Flux<String> ironMine = ironMine();

        coalMine.subscribe(s -> {
            if (Objects.equals(GRAVEL, s)) {
                gravelSink.tryEmitNext(s);
            }
        });
        ironMine.subscribe(s -> {
            if (Objects.equals(GRAVEL, s)) {
                gravelSink.tryEmitNext(s);
            }
        });
        Flux<String> unpackedCoconuts = coconutFarm()
                .concatMap(
                        harvest -> Flux.fromIterable(harvest).concatMap(Flux::fromIterable)
                );

        Flux.combineLatest(
                        Runner::id,
                        coalMine.filter(COAL::equals).buffer(3),
                        ironMine.filter(IRON::equals),
                        unpackedCoconuts.buffer(30)
                )
                .concatMap(Runner::convert)
                .subscribe(System.out::println, System.out::println);

        Thread.sleep(30000);
        System.out.println("DONE!!!");
        List<Runnable> runnables = executor1.shutdownNow();
        System.out.println(runnables.size());
        List<Runnable> runnables1 = executor.shutdownNow();
        System.out.println(runnables1.size());
    }

    private static Mono<String> convert(Object[] c) {
        if (c != null && c.length == 3) {
            List<Predicate<Object>> predicates = Arrays.asList(
                    (v) -> Objects.equals(v, IRON),
                    (v) -> Objects.equals(v, Arrays.asList(COAL, COAL, COAL)),
                    (v) -> {
                        if (v instanceof List) {
                            List<?> vList = (List<?>) v;
                            if (vList.size() == 30) {
                                return vList.stream().allMatch((e) -> Objects.equals(e, COCONUT));
                            }
                        }
                        return false;
                    }
            );
            for (Predicate<Object> predicate : predicates) {
                if (!anyMatches(c, predicate)) {
                    return Mono.just("garbage");
                }
            }
            return Mono.delay(Duration.ofSeconds(5)).map(a -> STEEL);
        }
        return Mono.just("garbage");
    }

    private static boolean anyMatches(Object[] c, Predicate<Object> check) {
        for (Object o : c) {
            if (check.test(o)) {
                return true;
            }
        }
        return false;
    }

    private static Flux<String> coalMine() {
        return Flux.interval(Duration.ofMillis(100))
                .flatMap(i -> {
                    double random = Math.random();
                    if (random > .7) {
                        return Mono.empty();
                    } else if (random > .4) {
                        return Mono.just(COAL);
                    } else {
                        return Mono.just(GRAVEL);
                    }
                });
    }

    private static Flux<String> ironMine() {
        return Flux.interval(Duration.ofMillis(100))
                .flatMap(i -> {
                    double random = Math.random();
                    if (random > .7) {
                        return Mono.empty();
                    } else if (random > .5) {
                        return Mono.just(IRON);
                    } else {
                        return Mono.just(GRAVEL);
                    }
                });
    }

    private static Flux<List<List<String>>> coconutFarm() {
        return Flux.interval(Duration.ofSeconds(5))
                .map(i -> {
                    double random = Math.random();
                    if (random > .5) {
                        return largeHarvest();
                    }

                    return smallHarvest();
                });
    }

    private static List<List<String>> coconutHarvest(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> IntStream.range(0, 20)
                        .mapToObj(j -> COCONUT)
                        .collect(Collectors.toList())
                ).collect(Collectors.toList());
    }

    private static List<List<String>> smallHarvest() {
        return coconutHarvest(50);
    }

    private static List<List<String>> largeHarvest() {
        return coconutHarvest(200);
    }

    private static <T> T id(T t) {
        return t;
    }
}
