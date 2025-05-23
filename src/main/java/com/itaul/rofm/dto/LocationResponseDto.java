package com.itaul.rofm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.itaul.rofm.model.enums.Language;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Setter
public class LocationResponseDto {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("latitude")
    private Double latitude;

    @JsonProperty("longitude")
    private Double longitude;

    @JsonProperty("promo_url")
    private String promoUrl;

    @JsonProperty("audio_url")
    private String audioUrl;

    @JsonProperty("media_urls")
    private List<String> mediaUrls;

    @JsonProperty("description")
    private String description;

    @JsonProperty("language")
    private Language language;

    @JsonProperty("audio_timestamps")
    private List<Double> audioTimestamps;

    @JsonProperty("actions")
    private List<ActionResponseDto> actions;

    // Draft fields

    @JsonProperty("title_draft")
    private String titleDraft;

    @JsonProperty("latitude_draft")
    private Double latitudeDraft;

    @JsonProperty("longitude_draft")
    private Double longitudeDraft;

    @JsonProperty("promo_url_draft")
    private String promoUrlDraft;

    @JsonProperty("audio_url_draft")
    private String audioUrlDraft;

    @JsonProperty("media_urls_draft")
    private List<String> mediaUrlsDraft;

    @JsonProperty("description_draft")
    private String descriptionDraft;

    @JsonProperty("language_draft")
    private Language languageDraft;

    @JsonProperty("audio_timestamps_draft")
    private List<Double> audioTimestampsDraft;

    @JsonProperty("actions_draft")
    private List<ActionResponseDto> actionsDraft;
}
