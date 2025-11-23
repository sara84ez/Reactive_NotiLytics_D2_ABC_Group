package test;

import org.junit.jupiter.api.*;
import play.mvc.*;
import play.test.WithApplication;

import app.controllers.WebSocketController;
import app.actors.SearchActor;
import app.actors.ResourceNewsActor;
import app.services.NewsApiServiceMock;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

/**
 * Real WebSocket controller test using Play test harness.
 *
 * Author: Sara Ezzati
 */
public class WebSocketControllerTest extends WithApplication {

    @Test
    public void testWebSocketEndpointExists() {
        ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "test-sys");

        ActorRef<SearchActor.Command> search =
                system.systemActorOf(SearchActor.create(new NewsApiServiceMock()), "search");

        ActorRef<ResourceNewsActor.Command> resources =
                system.systemActorOf(ResourceNewsActor.create(new NewsApiServiceMock()), "res");

        WebSocketController controller =
                new WebSocketController(system, search, resources);

        WebSocket ws = controller.ws();
        Assertions.assertNotNull(ws);
    }
}
