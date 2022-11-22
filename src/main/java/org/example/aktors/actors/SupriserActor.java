package org.example.aktors.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class SupriserActor extends AbstractActor {

    private final String TIMER = "timer";
    int rate = 1000;
    private final ActorRef targetActor;

    public SupriserActor(ActorRef targetActor) {
        this.targetActor = targetActor;
        getContext().actorOf(TimerWithId.props(), TIMER);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, (s) -> {
                    System.out.println(s);
                    if (s.startsWith("Ahh")) {
                        System.out.println("Successful scared!");
                    }
                    if (s.startsWith("SUSPICIOUS")) {
                        rate = rate + 1000;
                        System.out.println("Unsuccessful scare... wait longer " + rate);
                    }
                    getContext().findChild(TIMER).ifPresent((t) ->
                            t.tell(new TimerWithId.Start<>(new TryAgain(), Duration.of(rate, ChronoUnit.MILLIS)), getSelf())
                    );
                })
                .match(TryAgain.class, (ta) -> {
                    targetActor.tell(new MyActor.Surprise(), getSelf());
                })
                .build();
    }

    private static class TryAgain {}
}
