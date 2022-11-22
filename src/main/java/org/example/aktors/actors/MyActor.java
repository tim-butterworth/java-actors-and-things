package org.example.aktors.actors;

import akka.actor.AbstractActor;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public class MyActor extends AbstractActor {

    private AttentionState attentionState = AttentionState.CALM;
    private UUID deEscalationId;
    private static final String TIMER_CHILD_NAME = "timer";

    public MyActor() {
        getContext().actorOf(TimerWithId.props(), TIMER_CHILD_NAME);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Surprise.class, (surprise) -> {
                    if (attentionState == AttentionState.CALM) {
                        System.out.println("GOT SCARED");
                        getSender().tell("Ahhh [GOT SCARED]", getSelf());

                        attentionState = AttentionState.SCARED;

                        deEscalationId = UUID.randomUUID();
                        ExceededAttentionSpan exceededAttentionSpan = new ExceededAttentionSpan(deEscalationId);
                        getContext().findChild(TIMER_CHILD_NAME).ifPresent((timer) -> {
                            timer.tell(new TimerWithId.Start<>(exceededAttentionSpan, Duration.of(2, ChronoUnit.SECONDS)), getSelf());
                        });
                    } else if (attentionState == AttentionState.WARY || attentionState == AttentionState.SCARED || attentionState == AttentionState.SUSPICIOUS) {
                        System.out.println("Did not get scared....");
                        getSender().tell("SUSPICIOUS....", getSelf());

                        attentionState = AttentionState.SCARED;

                        deEscalationId = UUID.randomUUID();
                        ExceededAttentionSpan exceededAttentionSpan = new ExceededAttentionSpan(deEscalationId);
                        getContext().findChild(TIMER_CHILD_NAME).ifPresent((timer) -> {
                            timer.tell(new TimerWithId.Start<>(exceededAttentionSpan, Duration.of(5, ChronoUnit.SECONDS)), getSelf());
                        });
                    }
                })
                .match(ExceededAttentionSpan.class, (exceededAttentionSpan) -> {
                    if (Objects.equals(exceededAttentionSpan.deEscalationId, deEscalationId)) {
                        attentionState = deEscalateStat(attentionState);
                        System.out.println("CALMEND A BIT --> " + attentionState.name());
                        if (attentionState == AttentionState.WARY || attentionState == AttentionState.SCARED) {
                            deEscalationId = UUID.randomUUID();
                            getContext().findChild(TIMER_CHILD_NAME).ifPresent((timerActor) -> {
                                ExceededAttentionSpan onComplete = new ExceededAttentionSpan(deEscalationId);
                                Duration delay = Duration.of(1, ChronoUnit.SECONDS);
                                TimerWithId.Start<ExceededAttentionSpan> start = new TimerWithId.Start<>(onComplete, delay);
                                timerActor.tell(start, timerActor);
                            });
                        }
                    }
                })
                .match(Object.class, (a) -> {
                    System.out.println(a + " Not supported");
                })
                .build();
    }

    private AttentionState deEscalateStat(AttentionState attentionState) {
        switch (attentionState) {
            case CALM:
            case WARY:
                return AttentionState.CALM;
            case SCARED:
            case SUSPICIOUS:
                return AttentionState.WARY;
        }

        throw new RuntimeException();
    }

    public static class Surprise {
    }

    public static class ExceededAttentionSpan {

        private final UUID deEscalationId;

        public ExceededAttentionSpan(UUID deEscalationId) {
            this.deEscalationId = deEscalationId;
        }
    }

    private static enum AttentionState {
        SCARED, SUSPICIOUS, WARY, CALM
    }
}
