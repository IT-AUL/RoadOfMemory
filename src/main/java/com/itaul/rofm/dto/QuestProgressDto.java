package com.itaul.rofm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Set;
import java.util.UUID;

@Data
public class QuestProgressDto {
    @JsonProperty("quest_id")
    private UUID questId;

    @JsonProperty("quest_title")
    private String questTitle;

    @JsonProperty("completed")
    private boolean completed;

    @JsonProperty("visited_locations")
    private Set<UUID> visitedLocations;

    @JsonProperty("total_locations")
    private int totalLocations;
}