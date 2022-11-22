package org.example.aktors.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import org.example.aktors.chat.actors.typedactors.TcpBridge;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public class Examples {
    public static void main(String[] args) throws InterruptedException {
        ActorSystem system = ActorSystem.create();

//        ActorRef myActor = system.actorOf(Props.create(MyActor.class), "my-actor");
//        ActorRef supriser = system.actorOf(Props.create(SupriserActor.class, myActor), "supriser");
//
//        myActor.tell(new MyActor.Surprise(), supriser);


        ActorRef tcp = system.actorOf(Props.create(TcpBridge.class), "tcp");

        new Thread(() -> {
            try {
                Thread.sleep(2000);
                try (Socket socket = new Socket("localhost", 8808)) {
                    socket.getOutputStream().write("Hello world".getBytes(StandardCharsets.UTF_8));
                    socket.getOutputStream().flush();

                    Thread reader = new Thread(() -> {
                        try {
                            byte[] buffer = new byte[1024];
                            int read = socket.getInputStream().read(buffer);
                            if (read > 0) {
                                int index = 0;
                                byte[] copy = new byte[read];
                                while (index < read) {
                                    copy[index] = buffer[index];
                                    index++;
                                }
                                System.out.println(new String(copy, StandardCharsets.UTF_8));
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                    reader.start();
                    reader.join();
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();

        //        system.stop(myActor);
//        system.stop(readerActor);
//
//        system.terminate();
    }
}
