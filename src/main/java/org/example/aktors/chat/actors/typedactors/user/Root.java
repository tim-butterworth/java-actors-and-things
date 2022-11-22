package org.example.aktors.chat.actors.typedactors.user;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.*;
import org.example.aktors.chat.actors.ActorUtil;
import org.example.aktors.chat.actors.classicactors.connection.UserConnection;
import org.example.aktors.chat.actors.typedactors.Lobbey;
import org.example.aktors.chat.actors.typedactors.Login;

import java.util.UUID;

public class Root extends AbstractBehavior<Root.UserCommand> {

    private final ActorRef<Login.LoginCommand> login;

    private Root(ActorContext<UserCommand> context, ActorRef<Login.LoginCommand> login) {
        super(context);
        this.login = login;
    }

    public static Behavior<Root.UserCommand> create(ActorRef<Login.LoginCommand> login) {
        return Behaviors.setup((context) -> new Root(context, login));
    }

    @Override
    public Receive<UserCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(InitializeUser.class, initialize -> initialized(initialize.getTcpActor()))
                .build();
    }

    private Behavior<UserCommand> initialized(akka.actor.ActorRef tcp) {
        return newReceiveBuilder()
                .onMessage(IncomingMessage.class, message -> {
                    String messageString = message.getMessage();
                    System.out.println("User IncomingMessage --> " + messageString);
//                    getContext().getSelf().tell(new OutgoingMessage("RECEIVED ----> [" + messageString + "]"));
                    if (messageString != null && messageString.startsWith("login")) {
                        ActorRef<Login.LoginResult> loginAdapter = getContext().spawn(
                                LoginAdapter.create(getContext().getSelf()),
                                "user-login-adapter" + UUID.randomUUID()
                        );
                        login.tell(
                                new Login.Request(messageString.replace("login", "").trim(), loginAdapter)
                        );
                    }
                    return Behaviors.same();
                })
                .onMessage(OutgoingMessage.class, param -> {
                    System.out.println("Attempting to send message.....");
                    tcp.tell(
                            new UserConnection.SendMessage(param.getMessage()), Adapter.toClassic(getContext().getSelf())
                    );
                    return Behaviors.same();
                })
                .onMessage(LoginSuccess.class, loginSuccess -> {
                    getContext().getSelf().tell(new OutgoingMessage("WELCOME!!!"));
                    ActorRef<LobbeyAdapter.UserLobbeyAdapterCommand> lobbeyAdapter = getContext().spawn(
                            LobbeyAdapter.create(getContext().getSelf(), loginSuccess.getLobbeyActor()),
                            ActorUtil.uniqueId("user-lobbey-adapter")
                    );
                    lobbeyAdapter.tell(new LobbeyAdapter.RegisterWithLobbey(loginSuccess.getUsername()));
                    return loggedInUser(tcp, lobbeyAdapter);
                })
                .build();
    }

    private Behavior<UserCommand> loggedInUser(akka.actor.ActorRef tcp, ActorRef<LobbeyAdapter.UserLobbeyAdapterCommand> lobbeyActor) {
        System.out.println("Switching to logged in user...");
        return newReceiveBuilder()
                .onMessage(IncomingMessage.class, message -> {
                    String messageString = message.getMessage();
                    System.out.println("Logged in User IncomingMessage --> " + messageString);
//                    getContext().getSelf().tell(new OutgoingMessage("RECEIVED ----> [" + messageString + "]"));
                    if (messageString != null && messageString.startsWith("m")) {
                        String lobbeyMessage = messageString.replace("m", "").trim();
                        lobbeyActor.tell(new LobbeyAdapter.PublishMessage(lobbeyMessage));
                    }
                    return Behaviors.same();
                })
                .onMessage(OutgoingMessage.class, param -> {
                    System.out.println("Logged in user Attempting to send message.....");
                    tcp.tell(
                            new UserConnection.SendMessage(param.getMessage()), Adapter.toClassic(getContext().getSelf())
                    );
                    return Behaviors.same();
                })
                .build();
    }

    public interface UserCommand {}
    public static class LoginSuccess implements UserCommand {

        private final ActorRef<Lobbey.LobbeyCommand> lobbeyActor;
        private final String username;

        public LoginSuccess(ActorRef<Lobbey.LobbeyCommand> lobbeyActor, String username) {
            this.lobbeyActor = lobbeyActor;
            this.username = username;
        }

        public ActorRef<Lobbey.LobbeyCommand> getLobbeyActor() {
            return lobbeyActor;
        }

        public String getUsername() {
            return username;
        }
    }

    public static class InitializeUser implements UserCommand {
        private final akka.actor.ActorRef tcpActor;

        public InitializeUser(akka.actor.ActorRef tcpActor) {
            this.tcpActor = tcpActor;
        }

        public akka.actor.ActorRef getTcpActor() {
            return tcpActor;
        }
    }
    public static class IncomingMessage implements UserCommand {
        private final String message;
        private final akka.actor.ActorRef sender;

        public IncomingMessage(String message, akka.actor.ActorRef sender) {
            this.message = message;
            this.sender = sender;
        }

        public String getMessage() {
            return message;
        }

        public akka.actor.ActorRef getSender() {
            return sender;
        }
    }
    public static class OutgoingMessage implements UserCommand {
        private final String message;

        public OutgoingMessage(String message) {
            this.message = message;
        }
        public String getMessage() {
            return message;
        }
    }
}
