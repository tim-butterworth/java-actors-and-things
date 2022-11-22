package org.example.aktors.chat.actors.typedactors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.DispatcherSelector;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Arrays;
import java.util.UUID;

public class Chat extends AbstractBehavior<Chat.ChatCommand> {
    public static Behavior<ChatCommand> create() {
        return Behaviors.setup(Chat::new);
    }

    private Chat(ActorContext<ChatCommand> context) {
        super(context);
    }

    private final Receive<ChatCommand> initialized = newReceiveBuilder().build();
    private final Receive<ChatCommand> uninitialized = newReceiveBuilder()
            .onMessage(Initialize.class, (ignored) -> {
                ActorRef<Lobbey.LobbeyCommand> lobbey = getContext().spawn(Lobbey.create(), "lobby");

                getContext().spawn(
                        TcpBridge.create(Arrays.asList()),
                        "listener",
                        DispatcherSelector.fromConfig("blocking-io-dispatcher")
                );
                return initialized;
            })
            .build();

    @Override
    public Receive<ChatCommand> createReceive() {
        return uninitialized;
    }

    public interface ChatCommand {}
    public enum Initialize implements ChatCommand {
        INITIALIZE
    }
    public static class Connect implements ChatCommand {
        private final UUID userId;

        public Connect(UUID userId) {
            this.userId = userId;
        }

        public UUID getUserId() {
            return userId;
        }
    }
}
