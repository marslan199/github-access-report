package com.example.githubaccess.model;

import java.util.List;

public record AccessReportResponse(
        String organization,
        List<UserAccessReport> users
) {}
