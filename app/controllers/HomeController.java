
package controllers;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.stream.Materializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.*;
import javax.inject.Inject;
import actors.SupervisorActor;
import play.libs.streams.ActorFlow;

/**
 * HomeController (Reactive D2 Version)
 *
 * This controller replaces the traditional HTTP-based controller from D1.
 * For Delivery 2, all interactions must be fully reactive and WebSocket-based.
 *
 * Responsibilities:
 *  - Accept a WebSocket connection for each client.
 *  - Spawn a new UserActor (via SupervisorActor) per WebSocket session.
 *  - Route all real-time messages through server‑push updates.
 *
 * INPUT:
 *    - WebSocket upgrade request
 *
 * OUTPUT:
 *    - A bidirectional WebSocket stream managed by Pekko Actors
 *
 * AUTHOR: Sara Ezzati
 */
public class HomeController extends Controller {

    private final Logger logger = LoggerFactory.getLogger("application");
    private final ActorSystem<SupervisorActor.Command> supervisor;
    private final Materializer materializer;

    /**
     * Constructor for dependency injection.
     *
     * @param supervisor   Typed Pekko actor system hosting SupervisorActor
     * @param materializer Stream materializer for WebSocket flows
     */
    @Inject
    public HomeController(ActorSystem<SupervisorActor.Command> supervisor, Materializer materializer) {
        this.supervisor = supervisor;
        this.materializer = materializer;
    }

    /**
     * Reactive WebSocket endpoint (D2 requirement).
     *
     * Each new WebSocket connection spawns ONE UserActor supervised by SupervisorActor.
     *
     * @return WebSocket handler
     */
    public WebSocket ws() {
        logger.info("Opening WebSocket connection (D2 Reactive Mode)");

        return WebSocket.Text.accept(request ->
            ActorFlow.actorRef(
                out -> SupervisorActor.createUserActor(out),
                supervisor,
                materializer
            )
        );
    }
}
