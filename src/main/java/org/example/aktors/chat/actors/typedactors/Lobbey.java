package org.example.aktors.chat.actors.typedactors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Lobbey extends AbstractBehavior<Lobbey.LobbeyCommand> {

    private static final Map<UUID, Register> registeredUsers = new HashMap<>();

    public static Behavior<LobbeyCommand> create() {
        return Behaviors.setup(Lobbey::new);
    }

    private Lobbey(ActorContext<LobbeyCommand> context) {
        super(context);
    }

    @Override
    public Receive<LobbeyCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(Register.class, register -> {
                    ActorRef<LobbeyResponse> actor = register.getActor();
                    UUID uuid = UUID.randomUUID();
                    registeredUsers.put(uuid,  register);
                    actor.tell(new LobbeyRegistered(uuid));
                    return Behaviors.same();
                })
                .onMessage(ToLobbeyMessage.class, toLobbeyMessage -> {
                    UUID senderId = toLobbeyMessage.getFrom();
                    Register register = registeredUsers.get(senderId);
                    if (register == null) {
                        System.out.println("Not a registered user... maybe setup so a reply can be sent");
                        return Behaviors.same();
                    }

                    String senderName = register.getName();
                    for (Map.Entry<UUID, Register> entry : registeredUsers.entrySet()) {
                        if (!Objects.equals(entry.getKey(), senderId)) {
                            entry.getValue().getActor().tell(new FromLobbeyMessage(toLobbeyMessage.getText(), senderName));
                        }
                    }
                    return Behaviors.same();
                })
                .build();
    }

    public interface LobbeyCommand {}
    public static class Register implements LobbeyCommand {
        private final ActorRef<LobbeyResponse> actor;
        private final String name;

        public Register(ActorRef<LobbeyResponse> actor, String name) {
            this.actor = actor;
            this.name = name;
        }

        public ActorRef<LobbeyResponse> getActor() {
            return actor;
        }

        public String getName() {
            return name;
        }
    }

    public static class ToLobbeyMessage implements LobbeyCommand {
        private final UUID from;
        private final String text;


        public ToLobbeyMessage(UUID from, String text) {
            this.from = from;
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public UUID getFrom() {
            return from;
        }
    }

    public interface LobbeyResponse {}

    public static class LobbeyRegistered implements LobbeyResponse {

        private final UUID uuid;

        public LobbeyRegistered(UUID uuid) {
            this.uuid = uuid;
        }

        public UUID getUuid() {
            return uuid;
        }
    }

    public static class FromLobbeyMessage implements LobbeyResponse {
        private final String message;
        private final String from;

        public FromLobbeyMessage(String message, String from) {
            this.message = message;
            this.from = from;
        }

        public String getFrom() {
            return from;
        }

        public String getMessage() {
            return message;
        }
    }
}
