package com.itaul.rofm.controller;

import com.itaul.rofm.dto.RefreshTokenDto;
import com.itaul.rofm.dto.UserAuthDto;
import com.itaul.rofm.exception.BadRequestException;
import com.itaul.rofm.exception.NotFoundException;
import com.itaul.rofm.model.Location;
import com.itaul.rofm.model.Quest;
import com.itaul.rofm.model.User;
import com.itaul.rofm.services.JwtService;
import com.itaul.rofm.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/auth")
    public ResponseEntity<Map<String, String>> createUser(
            @Valid @RequestBody UserAuthDto userAuthDto) {
        var valid = userService.validateUser(userAuthDto);
        if (!valid)
            throw new BadRequestException("User data is not valid");
        var user = userService.findById(userAuthDto.getId());
        if (user.isEmpty())
            user = Optional.of(userService.addUser(userAuthDto));
        System.out.println(userAuthDto.getUsername());
        var tokens = jwtService.generateJwt(user.get().getId());
        return ResponseEntity.ok(tokens);

    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(
            @RequestBody RefreshTokenDto refreshTokenDto) {
        var refreshToken = jwtService.refreshJwt(refreshTokenDto.refreshToken());
        return ResponseEntity.ok(refreshToken);
    }

    @PostMapping("/progress/quests/{questId}/locations/{locationId}")
    public ResponseEntity<Map<String, String>> visitLocation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID questId,
            @PathVariable UUID locationId) {

        User user = userService.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Quest quest = questService.findById(questId)
                .orElseThrow(() -> new NotFoundException("Квест не найден"));

        Location location = locationService.findById(locationId)
                .orElseThrow(() -> new NotFoundException("Локация не найдена"));

        // Проверяем, что локация принадлежит квесту
        boolean locationBelongsToQuest = quest.getLocations().stream()
                .anyMatch(loc -> loc.getId().equals(locationId));

        if (!locationBelongsToQuest) {
            throw new BadRequestException("Локация не принадлежит указанному квесту");
        }

        // Проверяем, не была ли локация уже посещена
        if (userService.hasVisitedLocation(user, questId, locationId)) {
            return ResponseEntity.ok(Map.of("status", "already_visited"));
        }

        userService.visitLocation(user, questId, locationId);

        boolean isQuestCompleted = userService.isQuestCompleted(user, quest);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "quest_completed", String.valueOf(isQuestCompleted)
        ));
    }

    @GetMapping("/progress/quests")
    public ResponseEntity<List<QuestProgressDto>> getAllQuestsProgress(
            @AuthenticationPrincipal UserPrincipal principal) {

        User user = userService.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        List<Quest> allQuests = questService.findPublished();

        List<QuestProgressDto> progressList = allQuests.stream()
                .map(quest -> {
                    QuestProgressDto dto = new QuestProgressDto();
                    dto.setQuestId(quest.getId());
                    dto.setQuestTitle(quest.getTitle());
                    dto.setCompleted(userService.isQuestCompleted(user, quest));

                    Set<UUID> visitedLocationsIds = quest.getLocations().stream()
                            .filter(location -> userService.hasVisitedLocation(user, quest.getId(), location.getId()))
                            .map(Location::getId)
                            .collect(Collectors.toSet());

                    dto.setVisitedLocations(visitedLocationsIds);
                    dto.setTotalLocations(quest.getLocations().size());

                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(progressList);
    }

    @GetMapping("/progress/quests/{questId}")
    public ResponseEntity<QuestProgressDto> getQuestProgress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID questId) {

        User user = userService.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        Quest quest = questService.findById(questId)
                .orElseThrow(() -> new NotFoundException("Квест не найден"));

        QuestProgressDto dto = new QuestProgressDto();
        dto.setQuestId(quest.getId());
        dto.setQuestTitle(quest.getTitle());
        dto.setCompleted(userService.isQuestCompleted(user, quest));

        Set<UUID> visitedLocationsIds = quest.getLocations().stream()
                .filter(location -> userService.hasVisitedLocation(user, quest.getId(), location.getId()))
                .map(Location::getId)
                .collect(Collectors.toSet());

        dto.setVisitedLocations(visitedLocationsIds);
        dto.setTotalLocations(quest.getLocations().size());

        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/progress/quests/{questId}")
    public ResponseEntity<Map<String, String>> resetQuestProgress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID questId) {

        User user = userService.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));

        // Проверяем существование квеста
        if (!questService.existsById(questId)) {
            throw new NotFoundException("Квест не найден");
        }

        // Метод, который надо добавить в UserService
        userService.resetQuestProgress(user, questId);

        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
