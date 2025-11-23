package controllers;

import models.WordStatsViewModel;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.Sentiment;
import models.SentimentAnalysisResult;
import models.IndexViewModel;
import models.SearchResult;
import models.Source;
import models.SourceViewModel;
import models.WordStatsViewModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import play.libs.Json;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;
import services.SentimentAnalyzer;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;
import services.NewsApiClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static play.test.Helpers.*;
import static play.mvc.Http.Status.OK;


public class HomeControllerTest extends WithApplication {

    // Group part + P1: HomeController---------------------------------------------------------------------------------

    private IndexViewModel indexViewModel;
    private SourceViewModel sourceViewModel;
    private NewsApiClient client;
    private HomeController controller;
    private WordStatsViewModel wordStatsViewModel;

    private static SentimentAnalysisResult neutralStatement() {
        return new SentimentAnalysisResult(Sentiment.NEUTRAL, 0, 0,0,0);
    }

    @Before
    public void setup() {
        indexViewModel = Mockito.mock(IndexViewModel.class);
        sourceViewModel = Mockito.mock(SourceViewModel.class);
        client = Mockito.mock(NewsApiClient.class);
        wordStatsViewModel = Mockito.mock(WordStatsViewModel.class);
        controller = new HomeController(indexViewModel, sourceViewModel, client,wordStatsViewModel);
    }
    private static final ObjectMapper M = new ObjectMapper();

    /**
     * @author Pham Bao Quynh Nguyen
     */
    @Test
    public void testIndex() {

        when(indexViewModel.getResults(any(String.class))).thenReturn(Collections.emptyList());

        Http.Request request = new Http.RequestBuilder()
                .method(GET)
                .uri("/Notilytics")
                .build();

        Result result = controller.index(request);

        assertThat(result.status()).isEqualTo(OK);
        assertThat(result.session().getOptional("sessionId")).isPresent();
    }

    /**
     * @author Pham Bao Quynh Nguyen
     */
    @Test
    public void testSearch() {
        // Dummy result
        SearchResult dummyResult = new SearchResult("test", "relevancy", Collections.emptyList(), 0,neutralStatement());

        // Stub getNews to force synchronous execution and ensure thenApply runs
        when(indexViewModel.getNews(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    SearchResult result = dummyResult;
                    // Return a completed future and immediately apply a lambda
                    return CompletableFuture.completedFuture(result)
                            .thenApply(r -> {
                                return r;
                            });
                });

        // Stub getResults
        when(indexViewModel.getResults(anyString()))
                .thenReturn(Collections.singletonList(dummyResult));

        // Build request
        Http.Request request = new Http.RequestBuilder()
                .method(GET)
                .uri("/Notilytics/search")
                .build();

        // Call controller
        Result result = controller.search("test", "relevancy", request)
                .toCompletableFuture()
                .join();

        // Assertions
        assertThat(result.status()).isEqualTo(OK);
        assertThat(result.session().getOptional("sessionId")).isPresent();

        // Verify getNews was called with correct arguments
        verify(indexViewModel).getNews(anyString(), eq("test"), eq("relevancy"));
    }

    /**
     * @author Pham Bao Quynh Nguyen
     */
    @Test
    public void testSource() throws Exception {
        String domain = "example.com";

        // Dummy source info
        JsonNode dummySourceInfo = Json.newObject()
                .put("name", "Example Source")
                .put("description", "Example description")
                .put("url", "https://example.com");

        // Dummy article
        JsonNode dummyArticle = Json.newObject()
                .put("title", "Test Article")
                .put("url", "https://example.com")
                .put("author", "John Doe")
                .put("publishedAt", "2025-11-05T12:00:00Z")
                .put("description", "This is a test article")
                .set("source", Json.newObject().put("id", "source-id").put("name", "Example Source"));

        List<JsonNode> dummyArticles = Collections.singletonList(dummyArticle);

        // Stub SourceViewModel
        when(sourceViewModel.getSourceInfo(anyString()))
                .thenReturn(CompletableFuture.completedFuture(dummySourceInfo));
        when(sourceViewModel.getTop10ArticlesForSpecificSource(anyString()))
                .thenReturn(CompletableFuture.completedFuture(dummyArticles));

        // Call controller
        CompletionStage<Result> stage = controller.source(domain);
        Result result = stage.toCompletableFuture().join();

        // Assertions
        assertThat(result.status()).isEqualTo(OK);
    }

    //-------------------------------------------------------------------------------------------------------
    // P3: Home Controller
    @Test
    public void testList_returnsJson() throws Exception {
        // Dummy Source object
        Source dummySource = new Source("id1", "Name1", "Desc", "http://url.com", "tech", "en", "us");
        List<Source> dummySources = Collections.singletonList(dummySource);

        // Stub client.getSources
        when(client.getSources(any(), any(), any()))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(dummySources));

        // Call controller
        CompletionStage<Result> stage = controller.list("us", "tech", "en", "json");
        Result result = stage.toCompletableFuture().join();

