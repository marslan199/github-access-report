package com.example.githubaccess.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CollaboratorResponse(
        @JsonProperty("login") String login,
        @JsonProperty("id") long id
) {}
