package com.itaul.rofm.controller;

import com.itaul.rofm.dto.QuestRequestDto;
import com.itaul.rofm.dto.QuestResponseDto;
import com.itaul.rofm.exception.NotFoundException;
import com.itaul.rofm.model.Quest;
import com.itaul.rofm.model.enums.Language;
import com.itaul.rofm.model.enums.Type;
import com.itaul.rofm.services.JwtService;
import com.itaul.rofm.services.QuestService;
import com.itaul.rofm.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class QuestController {

    private final QuestService questService;
    private final JwtService jwtService;
    private final UserService userService;

    @PostMapping("/quests")
    public ResponseEntity<QuestResponseDto> createQuest(
            @RequestHeader("Authorization") String token,
            @RequestParam String title,
            @RequestParam Language language,
            @RequestParam Type type) {
        var user = userService.findById(jwtService.getUserId(token)).get();
        return ResponseEntity.ok(questService.save(new Quest(title, language, type, user)));
    }

    @GetMapping("/quests")
    public ResponseEntity<List<QuestResponseDto>> getQuests(
            @RequestParam(required = false, defaultValue = "0") int offset,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        var quests = questService.getQuests(offset, limit);
        return ResponseEntity.ok(quests);
    }

    @GetMapping("/quests/{id}")
    public ResponseEntity<QuestResponseDto> getQuest(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean draft) {
        var userId = jwtService.getUserId(token);
        var quest = questService.findById(id);
        if (quest.isEmpty())
            throw new NotFoundException("Quest not found");
        if (!quest.get().isPublished() && questService.notHasAccess(userId, quest.get()))
            throw new NotFoundException("Quest not published or you don't have access to it.");
        if (questService.notHasAccess(userId, quest.get()) && draft)
            throw new NotFoundException("You're trying to get draft version of not your quest");
        return ResponseEntity.ok(questService.getQuests(quest.get(), draft));
    }

    @GetMapping("/quests/user")
    public ResponseEntity<List<QuestResponseDto>> getUserQuests(
            @RequestHeader("Authorization") String token) {
        var userId = jwtService.getUserId(token);
        var quests = questService.getUserQuests(userId);
        return ResponseEntity.ok(quests);
    }

    @PutMapping(path = "/quests/{id}")
    public ResponseEntity<String> updateQuest(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID id,
            @Valid @RequestPart QuestRequestDto data,
            @RequestPart(required = false) MultipartFile promo,
            @RequestPart(required = false) MultipartFile audio) {

        var userId = jwtService.getUserId(token);
        var questOptional = questService.findById(id);
        if (questOptional.isEmpty() || questService.notHasAccess(userId, questOptional.get()))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

        questService.prepareAndUpdateAsync(data, questOptional.get(), promo, audio);

        return ResponseEntity.ok("Received");
    }


    @DeleteMapping("quests/{id}")
    public ResponseEntity<Boolean> deleteQuest(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID id) {
        var userId = jwtService.getUserId(token);
        var quest = questService.findById(id);
        if (quest.isEmpty() || questService.notHasAccess(userId, quest.get()))
            throw new NotFoundException("Quest not published or you don't have access to it.");
        else {
            questService.delete(quest.get());
            return ResponseEntity.ok(true);
        }
    }

    @PostMapping("quests/{id}/publish")
    public ResponseEntity<Boolean> publishQuest(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID id) {
        var userId = jwtService.getUserId(token);
        var quest = questService.findById(id);
        if (quest.isEmpty() || questService.notHasAccess(userId, quest.get()))
            throw new NotFoundException("Not Found");
        questService.publicQuest(quest.get());
        return ResponseEntity.ok(true);
    }
}
