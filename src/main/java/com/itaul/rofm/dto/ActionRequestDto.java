package com.itaul.rofm.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ActionRequestDto {
    @JsonProperty("title")
    private String title;

    @JsonProperty("description")
    private String description;

    @JsonProperty("answers")
    private List<String> answers;

    @JsonProperty("correct_answer_index")
    private Integer correctAnswerIndex;
}
