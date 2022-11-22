package org.example.aktors.chat.actors.classicactors;

import akka.actor.AbstractActor;
import akka.actor.typed.ActorRef;
import akka.io.Tcp;
import akka.io.TcpMessage;
import org.example.aktors.chat.actors.typedactors.TcpBridge;

import java.net.InetSocketAddress;

public class ClassicTcp extends AbstractActor {
    private final ActorRef<TcpBridge.Command> bridge;

    public static akka.actor.Props props(ActorRef<TcpBridge.Command> forward) {
        return akka.actor.Props.create(ClassicTcp.class, forward);
    }

    private ClassicTcp(ActorRef<TcpBridge.Command> bridge) {
        this.bridge = bridge;
    }

    @Override
    public void preStart() {
        akka.actor.ActorRef tcp = Tcp.get(getContext().getSystem()).manager();
        tcp.tell(TcpMessage.bind(getSelf(), new InetSocketAddress("localhost", 8808), 100), getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Tcp.Bound.class, (msg) -> {
                    System.out.println("Successfully bound!!!!");
                    System.out.println(msg);
                })
                .match(Tcp.CommandFailed.class, (failed) -> {
                    bridge.tell(new TcpBridge.FailedToBind(failed));
                })
                .match(Tcp.Connected.class, (connect) -> {
                    System.out.println("Connected");
                    System.out.println(connect);
                    bridge.tell(new TcpBridge.ConnectionCreated(connect, getSender()));
                })
                .matchAny((cmd) -> System.out.println("Unhandled --> " + cmd.getClass()))
                .build();
    }
}
