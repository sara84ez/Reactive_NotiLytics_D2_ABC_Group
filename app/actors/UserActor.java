
package actors;

import org.apache.pekko.actor.typed.*;
import org.apache.pekko.actor.typed.javadsl.*;

import app.models.Article;
import app.models.SourceInfo;
import java.util.*;

/**
 * UserActor (Core of D2 Reactive Push)
 *
 * One UserActor is created per WebSocket session.
 *
 * Responsibilities:
 *  - Receive search requests from WebSocket
 *  - Forward requests to SearchActor and ResourceNewsActor
 *  - Receive streaming results and push them to WebSocket
 *  - Filter duplicates, maintain history
 *
 * INPUT TYPES:
 *   - UserSearch(query)
 *   - UserRequestSources(country, category, language)
 *   - IncomingArticles(List<Article>)
 *   - IncomingSources(List<SourceInfo>)
 *
 * OUTPUT:
 *   - JSON strings pushed to the WebSocket
 *
 * AUTHOR: Sara Ezzati
 */
public class UserActor extends AbstractBehavior<UserActor.Command> {

    /* ============================================================
       MESSAGE PROTOCOL
       ============================================================ */

    public interface Command {}

    public static final class UserSearch implements Command {
        public final String query;
        public UserSearch(String q) { this.query = q; }
    }

    public static final class UserRequestSources implements Command {
        public final String country;
        public final String category;
        public final String language;
        public UserRequestSources(String c, String cat, String l) {
            this.country = c; this.category = cat; this.language = l;
        }
    }

    public static final class IncomingArticles implements Command {
        public final List<Article> articles;
        public IncomingArticles(List<Article> a) { this.articles = a; }
    }

    public static final class IncomingSources implements Command {
        public final List<SourceInfo> sources;
        public IncomingSources(List<SourceInfo> s) { this.sources = s; }
    }

    public static final class PushToWebSocket implements Command {
        public final String payload;
        public PushToWebSocket(String json) { this.payload = json; }
    }

    /* ============================================================
       INTERNAL FIELDS
       ============================================================ */

    private final ActorRef<String> websocketOut;
    private final Set<String> seenIds = new HashSet<>();

    /* ============================================================
       FACTORY
       ============================================================ */

    public static Behavior<Command> createLinked(ActorContext<?> parentCtx, ActorRef<String> wsOut) {
        return Behaviors.setup(ctx -> new UserActor(ctx, wsOut));
    }

    private UserActor(ActorContext<Command> ctx, ActorRef<String> wsOut) {
        super(ctx);
        this.websocketOut = wsOut;
    }

    /* ============================================================
       MAIN MESSAGE DISPATCH
       ============================================================ */

    @Override
    public Behavior<Command> onMessage(Command msg) {

        if (msg instanceof UserSearch m) return onSearch(m.query);

        if (msg instanceof IncomingArticles m) return onIncomingArticles(m.articles);

        if (msg instanceof IncomingSources m) return onIncomingSources(m.sources);

        if (msg instanceof PushToWebSocket m) {
            websocketOut.tell(m.payload);
            return this;
        }

        return this;
    }

    /* ============================================================
       MESSAGE HANDLERS
       ============================================================ */

    private Behavior<Command> onSearch(String query) {
        // TODO: integrate SearchActor when its API is provided
        return this;
    }

    private Behavior<Command> onIncomingArticles(List<Article> list) {
        List<Article> fresh = new ArrayList<>();

        for (Article a : list) {
            if (!seenIds.contains(a.id)) {
                seenIds.add(a.id);
                fresh.add(a);
            }
        }

        if (!fresh.isEmpty()) {
            websocketOut.tell(toJsonArticles(fresh));
        }

        return this;
    }

    private Behavior<Command> onIncomingSources(List<SourceInfo> list) {
        websocketOut.tell(toJsonSources(list));
        return this;
    }

    /* ============================================================
       JSON HELPERS
       ============================================================ */

    private String toJsonArticles(List<Article> list) {
        StringBuilder sb = new StringBuilder("{"articles":[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toJson());
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }

    private String toJsonSources(List<SourceInfo> list) {
        StringBuilder sb = new StringBuilder("{"sources":[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i).toJson());
            if (i < list.size() - 1) sb.append(",");
        }
        sb.append("]}");
        return sb.toString();
    }
}
