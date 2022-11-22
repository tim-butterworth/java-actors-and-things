package org.example.aktors.chat.actors.typedactors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Login extends AbstractBehavior<Login.LoginCommand> {

    private final List<String> validUsers;
    private final ActorRef<Lobbey.LobbeyCommand> lobbeyActor;
    private final Set<String> loggedInUsers = new HashSet<>();

    public static Behavior<LoginCommand> create(List<String> validUsers, ActorRef<Lobbey.LobbeyCommand> lobbeyActor) {
        return Behaviors.setup((context) -> new Login(context, validUsers, lobbeyActor));
    }

    private Login(ActorContext<LoginCommand> context, List<String> validUsers, ActorRef<Lobbey.LobbeyCommand> lobbeyActor) {
        super(context);
        this.validUsers = validUsers;
        this.lobbeyActor = lobbeyActor;
    }

    @Override
    public Receive<LoginCommand> createReceive() {
        return newReceiveBuilder()
                .onMessage(Request.class, request -> {
                    String username = request.getUsername();
                    if (validUsers.contains(username) && !loggedInUsers.contains(username)) {
                        loggedInUsers.add(username);
                        request.getReply().tell(new LoginSuccess(lobbeyActor, username));
                    } else {
                        request.getReply().tell(Result.FAILURE);
                    }
                    return Behaviors.same();
                })
                .build();
    }

    public interface LoginResult {}
    public interface LoginCommand {}

    public enum Result implements LoginResult {
        FAILURE
    }

    public static class LoginSuccess implements LoginResult {
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

    public static class Request implements LoginCommand {
        private final String username;
        private final ActorRef<LoginResult> reply;

        public Request(String username, ActorRef<LoginResult> reply) {
            this.username = username;
            this.reply = reply;
        }

        public String getUsername() {
            return username;
        }

        public ActorRef<LoginResult> getReply() {
            return reply;
        }
    }
}
