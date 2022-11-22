package org.example.aktors.chat;

import akka.actor.typed.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.typed.javadsl.Adapter;
import org.example.aktors.chat.actors.typedactors.TcpBridge;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Runner {
    public static void main(String[] args) throws IOException {
//        ActorSystem<Chat.ChatCommand> user = ActorSystem.create(Chat.create(), "user");
//        user.tell(new Chat.ChatCommand() {});
//        System.out.println(user.printTree());
//
//
//        ChatServer chatServer = new ChatServer(8800, new ChatServer.ConnectionHandler() {
//            @Override
//            public void register(Socket connection) {
//
//            }
//        });
//
//        chatServer.listen();

        ActorSystem as = ActorSystem.create();
        List<String> validUsers = Arrays.asList("name1", "name2", "name3", "name4");
        ActorRef<TcpBridge.Command> typed = Adapter.spawn(as, TcpBridge.create(validUsers), "Typed");
//        typed.tell(Typed.Pong.INSTANCE);
    }
}
