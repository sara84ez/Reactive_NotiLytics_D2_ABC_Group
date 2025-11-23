package controllers;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.stream.Materializer;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.mvc.Controller;
import play.mvc.WebSocket;
import play.libs.streams.ActorFlow;

import actors.SupervisorActor;
import actors.UserActor;

/**
 * Delivery 2 (D2) WebSocket-only controller for NotiLytics.
 * <p>
 * This controller exposes a single WebSocket endpoint used for all
 * real-time bidirectional communication between the frontend and the backend.
 * There are no HTTP endpoints in D2 (Reactive architecture requirement).
 * </p>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *     <li>Upgrade HTTP request to WebSocket</li>
 *     <li>Create a dedicated {@link UserActor} for each client</li>
 *     <li>Route all WebSocket messages to the SupervisorActor system</li>
 *     <li>Ensure reactive, asynchronous, non-blocking communication</li>
 * </ul>
 *
 * <h2>Author</h2>
 * <p><b>Sara Ezzati</b></p>
 */
public class HomeController extends Controller {

    private final Logger logger = LoggerFactory.getLogger("application");

    /** The root typed actor system that supervises all UserActors. */
    private final ActorSystem<SupervisorActor.Command> supervisor;

    /** Materializer required by ActorFlow to bind streams. */
    private final Materializer materializer;

    /**
     * Constructs the D2 reactive WebSocket controller.
     *
     * @param supervisor The root actor system housing the SupervisorActor.
     * @param materializer Stream materializer used by ActorFlow.
     */
    @Inject
    public HomeController(
            ActorSystem<SupervisorActor.Command> supervisor,
            Materializer materializer
    ) {
        this.supervisor = supervisor;
        this.materializer = materializer;
    }

    /**
     * Main WebSocket endpoint for D2.
     * <p>
     * Each client establishes a WebSocket connection which automatically spawns
     * a new {@link UserActor} via the SupervisorActor. The communication is fully
     * asynchronous and actor-driven.
     * </p>
     *
     * <h3>Input (from frontend)</h3>
     * <pre>
     * {
     *   "type": "search",
     *   "query": "bitcoin"
     * }
     *
     * {
     *   "type": "sources",
     *   "country": "us",
     *   "category": "technology",
     *   "language": "en"
     * }
     * </pre>
     *
     * <h3>Output (to frontend)</h3>
     * <pre>
     * {
     *   "articles": [ ... ]
     * }
     *
     * {
     *   "sources": [ ... ]
     * }
     * </pre>
     *
     * @return A WebSocket instance connected to a dedicated UserActor.
     */
    public WebSocket ws() {
        logger.info("WebSocket connection requested.");

        return WebSocket.Text.accept(request ->
                ActorFlow.actorRef(
                        /** For each client, create a new UserActor under SupervisorActor */
                        (ActorRef<String> out) -> SupervisorActor.createUserActor(out),

                        /** Provide SupervisorActor system */
                        supervisor,

                        /** Provide materializer */
                        materializer
                )
        );
    }
}
