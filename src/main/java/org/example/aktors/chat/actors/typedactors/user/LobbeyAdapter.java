package org.example.aktors.chat.actors.typedactors.user;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.aktors.chat.actors.ActorUtil;
import org.example.aktors.chat.actors.typedactors.Lobbey;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public class LobbeyAdapter extends AbstractBehavior<LobbeyAdapter.UserLobbeyAdapterCommand> {

    private final ActorRef<Root.UserCommand> user;
    private final ActorRef<Lobbey.LobbeyCommand> lobbeyActor;
    private final Queue<PublishMessage> pendingMessages = new LinkedList<>();

    public static Behavior<UserLobbeyAdapterCommand> create(ActorRef<Root.UserCommand> user, ActorRef<Lobbey.LobbeyCommand> lobbeyActor) {
        return Behaviors.setup(context -> new LobbeyAdapter(context, user, lobbeyActor));
    }

    private LobbeyAdapter(ActorContext<UserLobbeyAdapterCommand> context, ActorRef<Root.UserCommand> user, ActorRef<Lobbey.LobbeyCommand> lobbeyActor) {
        super(context);
        this.user = user;
        this.lobbeyActor = lobbeyActor;
    }

    @Override
    public Receive<UserLobbeyAdapterCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(RegisterWithLobbey.class, register -> {
                    ActorRef<Lobbey.LobbeyResponse> lobbeyResponse = getContext().spawn(
                            LobbeyReceiverAdapter.create(getContext().getSelf()),
                            ActorUtil.uniqueId("lobby-user-receiver")
                    );
                    lobbeyActor.tell(new Lobbey.Register(lobbeyResponse, register.getUsername()));

                    return pendingRegister();
                })
                .onMessage(PublishMessage.class, m -> {
                    pendingMessages.add(m);
                    return Behaviors.same();
                })
                .build();
    }

    public Behavior<UserLobbeyAdapterCommand> pendingRegister() {
        return newReceiveBuilder()
                .onMessage(PublishMessage.class, m -> {
                    pendingMessages.add(m);
                    return Behaviors.same();
                })
                .onMessage(RegisterSuccess.class, registerSuccess -> {
                    UUID lobbeyId = registerSuccess.getLobbeyId();
                    while (pendingMessages.size() > 0) {
                        lobbeyActor.tell(new Lobbey.ToLobbeyMessage(lobbeyId, pendingMessages.remove().getMessage()));
                    }
                    return registered(lobbeyId);
                })
                .build();
    }

    public Behavior<UserLobbeyAdapterCommand> registered(UUID lobbeyId) {
        return newReceiveBuilder()
                .onMessage(PublishMessage.class, message -> {
                    lobbeyActor.tell(new Lobbey.ToLobbeyMessage(lobbeyId, message.getMessage()));
                    return Behaviors.same();
                })
                .onMessage(ReceiveMessage.class, toUserMessage -> {
                    user.tell(new Root.OutgoingMessage(toUserMessage.getName() + " ---> " + toUserMessage.getMessage()));
                    return Behaviors.same();
                })
                .build();
    }

    public interface UserLobbeyAdapterCommand {}
    public static class RegisterWithLobbey implements UserLobbeyAdapterCommand {
        private final String username;

        public RegisterWithLobbey(String username) {
            this.username = username;
        }

        public String getUsername() {
            return username;
        }
    }

    public static class RegisterSuccess implements UserLobbeyAdapterCommand {
        private final UUID lobbeyId;

        public RegisterSuccess(UUID lobbeyId) {
            this.lobbeyId = lobbeyId;
        }

        public UUID getLobbeyId() {
            return lobbeyId;
        }
    }
    public static class PublishMessage implements UserLobbeyAdapterCommand {
        private final String message;

        public PublishMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class ReceiveMessage implements UserLobbeyAdapterCommand {
        private final String name;
        private final String message;

        public ReceiveMessage(String name, String message) {
            this.name = name;
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public String getName() {
            return name;
        }
    }

    public static class LobbeyReceiverAdapter extends AbstractBehavior<Lobbey.LobbeyResponse> {

        private final ActorRef<UserLobbeyAdapterCommand> toUser;

        public static Behavior<Lobbey.LobbeyResponse> create(ActorRef<LobbeyAdapter.UserLobbeyAdapterCommand> toUser) {
            return Behaviors.setup(context -> new LobbeyReceiverAdapter(context, toUser));
        }

        private LobbeyReceiverAdapter(ActorContext<Lobbey.LobbeyResponse> context, ActorRef<UserLobbeyAdapterCommand> toUser) {
            super(context);
            this.toUser = toUser;
        }

        @Override
        public Receive<Lobbey.LobbeyResponse> createReceive() {
            return newReceiveBuilder()
                    .onMessage(Lobbey.LobbeyRegistered.class, registeredWithLobbey -> {
                        toUser.tell(new RegisterSuccess(registeredWithLobbey.getUuid()));
                        return Behaviors.same();
                    })
                    .onMessage(Lobbey.FromLobbeyMessage.class, messageToUser -> {
                        toUser.tell(new ReceiveMessage(messageToUser.getFrom(), messageToUser.getMessage()));
                        return Behaviors.same();
                    })
                    .build();
        }
    }
}
