package models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import services.NewsService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Pham Bao Quynh Nguyen
 */

public class SourceViewModelTest {

    private NewsService newsService;
    private SourceViewModel sourceViewModel;

    @Before
    public void setUp() {
        newsService = Mockito.mock(NewsService.class);
    }

    @Test
    public void testGetSourceInfo() throws Exception {

        sourceViewModel = new SourceViewModel(newsService);

        // Arrange
        String domain = "example.com";
        JsonNode fakeJson = JsonNodeFactory.instance.objectNode()
                .put("domain", domain);

        when(newsService.getSourceInfo(domain))
                .thenReturn(CompletableFuture.completedFuture(fakeJson));

        // Act
        JsonNode result = sourceViewModel.getSourceInfo(domain).toCompletableFuture().get();

        // Assert
        assertThat(result.get("domain").asText()).isEqualTo(domain);
        verify(newsService, times(1)).getSourceInfo(domain);
    }

    @Test
    public void testGetTop10ArticlesForSpecificSource() throws Exception {

        sourceViewModel = new SourceViewModel(newsService);

        // Arrange
        String domain = "example.com";
        JsonNode article1 = JsonNodeFactory.instance.objectNode().put("title", "Article 1");
        JsonNode article2 = JsonNodeFactory.instance.objectNode().put("title", "Article 2");
        List<JsonNode> fakeArticles = List.of(article1, article2);

        when(newsService.getTop10ArticlesForSpecificSource(domain))
                .thenReturn(CompletableFuture.completedFuture(fakeArticles));

        // Act
        List<JsonNode> result = sourceViewModel.getTop10ArticlesForSpecificSource(domain).toCompletableFuture().get();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("title").asText()).isEqualTo("Article 1");
        assertThat(result.get(1).get("title").asText()).isEqualTo("Article 2");

        verify(newsService, times(1)).getTop10ArticlesForSpecificSource(domain);
    }
}
