package test;

import org.apache.pekko.actor.testkit.typed.javadsl.*;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.jupiter.api.*;

import app.actors.ResourceNewsActor;
import app.services.NewsApiServiceMock;

/**
 * Real tests for ResourceNewsActor using Pekko TestKit.
 *
 * Author: Sara Ezzati
 */
public class ResourceNewsActorTest {

    private static ActorTestKit testKit;

    @BeforeAll
    static void init() {
        testKit = ActorTestKit.create();
    }

    @AfterAll
    static void close() {
        testKit.shutdownTestKit();
    }

    @Test
    public void testGetSourcesReturnsMock() {
        TestProbe<ResourceNewsActor.SourcesResponse> probe =
                testKit.createTestProbe();

        ActorRef<ResourceNewsActor.Command> actor =
                testKit.spawn(ResourceNewsActor.create(new NewsApiServiceMock()));

        actor.tell(new ResourceNewsActor.GetSources("us","general","en", probe.getRef()));

        ResourceNewsActor.SourcesResponse res =
                probe.expectMessageClass(ResourceNewsActor.SourcesResponse.class);

        Assertions.assertEquals(1, res.sources.size());
        Assertions.assertEquals("Mock Source", res.sources.get(0).name);
    }
}
