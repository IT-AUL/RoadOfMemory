package com.itaul.rofm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.itaul.rofm.model.enums.Language;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class LocationRequestDto {
    @JsonProperty("title")
    @Size(min = 5, max = 30)
    private String title;

    @JsonProperty("description")
    @Size(min = 50, max = 200)
    private String description;

    @JsonProperty("language")
    private Language language;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;
}
