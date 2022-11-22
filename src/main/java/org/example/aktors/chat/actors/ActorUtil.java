package org.example.aktors.chat.actors;

import akka.japi.pf.FI;

import java.util.concurrent.atomic.AtomicInteger;

public class ActorUtil {

    private static AtomicInteger id = new AtomicInteger(0);
    public static <T> FI.UnitApply<T> logging(FI.UnitApply<T> effect) {
        return (T t) -> {
            System.out.println("     LOGGING ----> " + t.getClass());
            System.out.println("     LOGGING ----> " + effect);
            effect.apply(t);
        };
    }

    public static String uniqueId(String base) {
        int uniqueSuffix = id.getAndAdd(1);
        return base + "_" + uniqueSuffix;
    }
}
