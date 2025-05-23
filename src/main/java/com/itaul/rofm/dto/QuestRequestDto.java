package com.itaul.rofm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.itaul.rofm.model.enums.Language;
import com.itaul.rofm.model.enums.Type;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;


import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class QuestRequestDto {

    @JsonProperty("title")
    @Size(min = 5, max = 30)
    private String title;

    @JsonProperty("description")
    @Size(min = 5, max = 200)
    private String description;

    @JsonProperty("locations")
    private List<UUID> locations;

    @JsonProperty("language")
    private Language language;

    @JsonProperty("type")
    private Type type;
}
