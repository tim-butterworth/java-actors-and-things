package org.example.aktors.chat.actors.typedactors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.Terminated;
import akka.actor.typed.javadsl.*;
import akka.io.Tcp;
import akka.io.TcpMessage;
import org.example.aktors.chat.actors.ActorUtil;
import org.example.aktors.chat.actors.classicactors.ClassicTcp;
import org.example.aktors.chat.actors.classicactors.connection.UserConnection;
import org.example.aktors.chat.actors.typedactors.user.Root;

import java.util.List;
import java.util.UUID;

public class TcpBridge extends AbstractBehavior<TcpBridge.Command> {

    public interface Command {
    }

    public static class FailedToBind implements Command {
        private final Tcp.CommandFailed failed;

        public FailedToBind(Tcp.CommandFailed failed) {
            this.failed = failed;
        }

        public Tcp.CommandFailed getFailed() {
            return failed;
        }
    }

    public static class Received implements Command {
        private final Tcp.Received tcpReceived;

        public Received(Tcp.Received tcpReceived) {
            this.tcpReceived = tcpReceived;
        }

        public Tcp.Received getTcpReceived() {
            return tcpReceived;
        }
    }

    public static class ConnectionCreated implements Command {
        private final Tcp.Connected tcpConnected;
        private final akka.actor.ActorRef sender;

        public ConnectionCreated(Tcp.Connected tcpConnected, akka.actor.ActorRef sender) {
            this.tcpConnected = tcpConnected;
            this.sender = sender;
        }

        public Tcp.Connected getTcpConnected() {
            return tcpConnected;
        }

        public akka.actor.ActorRef getSender() {
            return sender;
        }
    }

    private final akka.actor.ActorRef classicTcp;
    private final ActorRef<Login.LoginCommand> login;

    private TcpBridge(
            ActorContext<Command> context,
            akka.actor.ActorRef classicTcp,
            ActorRef<Login.LoginCommand> login
    ) {
        super(context);
        this.classicTcp = classicTcp;
        this.login = login;
    }

    public static Behavior<Command> create(List<String> validUsers) {
        return akka.actor.typed.javadsl.Behaviors.setup(
                context -> {
                    ActorRef<Lobbey.LobbeyCommand> lobbeyActor = context.spawn(Lobbey.create(), "lobbey");
                    ActorRef<Login.LoginCommand> loginActor = context.spawn(Login.create(validUsers, lobbeyActor), "login");
                    akka.actor.ActorRef classicTcp = Adapter.actorOf(context, ClassicTcp.props(context.getSelf()), "classic-tcp");
                    Adapter.watch(context, classicTcp);

                    return new TcpBridge(context, classicTcp, loginActor);
                });
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(ConnectionCreated.class, connected -> {
                    akka.actor.ActorRef sender = connected.getSender();

                    System.out.println(sender);
                    System.out.println("Spawning a [ConnectionWrapper]");

                    ActorRef<Root.UserCommand> user = getContext().spawn(Root.create(login), "user" + UUID.randomUUID());

                    akka.actor.ActorRef actorRef = Adapter.actorOf(
                            getContext(),
                            UserConnection.props(user),
                            ActorUtil.uniqueId("user-connection")
                    );
                    actorRef.tell(new UserConnection.InitializeUserConnection(sender), akka.actor.ActorRef.noSender());
                    connected.getSender().tell(TcpMessage.register(actorRef), actorRef);
                    return Behaviors.same();
                })
                .onMessage(Received.class, received -> {
                    System.out.println("Typed --> Received");
                    return Behaviors.same();
                })
                .onMessage(FailedToBind.class, failedToBind -> {
                    System.out.println(failedToBind);
                    System.out.println(failedToBind.getFailed());
                    Adapter.stop(getContext(), classicTcp);
                    return Behaviors.same();
                })
                .onSignal(Terminated.class, sig -> Behaviors.stopped())
                .build();
    }
}