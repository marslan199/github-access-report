package com.example.githubaccess.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RepositoryResponse(
        @JsonProperty("name") String name,
        @JsonProperty("full_name") String fullName,
        @JsonProperty("private") boolean isPrivate
) {}
