package com.itaul.rofm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UserAuthDto {
    @NotNull(message = "Long id is required")
    @JsonProperty("id")
    private Long id;

    @NotNull(message = "First name is required")
    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("username")
    private String username;

    @JsonProperty("photo_url")
    private String photoURL;

    @JsonProperty("auth_date")
    @NotNull(message = "Auth date is required")
    private Integer authDate;

    @NotNull(message = "Hash is required")
    @JsonProperty("hash")
    private String hash;
}
