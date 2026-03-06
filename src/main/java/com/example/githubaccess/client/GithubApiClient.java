package com.example.githubaccess.client;

import com.example.githubaccess.model.CollaboratorResponse;
import com.example.githubaccess.model.RepositoryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

@Component
public class GithubApiClient {

    private final WebClient webClient;
    private final int perPage;

    public GithubApiClient(WebClient githubWebClient,
                           @Value("${github.api.per-page}") int perPage) {
        this.webClient = githubWebClient;
        this.perPage = perPage;
    }

    public Flux<RepositoryResponse> fetchOrgRepositories(String org) {
        return paginateFlux(page ->
                webClient.get()
                        .uri("/orgs/{org}/repos?type=all&per_page={perPage}&page={page}", org, perPage, page)
                        .retrieve()
                        .onStatus(
                                status -> status == HttpStatus.NOT_FOUND,
                                response -> response.createException()
                                        .map(ex -> new IllegalArgumentException("Organization not found: " + org))
                        )
                        .onStatus(
                                status -> status == HttpStatus.UNAUTHORIZED,
                                response -> response.createException()
                                        .map(ex -> new SecurityException("Invalid or missing GitHub token"))
                        )
                        .bodyToFlux(RepositoryResponse.class)
                        .collectList()
        );
    }

    public Flux<CollaboratorResponse> fetchRepositoryCollaborators(String org, String repoName) {
        return paginateFlux(page ->
                webClient.get()
                        .uri("/repos/{org}/{repo}/collaborators?affiliation=all&per_page={perPage}&page={page}",
                                org, repoName, perPage, page)
                        .retrieve()
                        .bodyToFlux(CollaboratorResponse.class)
                        .collectList()
                        .onErrorResume(WebClientResponseException.Forbidden.class, ex -> Mono.just(List.of()))
                        .onErrorResume(WebClientResponseException.NotFound.class, ex -> Mono.just(List.of()))
        );
    }

    private <T> Flux<T> paginateFlux(Function<Integer, Mono<List<T>>> pageFetcher) {
        return Flux.range(1, Integer.MAX_VALUE)
                .concatMap(pageFetcher)
                .takeWhile(page -> !page.isEmpty())
                .flatMap(Flux::fromIterable);
    }
}
