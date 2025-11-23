
package app.actors;

import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.javadsl.*;

import app.services.NewsApiService;
import app.models.SourceInfo;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * ResourceNewsActor
 *
 * Reactive actor responsible for retrieving <b>news sources</b> from the News API.
 * One shared instance of this actor is created in the Guice {@code Module} and
 * used by {@link app.actors.UserActor} to implement the "News Sources" feature.
 *
 * <p><b>Delivery 2 responsibilities (group part + news sources):</b></p>
 * <ul>
 *   <li>Accept filters for country, category, and language</li>
 *   <li>Call {@link NewsApiService#getSources(String, String, String)} asynchronously</li>
 *   <li>Return a {@link SourcesResponse} to the requester (typically {@link app.actors.UserActor})</li>
 *   <li>Remain fully non-blocking (no join/get/sleep)</li>
 * </ul>
 *
 * <p>INPUT MESSAGE TYPES:</p>
 * <ul>
 *   <li>{@link GetSources} – includes optional filters and a {@code replyTo} actor</li>
 * </ul>
 *
 * <p>OUTPUT:</p>
 * <ul>
 *   <li>{@link SourcesResponse} – wraps a {@code List<SourceInfo>} and is delivered to the caller</li>
 * </ul>
 *
 * @author Sara Ezzati
 */
public final class ResourceNewsActor extends AbstractBehavior<ResourceNewsActor.Command> {

    /**
     * Marker interface for all commands that {@link ResourceNewsActor} can handle.
     */
    public interface Command { }

    /**
     * Command used to request a list of news sources from the News API.
     *
     * <p>INPUT:</p>
     * <ul>
     *   <li>{@code country}  – ISO country code filter (may be null or blank)</li>
     *   <li>{@code category} – news category filter (may be null or blank)</li>
     *   <li>{@code language} – language filter (may be null or blank)</li>
     *   <li>{@code replyTo} – actor that will receive {@link SourcesResponse}</li>
     * </ul>
     */
    public static final class GetSources implements Command {
        public final String country;
        public final String category;
        public final String language;
        public final ActorRef<SourcesResponse> replyTo;

        public GetSources(String country,
                          String category,
                          String language,
                          ActorRef<SourcesResponse> replyTo) {
            this.country = country;
            this.category = category;
            this.language = language;
            this.replyTo = replyTo;
        }
    }

    /**
     * Immutable wrapper for a list of news sources.
     *
     * <p>OUTPUT:</p>
     * <ul>
     *   <li>{@code sources} – list of sources returned from NewsAPI</li>
     * </ul>
     */
    public static final class SourcesResponse {
        public final List<SourceInfo> sources;

        public SourcesResponse(List<SourceInfo> sources) {
            this.sources = sources;
        }
    }

    /** Non-blocking NewsAPI facade injected via Guice. */
    private final NewsApiService newsApi;

    /**
     * Factory method used by {@link modules.Module} to create this actor.
     *
     * @param api asynchronous News API client implementation
     * @return {@link Behavior} instance that can be spawned as {@code ResourceNewsActor}
     */
    public static Behavior<Command> create(NewsApiService api) {
        return Behaviors.setup(ctx -> new ResourceNewsActor(ctx, api));
    }

    private ResourceNewsActor(ActorContext<Command> ctx, NewsApiService api) {
        super(ctx);
        this.newsApi = api;
    }

    @Override
    public Behavior<Command> onMessage(Command msg) {
        if (msg instanceof GetSources m) {
            return onGetSources(m);
        }
        // Unknown message type – ignore and keep behavior.
        return this;
    }

    /**
     * Handles {@link GetSources} by querying the NewsAPI sources endpoint.
     *
     * <p>This method MUST remain non-blocking. It calls
     * {@link NewsApiService#getSources(String, String, String)} and, when
     * the {@link java.util.concurrent.CompletionStage} completes, sends a
     * {@link SourcesResponse} back to the requester.</p>
     *
     * @param msg the {@link GetSources} command with filters and replyTo
     * @return current {@link Behavior} so the actor can continue processing messages
     */
    private Behavior<Command> onGetSources(GetSources msg) {
        final String country = msg.country;
        final String category = msg.category;
        final String language = msg.language;

        CompletionStage<List<SourceInfo>> future =
                newsApi.getSources(country, category, language);

        future.thenAccept(list ->
                msg.replyTo.tell(new SourcesResponse(list))
        );

        return this;
    }
}
