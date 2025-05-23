package com.itaul.rofm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.itaul.rofm.model.enums.Language;
import com.itaul.rofm.model.enums.Type;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Setter
public class QuestResponseDto {
    @JsonProperty("id")
    private UUID id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("promo_url")
    private String promoUrl;

    @JsonProperty("audio_url")
    private String audioUrl;

    @JsonProperty("description")
    private String description;

    @JsonProperty("locations")
    private List<UUID> locations;

    @JsonProperty("language")
    private Language language;

    @JsonProperty("type")
    private Type type;

    // Draft fields

    @JsonProperty("title_draft")
    private String titleDraft;

    @JsonProperty("promo_url_draft")
    private String promoUrlDraft;

    @JsonProperty("audio_url_draft")
    private String audioUrlDraft;

    @JsonProperty("description_draft")
    private String descriptionDraft;

    @JsonProperty("locations_draft")
    private List<UUID> locationsDraft;

    @JsonProperty("language_draft")
    private Language languageDraft;

    @JsonProperty("type_draft")
    private Type typeDraft;

    @JsonProperty("rating")
    private float rating = 0.0f;

    @JsonProperty("rating_count")
    private int ratingCount = 0;

    @JsonProperty("author")
    private String author;
}
