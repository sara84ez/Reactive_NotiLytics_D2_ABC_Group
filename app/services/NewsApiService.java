package app.services;

import app.models.Article;
import app.models.SourceInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * NewsApiService provides asynchronous access to NewsAPI endpoints.
 * All network calls must be non-blocking for Delivery 2.
 *
 * Methods:
 *  - searchArticles(query): retrieves news articles
 *  - getSources(country, category, language): retrieves news sources
 *
 * Author: Sara Ezzati
 */
public interface NewsApiService {

    CompletionStage<List<Article>> searchArticles(String query);

    CompletionStage<List<SourceInfo>> getSources(String country, String category, String language);
}
