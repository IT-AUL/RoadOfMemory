package com.itaul.rofm.controller;

import com.itaul.rofm.dto.ActionRequestDto;
import com.itaul.rofm.dto.ActionResponseDto;
import com.itaul.rofm.services.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/actions")
@RequiredArgsConstructor
public class ActionController {

    private final LocationService locationService;

    @PostMapping("/{locationId}")
    public ResponseEntity<ActionResponseDto> createAction(
            @PathVariable UUID locationId,
            @RequestBody ActionRequestDto actionDto
    ) {
        ActionResponseDto createdAction = locationService.createAction(locationId, actionDto);
        return ResponseEntity.ok(createdAction);
    }

    @PutMapping("/{actionId}")
    public ResponseEntity<ActionResponseDto> updateAction(
            @RequestBody ActionRequestDto actionDto,
            @PathVariable UUID actionId
    ) {
        ActionResponseDto updatedAction = locationService.updateAction(actionId, actionDto);
        return ResponseEntity.ok(updatedAction);
    }

    @DeleteMapping("/{actionId}")
    public ResponseEntity<Void> deleteAction(
            @PathVariable UUID actionId
    ) {
        locationService.deleteAction(actionId);
        return ResponseEntity.noContent().build();
    }
}

