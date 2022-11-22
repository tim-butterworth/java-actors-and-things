package org.example.rx.factory;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.parallel.ParallelFlowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class Runner {
    public static void main(String[] args) {
        Observable<String> stringObservable = Observable.fromArray("one", "two", "three");

        stringObservable.subscribe(s -> System.out.println(s));

        stringObservable.subscribe(s -> System.out.println(s));

        // stringObservable is a "cold" observable, nothing happens until there is a subscription
        // what about a "hot" observable


        Subject<String> stringSubject = PublishSubject.create();
        stringSubject.onNext("one");
        stringSubject.onNext("two");

        Disposable complete = stringSubject.subscribe(
                s -> System.out.println(s),
                e -> System.out.println(e),
                () -> System.out.println("COMPLETE")
        );

        stringSubject.onNext("three");
        stringSubject.onNext("four");
        stringSubject.onNext("five");
        stringSubject.onComplete();


        PublishSubject<String> objectPublishSubject = PublishSubject.create();

        Observable<@NonNull List<String>> listObservable = objectPublishSubject
                .buffer(1024)
//                .observeOn(Schedulers.computation());
                .observeOn(Schedulers.from(Executors.newFixedThreadPool(7)));
//                .subscribeOn(Schedulers.from(Executors.newFixedThreadPool(7)));
        IntStream.range(0, 10).forEach(i -> {
            listObservable.subscribe(s -> {
                System.out.println(Thread.currentThread().getName());
                Thread.sleep(1000);
                System.out.println(s);
            }, e -> {
                System.out.println(e);
            });
        });

        IntStream.range(0, 1_000).forEach((i) -> objectPublishSubject.onNext(i + ""));
        objectPublishSubject.onComplete();

        ParallelFlowable<Integer> parallel = Flowable.range(0, 10000).parallel();
        parallel.filter(v -> v % 3 == 0).map(v -> v + v).sequential().filter(v -> v < 100).subscribe(v -> System.out.println(v));


        Observable<String> ironMine = Observable.interval(10, TimeUnit.MILLISECONDS).flatMap(i -> {
            double random = Math.random();
            if (random > .7) {
                return Observable.empty();
            } else if (random > .2) {
                return Observable.just("coal");
            } else {
                return Observable.just("iron");
            }
        });

        ironMine.subscribe(item -> System.out.println(item));
    }
}

