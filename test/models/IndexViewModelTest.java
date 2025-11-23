package models;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import services.NewsService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Pham Bao Quynh Nguyen
 */
public class IndexViewModelTest {

    private NewsService newsService;
    private IndexViewModel indexViewModel;

    @Before
    public void setUp() {
        newsService = Mockito.mock(NewsService.class);
    }

    @Test
    public void testGetResults() {
        indexViewModel = new IndexViewModel(newsService);
        List<SearchResult> list = indexViewModel.getResults("MySession");
        assertThat(list.isEmpty()).isEqualTo(true);
    }

    @Test
    public void testGetNews() throws Exception {

        indexViewModel = new IndexViewModel(newsService);

        String sessionId = "session2";
        String query = "emptyTest";
        String sortBy = "latest";

        ObjectNode fakeJson = JsonNodeFactory.instance.objectNode();
        ArrayNode articlesArray = JsonNodeFactory.instance.arrayNode();
        fakeJson.set("articles", articlesArray);
        fakeJson.put("totalResults", 10);

        when(newsService.searchNews(query, sortBy))
                .thenReturn(CompletableFuture.completedFuture(fakeJson));

        SearchResult result = indexViewModel.getNews(sessionId, query, sortBy).toCompletableFuture().join();

        // Verify result
        assertThat(result.query).isEqualTo(query);
        assertThat(result.sortBy).isEqualTo(sortBy);
        assertThat(result.articles).isEmpty();
        assertThat(result.totalResults).isEqualTo(10);

        // Verify sessionResults
        List<SearchResult> storedResults = indexViewModel.getResults(sessionId);
        assertThat(storedResults).hasSize(1);
        assertThat(storedResults.get(0)).isEqualTo(result);
    }
}
