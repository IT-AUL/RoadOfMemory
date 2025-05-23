package com.itaul.rofm.controller;

import com.itaul.rofm.dto.LocationRequestDto;
import com.itaul.rofm.dto.LocationResponseDto;
import com.itaul.rofm.exception.NotFoundException;
import com.itaul.rofm.model.Location;
import com.itaul.rofm.model.enums.Language;
import com.itaul.rofm.services.JwtService;
import com.itaul.rofm.services.LocationService;
import com.itaul.rofm.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class LocationController {

    private final JwtService jwtService;
    private final UserService userService;
    private final LocationService locationService;


    @PostMapping("/locations")
    public ResponseEntity<LocationResponseDto> createLocation(
            @RequestHeader("Authorization") String token,
            @RequestParam("title") String title,
            @RequestParam("language") Language language) {
        var user = userService.findById(jwtService.getUserId(token)).get();
        var location = new Location(user, title, language);
        return ResponseEntity.ok(locationService.save(location));
    }

    @PostMapping("/locations/{id}/publish")
    public ResponseEntity<Boolean> publishLocation(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID id) {
        var userID = jwtService.getUserId(token);
        var location = locationService.findById(id);

        if (location.isEmpty() || locationService.notHasAccess(userID, location.get())) {
            throw new NotFoundException("Not Found");
        }

        locationService.publish(location.get());
        return ResponseEntity.ok(true);
    }

    @PutMapping("/locations/{id}")
    public ResponseEntity<String> updateLocation(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID id,
            @Valid @RequestPart LocationRequestDto data,
            @RequestPart(required = false) MultipartFile promo,
            @RequestPart(required = false) MultipartFile audio,
            @RequestPart(required = false) List<MultipartFile> media) {

        var userId = jwtService.getUserId(token);
        var location = locationService.findById(id);
        if (location.isEmpty() || locationService.notHasAccess(userId, location.get()))
            throw new NotFoundException("Not Found");

        locationService.prepareAndUpdateAsync(data, location.get(), promo, audio, media);

        return ResponseEntity.accepted().body("Started to update location");
    }


    @DeleteMapping("/locations/{id}")
    public ResponseEntity<Boolean> deleteLocation(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID id) {
        var userId = jwtService.getUserId(token);
        var location = locationService.findById(id);
        if (location.isEmpty() || locationService.notHasAccess(userId, location.get()))
            throw new NotFoundException("Not Found");
        else
            locationService.delete(location.get());
        return ResponseEntity.ok(true);
    }

    @GetMapping("/locations/{id}")
    public ResponseEntity<LocationResponseDto> getLocation(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean draft) {
        var userId = jwtService.getUserId(token);
        var location = locationService.findById(id);
        if (location.isEmpty()) {
            throw new NotFoundException("Not Found");
        }
        if (!location.get().isPublished() && locationService.notHasAccess(userId, location.get())) {
            throw new NotFoundException("Not Found");
        }
        if (!locationService.notHasAccess(userId, location.get()) && draft) {
            throw new NotFoundException("Not Found");
        }
        return ResponseEntity.ok(locationService.getDto(location.get(), draft));
    }

    @GetMapping("/locations/user")
    public ResponseEntity<List<LocationResponseDto>> getUserLocations(
            @RequestHeader("Authorization") String token) {
        var userId = jwtService.getUserId(token);
        return ResponseEntity.ok(locationService.getUserLocations(userId));
    }
}