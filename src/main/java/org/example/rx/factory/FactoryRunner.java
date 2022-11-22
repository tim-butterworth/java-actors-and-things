package org.example.rx.factory;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.observers.DisposableObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subscribers.DisposableSubscriber;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FactoryRunner {
    public static void main(String[] args) {
        Observable<String> ironMine = getIronMine();
        Observable<String> copperMine = getCopperMine();

        ExecutorService executor = Executors.newFixedThreadPool(1);
        ironMine.observeOn(Schedulers.from(executor), true, 10).subscribe((s) -> {
            Thread.sleep(1000);
            System.out.println(s);
        });

        try {
            Thread.sleep(10000);
            executor.shutdown();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Observable<String> getIronMine() {
        return Observable.interval(10, TimeUnit.MILLISECONDS)
                .flatMap(i -> {
                    double random = Math.random();
                    System.out.println("PRODUCING");
                    if (random > .7) {
                        return Observable.empty();
                    } else if (random > .2) {
                        return Observable.just("coal");
                    } else {
                        return Observable.just("iron-ore");
                    }
                });
    }

    private static Observable<String> getCopperMine() {
        return Observable.interval(10, TimeUnit.MILLISECONDS)
                .flatMap(i -> {
                    double random = Math.random();
                    if (random > .7) {
                        return Observable.empty();
                    } else if (random > .2) {
                        return Observable.just("coal");
                    } else if (random > .1) {
                        return Observable.just("copper-ore");
                    } else {
                        return Observable.just("fossils");
                    }
                });
    }
}
