package com.example.githubaccess.model;

import java.util.List;

public record UserAccessReport(
        String username,
        List<String> repositories
) {}
