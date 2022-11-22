package org.example.aktors.chat.actors.typedactors.user;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.aktors.chat.actors.typedactors.Login;

public class LoginAdapter extends AbstractBehavior<Login.LoginResult> {

    private final ActorRef<Root.UserCommand> user;

    private LoginAdapter(ActorContext<Login.LoginResult> context, ActorRef<Root.UserCommand> user) {
        super(context);
        this.user = user;
    }

    public static Behavior<Login.LoginResult> create(ActorRef<Root.UserCommand> user) {
        return Behaviors.setup((context) -> new LoginAdapter(context, user));
    }

    @Override
    public Receive<Login.LoginResult> createReceive() {
        return newReceiveBuilder()
                .onMessage(Login.LoginSuccess.class, result -> {
                    user.tell(new Root.LoginSuccess(result.getLobbeyActor(), result.getUsername()));
                    return Behaviors.same();
                })
                .build();
    }
}