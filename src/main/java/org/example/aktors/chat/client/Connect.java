package org.example.aktors.chat.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Connect {
    public static void main(String[] args) {
        ThreadSafeWriter threadSafeWriter = new ThreadSafeWriter();
        List<Thread> threadList = Stream.of("name1", "name2", "name3", "name4", "name5")
                .map(name -> new Thread(() -> userThread(name, threadSafeWriter)))
                .collect(Collectors.toList());

        for (Thread thread : threadList) {
            thread.start();
        }

        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void userThread(String userName, ThreadSafeWriter threadSafeWriter) {
        try (Socket socket = new Socket("localhost", 8808)) {
            Thread readStuffThread = new Thread(getReadStuff(socket, userName, threadSafeWriter));
            readStuffThread.start();

            Thread writeStuffThread = new Thread(getWriteStuff(socket, Arrays.asList(
                    "HI",
                    "Message 1",
                    "Message 2",
                    "message 3",
                    "another message",
                    "login " + userName,
                    "m hi there " + UUID.randomUUID(),
                    "m " + UUID.randomUUID(),
                    "m " + UUID.randomUUID(),
                    "m " + UUID.randomUUID()
            )));
            writeStuffThread.start();

            writeStuffThread.join();
            readStuffThread.join();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Runnable getReadStuff(final Socket socket, String userName, ThreadSafeWriter threadSafeWriter) {
        return () -> {
            try {
                byte[] buffer = new byte[1024];
                int messageCount = 0;
                while (messageCount < 10) {
                    int read = socket.getInputStream().read(buffer);
                    byte[] result = new byte[read];
                    IntStream.range(0, read).forEach((index) -> {
                        result[index] = buffer[index];
                    });
                    threadSafeWriter.write("[" + userName + "] " + new String(result));
                    messageCount++;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static Runnable getWriteStuff(final Socket socket, final List<String> messages) {
        return () -> {
            try {
                OutputStream outputStream = socket.getOutputStream();
                for (String message : messages) {
                    Thread.sleep(200);
                    outputStream.write(message.getBytes(StandardCharsets.UTF_8));
                    outputStream.write("\n".getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
    }

    private static class ThreadSafeWriter {
        public synchronized void write(String text) {
            System.out.println(text);
        }
    }
}
