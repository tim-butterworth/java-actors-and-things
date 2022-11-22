package org.example.aktors.chat.actors.classicactors.connection;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.actor.typed.ActorRef;
import akka.io.Tcp;
import akka.io.TcpMessage;
import akka.util.ByteString;
import org.example.aktors.chat.actors.typedactors.user.Root;

import static org.example.aktors.chat.actors.ActorUtil.logging;

public class UserConnection extends AbstractActor {
    private final ActorRef<Root.UserCommand> user;

    public static Props props(ActorRef<Root.UserCommand> user) {
        return akka.actor.Props.create(UserConnection.class, user);
    }

    private UserConnection(ActorRef<Root.UserCommand> user) {
        this.user = user;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(InitializeUserConnection.class, logging(initializeUserConnection -> {
                    user.tell(new Root.InitializeUser(getSelf()));
                    getContext().become(initialized(initializeUserConnection.getTcpActor()));
                }))
                .matchAny((m) -> System.out.println("Unhandled message ---> " + m.getClass()))
                .build();
    }

    private Receive initialized(akka.actor.ActorRef tcpActor) {
        return receiveBuilder()
                .match(Tcp.Received.class, logging(msg -> {
                    final ByteString data = msg.data();
                    user.tell(new Root.IncomingMessage(data.utf8String(), getSelf()));
                }))
                .match(Tcp.ConnectionClosed.class, logging(msg -> {
                    getContext().stop(getSelf());
                }))
                .match(SendMessage.class, logging(out -> {
                    tcpActor.tell(TcpMessage.write(ByteString.fromString("<{ " + out.getMessage() + " }>")), getSelf());
                }))
                .matchAny((m) -> System.out.println("Unhandled message ---> " + m.getClass()))
                .build();
    }
    public static class SendMessage {
        private final String message;

        public SendMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class InitializeUserConnection {
        private final akka.actor.ActorRef tcpActor;

        public InitializeUserConnection(akka.actor.ActorRef tcpActor) {
            this.tcpActor = tcpActor;
        }

        public akka.actor.ActorRef getTcpActor() {
            return tcpActor;
        }
    }
}
