package com.example.githubaccess.service;

import com.example.githubaccess.client.GithubApiClient;
import com.example.githubaccess.model.AccessReportResponse;
import com.example.githubaccess.model.UserAccessReport;
import com.example.githubaccess.util.ConcurrentAggregator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AccessReportService {

    private final GithubApiClient githubApiClient;

    public AccessReportService(GithubApiClient githubApiClient) {
        this.githubApiClient = githubApiClient;
    }

    public Mono<AccessReportResponse> generateReport(String org) {
        ConcurrentAggregator aggregator = new ConcurrentAggregator();

        return githubApiClient.fetchOrgRepositories(org)
                .flatMap(repo ->
                        githubApiClient.fetchRepositoryCollaborators(org, repo.name())
                                .doOnNext(collaborator -> aggregator.addAccess(collaborator.login(), repo.name()))
                                .then(),
                        20
                )
                .then(Mono.fromCallable(() -> buildResponse(org, aggregator)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private AccessReportResponse buildResponse(String org, ConcurrentAggregator aggregator) {
        List<UserAccessReport> users = new ArrayList<>();

        aggregator.getSnapshot().forEach((username, repos) -> {
            List<String> sortedRepos = new ArrayList<>(repos);
            sortedRepos.sort(Comparator.naturalOrder());
            users.add(new UserAccessReport(username, sortedRepos));
        });

        users.sort(Comparator.comparing(UserAccessReport::username));
        return new AccessReportResponse(org, users);
    }
}
