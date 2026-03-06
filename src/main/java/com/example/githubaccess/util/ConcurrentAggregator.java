package com.example.githubaccess.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentAggregator {

    private final ConcurrentHashMap<String, List<String>> userRepoMap = new ConcurrentHashMap<>();

    public void addAccess(String username, String repositoryName) {
        userRepoMap.computeIfAbsent(username, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(repositoryName);
    }

    public ConcurrentHashMap<String, List<String>> getSnapshot() {
        return userRepoMap;
    }
}
