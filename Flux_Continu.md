import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactiv.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Slf4j
@Service
public class ParallelStreamService {

    private final WebClient webClient;

    public ParallelStreamService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://api.exemple.com").build();
    }

    public void consumeInParallel() {
        this.webClient.get()
            .uri("/v1/data-stream")
            .headers(h -> h.setBearerAuth("VOTRE_TOKEN"))
            .retrieve()
            .bodyToFlux(StockPrice.class)
            // On ne perd rien : si le débit est trop fort, le flux attend ici
            .onBackpressureBuffer(500) 
            
            // Parallélisation : on traite jusqu'à 10 éléments simultanément
            .flatMap(data -> {
                return processAsync(data)
                    .subscribeOn(Schedulers.boundedElastic()); // Exécution sur pool de threads élastique
            }, 10) // <-- Paramètre de concurrence (max 10 tâches en parallèle)
            
            .subscribe(
                result -> log.info("Traité avec succès: {}", result),
                error -> log.error("Erreur fatale: ", error)
            );
    }

    private Mono<String> processAsync(StockPrice data) {
        return Mono.fromCallable(() -> {
            // Votre logique métier complexe (appel DB, calcul, etc.)
            log.info("Traitement de {}", data.getId());
            Thread.sleep(500); // Simulation d'une tâche de 500ms
            return "OK-" + data.getId();
        });
    }
}
