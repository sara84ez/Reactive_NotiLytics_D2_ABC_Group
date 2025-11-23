
package app.actors;

import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.javadsl.*;

import app.models.Article;
import app.services.NewsApiService;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * SearchActor
 *
 * Reactive actor responsible for querying the News API for articles.
 * One shared instance of this actor is created in the Guice {@code Module}
 * and used by all {@link app.actors.UserActor} instances.
 *
 * <p><b>Delivery 2 responsibilities (group part + news feature)</b></p>
 * <ul>
 *   <li>Receive search commands (user-entered query)</li>
 *   <li>Invoke {@link NewsApiService#searchArticles(String)} asynchronously</li>
 *   <li>Send results back to the caller (typically {@link app.actors.UserActor})</li>
 *   <li>Never block (no join/get/sleep)</li>
 * </ul>
 *
 * <p>INPUT MESSAGE TYPES:</p>
 * <ul>
 *   <li>{@link SearchArticles} – contains a free-text query and a {@code replyTo} actor</li>
 * </ul>
 *
 * <p>OUTPUT:</p>
 * <ul>
 *   <li>{@link SearchResults} – wraps a {@code List<Article>} and is sent to the caller via {@code replyTo}</li>
 * </ul>
 *
 * @author Sara Ezzati
 */
public final class SearchActor extends AbstractBehavior<SearchActor.Command> {

    /**
     * Marker interface for all messages that {@link SearchActor} can handle.
     */
    public interface Command { }

    /**
     * Message sent by {@link app.actors.UserActor} to trigger a search.
     *
     * <p>INPUT:</p>
     * <ul>
     *   <li>{@code query} – non-null search string (can be a phrase)</li>
     *   <li>{@code replyTo} – actor reference that will receive {@link SearchResults}</li>
     * </ul>
     */
    public static final class SearchArticles implements Command {
        public final String query;
        public final ActorRef<SearchResults> replyTo;

        public SearchArticles(String query, ActorRef<SearchResults> replyTo) {
            this.query = query;
            this.replyTo = replyTo;
        }
    }

    /**
     * Immutable wrapper for a list of articles returned from {@link NewsApiService}.
     *
     * <p>OUTPUT:</p>
     * <ul>
     *   <li>{@code articles} – list of articles matching the original query</li>
     * </ul>
     */
    public static final class SearchResults {
        public final List<Article> articles;

        public SearchResults(List<Article> articles) {
            this.articles = articles;
        }
    }

    /** Non-blocking NewsAPI facade injected via Guice. */
    private final NewsApiService newsApi;

    /**
     * Factory method used by Guice {@link modules.Module} to create this actor.
     *
     * @param api asynchronous News API client implementation
     * @return a {@link Behavior} that can be spawned as {@code SearchActor}
     */
    public static Behavior<Command> create(NewsApiService api) {
        return Behaviors.setup(ctx -> new SearchActor(ctx, api));
    }

    private SearchActor(ActorContext<Command> ctx, NewsApiService api) {
        super(ctx);
        this.newsApi = api;
    }

    @Override
    public Behavior<Command> onMessage(Command msg) {
        if (msg instanceof SearchArticles m) {
            return onSearchArticles(m);
        }
        // Unknown message type – ignore and keep same behavior
        return this;
    }

    /**
     * Handles {@link SearchArticles} by asynchronously querying the News API.
     *
     * <p>This method MUST be non-blocking. It triggers the HTTP call via
     * {@link NewsApiService#searchArticles(String)} and, once the future completes,
     * sends a {@link SearchResults} message to the {@code replyTo} actor.</p>
     *
     * @param msg the {@link SearchArticles} message containing query and replyTo
     * @return current {@link Behavior} (actor remains available for more messages)
     */
    private Behavior<Command> onSearchArticles(SearchArticles msg) {
        // Defensive null-handling – treat null query as empty string.
        final String query = msg.query == null ? "" : msg.query;

        CompletionStage<List<Article>> future = newsApi.searchArticles(query);

        // Asynchronously send SearchResults to the caller, without blocking this actor.
        future.thenAccept(articles ->
                msg.replyTo.tell(new SearchResults(articles))
        );

        return this;
    }
}
