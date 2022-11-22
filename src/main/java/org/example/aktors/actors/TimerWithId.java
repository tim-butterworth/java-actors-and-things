package org.example.aktors.actors;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.actor.Props;

import java.util.UUID;

public class TimerWithId extends AbstractActorWithTimers {

    public static Props props() {
        return Props.create(TimerWithId.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Start.class, (start) -> {
                    getTimers().startSingleTimer(UUID.randomUUID(), new Done(start.getOnComplete()), start.getDuration());
                })
                .match(Done.class, (done) -> {
                    getContext().getParent().tell(done.getOnComplete(), ActorRef.noSender());
                })
                .build();
    }

    private static class Done {
        private final Object onComplete;

        public Done(Object onComplete) {
            this.onComplete = onComplete;
        }

        public Object getOnComplete() {
            return onComplete;
        }
    }
    public static class Start<T> {
        private final T onComplete;
        private final java.time.Duration duration;

        public Start(T onComplete, java.time.Duration duration) {
            this.onComplete = onComplete;
            this.duration = duration;
        }

        public T getOnComplete() {
            return onComplete;
        }

        public java.time.Duration getDuration() {
            return duration;
        }
    }
}
