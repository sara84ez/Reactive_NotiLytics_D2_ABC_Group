package modules;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

import app.actors.SupervisorActor;
import app.actors.SearchActor;
import app.actors.ResourceNewsActor;
import app.services.NewsApiService;

/**
 * Clean D2-only Guice Module
 * Wires only the reactive actors required for Delivery 2:
 *  - SupervisorActor
 *  - SearchActor
 *  - ResourceNewsActor
 *
 * Author: Sara Ezzati
 */
public class Module extends AbstractModule {

    @Override
    protected void configure() {

        // --- Root Actor System ---
        ActorSystem<SupervisorActor.Command> system =
                ActorSystem.create(SupervisorActor.create(), "notilytics-reactive-system");

        // --- Services (Real Implementation) ---
        NewsApiService newsApiService = new NewsApiService();

        // --- Child Actors registered under Supervisor ---

        ActorRef<SearchActor.Command> searchActor =
                system.systemActorOf(
                        SearchActor.create(newsApiService),
                        "search-actor"
                );

        ActorRef<ResourceNewsActor.Command> resourceActor =
                system.systemActorOf(
                        ResourceNewsActor.create(newsApiService),
                        "resource-actor"
                );

        // --- Bindings for Dependency Injection ---

        bind(new TypeLiteral<ActorSystem<SupervisorActor.Command>>() {})
                .toInstance(system);

        bind(new TypeLiteral<ActorRef<SearchActor.Command>>() {})
                .toInstance(searchActor);

        bind(new TypeLiteral<ActorRef<ResourceNewsActor.Command>>() {})
                .toInstance(resourceActor);

        bind(NewsApiService.class).toInstance(newsApiService);
    }
}
