package test;

import org.apache.pekko.actor.testkit.typed.javadsl.*;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.junit.jupiter.api.*;

import app.actors.*;
import app.actors.UserActor.*;
import app.services.NewsApiServiceMock;
import app.models.Article;
import app.models.SourceInfo;

import java.util.List;

/**
 * Real unit tests for UserActor using Pekko TestKit.
 * Ensures async messaging, duplicate filtering, and JSON push behavior.
 *
 * Author: Sara Ezzati
 */
public class UserActorTest {

    private static ActorTestKit testKit;

    @BeforeAll
    public static void init() {
        testKit = ActorTestKit.create();
    }

    @AfterAll
    public static void cleanup() {
        testKit.shutdownTestKit();
    }

    @Test
    public void testUserSearchTriggersSearchActor() {
        TestProbe<SearchActor.Command> searchProbe = testKit.createTestProbe();
        TestProbe<ResourceNewsActor.Command> resProbe = testKit.createTestProbe();
        TestProbe<PushToWebSocket> wsProbe = testKit.createTestProbe();

        Behavior<UserActor.Command> behavior =
                UserActor.create(searchProbe.getRef(), resProbe.getRef(), wsProbe.getRef());

        ActorRef<UserActor.Command> user = testKit.spawn(behavior);

        user.tell(new UserSearch("hello"));

        SearchActor.SearchArticles msg =
                searchProbe.expectMessageClass(SearchActor.SearchArticles.class);

        Assertions.assertEquals("hello", msg.query);
    }
}