        assertThat(result.status()).isEqualTo(OK);
        assertThat(result.contentType().orElse("")).isEqualTo("application/json");

        // Get content string properly
        String jsonContent = play.test.Helpers.contentAsString(result);
        assertThat(Json.parse(jsonContent).get(0).get("id").asText()).isEqualTo("id1");
    }

    //P3 Test
    @Test
    public void apiReturnsJson() throws Exception {
        // mock dependencies
        NewsApiClient fakeClient = Mockito.mock(NewsApiClient.class);
        IndexViewModel indexVm = Mockito.mock(IndexViewModel.class);
        SourceViewModel sourceVm = Mockito.mock(SourceViewModel.class);

        when(fakeClient.getSources(any(), any(), any()))
                .thenReturn(
                        CompletableFuture.completedFuture(
                                List.of(
                                        new Source("x", "X", "desc", "https://x.test", "general", "en", "us")
                                )
                        )
                );

        HomeController controller = new HomeController(indexVm, sourceVm, fakeClient,wordStatsViewModel);

        Result result = controller.api("us", "", "en").toCompletableFuture().get();

        assertThat(result.status()).isEqualTo(200);
        assertThat(result.contentType().orElse("")).isEqualTo("application/json");
    }

    @Test
    public void wordStatsTest() {
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(GET)
                .uri("/Notilytics/stats");
        Result result = route(app, request);
        assertEquals(OK, result.status());
    }

    //P4 Test
    private static JsonNode article(String desc) {
        ObjectNode node = M.createObjectNode();
        if(desc == null) {
            node.putNull("description");
        } else {
            node.put("description", desc);
        }
        return node;
    }

    @Test
    public void classifyArticleTest() {
        JsonNode nullNode = NullNode.getInstance();
        String happy_text = "good great excellent amazing";
        String sad_text = "bad terrible awful sad good great";
        String neutral_text = "good great love sad bad hate";
        String unknown_text = "words not covered in dictionary";

        assertEquals(Sentiment.NEUTRAL, SentimentAnalyzer.classifyArticle(null));
        assertEquals(Sentiment.NEUTRAL, SentimentAnalyzer.classifyArticle(nullNode));
        assertEquals(Sentiment.NEUTRAL, SentimentAnalyzer.classifyArticle(article(null)));
        assertEquals(Sentiment.NEUTRAL, SentimentAnalyzer.classifyArticle(article(" ")));
        assertEquals(Sentiment.NEUTRAL, SentimentAnalyzer.classifyArticle(article(neutral_text)));
        assertEquals(Sentiment.NEUTRAL, SentimentAnalyzer.classifyArticle(article(unknown_text)));

        assertEquals(Sentiment.HAPPY, SentimentAnalyzer.classifyArticle(article(happy_text)));
        assertEquals(Sentiment.SAD, SentimentAnalyzer.classifyArticle(article(sad_text)));
    }

    @Test
    public void analyzeStreamTest() {
        List<JsonNode> happy_list = List.of(
                article("good great excellent"),
                article("great amazing")
        );

        List<JsonNode> sad_list = List.of(
                article("bad terrible"),
                article("awful sad")
        );

        List<JsonNode> neutral_list = List.of(
                article("good great excellent"),
                article("bad terrible sad")
        );

        SentimentAnalysisResult happy_result = SentimentAnalyzer.analyzeStream(happy_list);
        SentimentAnalysisResult sad_result = SentimentAnalyzer.analyzeStream(sad_list);
        SentimentAnalysisResult neutral_result = SentimentAnalyzer.analyzeStream(neutral_list);

        assertEquals(Sentiment.HAPPY, happy_result.overall);
        assertEquals(2, happy_result.happyCount);
        assertEquals(0, happy_result.sadCount);

        assertEquals(Sentiment.SAD, sad_result.overall);
        assertEquals(2, sad_result.sadCount);
        assertEquals(0, sad_result.happyCount);

        assertEquals(Sentiment.NEUTRAL, neutral_result.overall);
    }

    @Test
    public void classifyArticleExceptionHandlingTest() {
        ObjectNode exception = new ObjectNode(null) {
            @Override
            public JsonNode get(String fieldName) {
                throw new RuntimeException();
            }
        };

        Sentiment classifyArticle = SentimentAnalyzer.classifyArticle(exception);
        assertEquals(Sentiment.NEUTRAL, classifyArticle);
    }

    @Test
    public void analyzeStreamExceptionHandlingTest() {
        List<JsonNode> exceptionList = new java.util.AbstractList<JsonNode>() {
            @Override
            public JsonNode get(int index) {
                throw new RuntimeException();
            }

            @Override
            public int size() {
                return 1;
            }
        };

        SentimentAnalysisResult result = SentimentAnalyzer.analyzeStream(exceptionList);
        assertEquals(Sentiment.NEUTRAL, result.overall);
    }
}

