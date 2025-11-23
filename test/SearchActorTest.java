package test;

import org.apache.pekko.actor.testkit.typed.javadsl.*;
import org.apache.pekko.actor.typed.ActorRef;
import org.junit.jupiter.api.*;

import app.actors.SearchActor;
import app.services.NewsApiServiceMock;

/**
 * Real tests for SearchActor using Pekko TestKit.
 *
 * Author: Sara Ezzati
 */
public class SearchActorTest {

    private static ActorTestKit testKit;

    @BeforeAll
    public static void setup() {
        testKit = ActorTestKit.create();
    }

    @AfterAll
    public static void tearDown() {
        testKit.shutdownTestKit();
    }

    @Test
    public void testSearchReturnsMockArticle() {
        TestProbe<SearchActor.SearchResults> probe = testKit.createTestProbe();

        ActorRef<SearchActor.Command> actor =
                testKit.spawn(SearchActor.create(new NewsApiServiceMock()));

        actor.tell(new SearchActor.SearchArticles("hello", probe.getRef()));

        SearchActor.SearchResults result =
                probe.expectMessageClass(SearchActor.SearchResults.class);

        Assertions.assertEquals(1, result.articles.size());
        Assertions.assertEquals("Test Title", result.articles.get(0).title);
    }
}
