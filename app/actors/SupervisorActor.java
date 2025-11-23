
package actors;

import org.apache.pekko.actor.typed.*;
import org.apache.pekko.actor.typed.javadsl.*;

/**
 * SupervisorActor (D2 Requirement)
 *
 * Defines the supervisor strategy for all child actors:
 *  - UserActor (per WebSocket)
 *  - SearchActor (per search request)
 *  - ResourceNewsActor (for filtering sources)
 *
 * Responsibilities:
 *  - Maintain application stability
 *  - Auto‑restart child actors on failure
 *
 * AUTHOR: Sara Ezzati
 */
public class SupervisorActor {

    /** Marker interface for Supervisor messages. */
    public interface Command {}

    /**
     * Wraps an actor behavior with a restart‑on‑failure strategy.
     *
     * INPUT:
     *   childBehavior — Behavior<T> of the child actor
     *
     * OUTPUT:
     *   Behavior<T> wrapped with supervision
     */
    public static <T> Behavior<T> supervise(Behavior<T> childBehavior) {
        return Behaviors.supervise(childBehavior)
                .onFailure(Exception.class, SupervisorStrategy.restart());
    }

    /**
     * Spawns a supervised child actor.
     *
     * @param childBehavior the child behavior definition
     * @param ctx           actor context
     * @param name          unique actor name
     * @return supervised child actor reference
     */
    public static <T> ActorRef<T> spawnSupervised(
            Behavior<T> childBehavior,
            ActorContext<?> ctx,
            String name
    ) {
        return ctx.spawn(supervise(childBehavior), name);
    }

    /**
     * Factory used by HomeController → SupervisorActor → UserActor linkage.
     *
     * @param out WebSocket output actor
     * @return Behavior<UserActor.Command>
     */
    public static Behavior<Command> createUserActor(ActorRef<String> out) {
        return Behaviors.setup(ctx ->
            (Behavior<Command>) UserActor.createLinked(ctx, out)
        );
    }
}
