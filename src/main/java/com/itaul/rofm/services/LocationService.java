package com.itaul.rofm.services;

import com.itaul.rofm.dto.ActionRequestDto;
import com.itaul.rofm.dto.ActionResponseDto;
import com.itaul.rofm.dto.LocationRequestDto;
import com.itaul.rofm.dto.LocationResponseDto;
import com.itaul.rofm.exception.InternalServerException;
import com.itaul.rofm.exception.NotFoundException;
import com.itaul.rofm.model.ActionEntity;
import com.itaul.rofm.model.Location;
import com.itaul.rofm.repository.ActionRepository;
import com.itaul.rofm.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service
@Slf4j
public class LocationService {

    private final LocationRepository locationRepository;
    private final S3Service s3Service;
    private final FileProcessingService fileProcessingService;
    private final SimpMessagingTemplate template;
    private final ActionRepository actionRepository;

    public List<LocationResponseDto> getUserLocations(Long userId) {
        try {
            var locations = locationRepository.findByUser_Id(userId);

            return locations.stream()
                    .map(location -> convertToResponseDto(location, true))
                    .toList();
        } catch (
                Exception e) {
            throw new InternalServerException("Failed to get user locations", e);
        }
    }

    public LocationResponseDto save(Location location) {
        try {
            return convertToResponseDto(locationRepository.save(location), true);
        } catch (Exception e) {
            throw new InternalServerException("Failed to save location", e);
        }
    }

    public LocationResponseDto convertToResponseDto(Location location, boolean draft) {
        var dto = new LocationResponseDto();
        dto.setId(location.getId());
        dto.setTitle(location.getTitle());
        dto.setLatitude(location.getLatitude());
        dto.setLongitude(location.getLongitude());
        dto.setPromoUrl(s3Service.generatePresignedUrl(location.getPromoUrl()));
        dto.setAudioUrl(s3Service.generatePresignedUrl(location.getAudioUrl()));
        dto.setDescription(location.getDescription());
        dto.setLanguage(location.getLanguage());
        dto.setMediaUrls(location.getMediaUrls().stream().map(s3Service::generatePresignedUrl).toList());
        dto.setAudioTimestamps(location.getAudioTimestamps());
        dto.setActions(
                location.getActions().stream()
                        .map(this::convertToDto)
                        .toList()
        );
        if (draft) {
            dto.setTitleDraft(location.getTitleDraft());
            dto.setLatitudeDraft(location.getLatitudeDraft());
            dto.setLongitudeDraft(location.getLongitudeDraft());
            dto.setPromoUrlDraft(s3Service.generatePresignedUrl(location.getPromoUrlDraft()));
            dto.setAudioUrlDraft(s3Service.generatePresignedUrl(location.getAudioUrlDraft()));
            dto.setDescriptionDraft(location.getDescriptionDraft());
            dto.setLanguageDraft(location.getLanguageDraft());
            dto.setMediaUrlsDraft(location.getMediaUrlsDraft().stream().map(s3Service::generatePresignedUrl).toList());
            dto.setAudioTimestampsDraft(location.getAudioTimestampsDraft());
            dto.setActionsDraft(
                    location.getActionsDraft().stream()
                            .map(this::convertToDto)
                            .toList()
            );
        }
        return dto;
    }

    public boolean checkForPublish(Location location) {
        return location.getTitleDraft() != null &&
                location.getLatitudeDraft() != null &&
                location.getLongitudeDraft() != null &&
                location.getPromoUrlDraft() != null &&
                location.getDescriptionDraft() != null &&
                !location.getMediaUrlsDraft().isEmpty() &&
                location.getLanguageDraft() != null;

    }

    public void publish(Location location) {
        try {
            if (!checkForPublish(location))
                return;

            s3Service.delete(location.getPromoUrl());

            s3Service.delete(location.getAudioUrl());
            location.getMediaUrls().forEach(s3Service::delete);

            location.setTitle(location.getTitleDraft());
            location.setLatitude(location.getLatitudeDraft());
            location.setLongitude(location.getLongitudeDraft());
            location.setDescription(location.getDescriptionDraft());
            location.setLanguage(location.getLanguageDraft());

            location.setPromoUrl(s3Service.generatePath("location", location.getId(), "webp"));
            if (location.getAudioUrlDraft() != null)
                location.setAudioUrl(s3Service.generatePath("location", location.getId(), "aac"));

            location.getMediaUrls().clear();
            for (var mediaUrl : location.getMediaUrlsDraft()) {
                var array = mediaUrl.split("\\.");
                var extension = array[array.length - 1];
                location.getMediaUrls().add(s3Service.generatePath("location", location.getId(), extension));
            }

            s3Service.copy(location.getPromoUrlDraft(), location.getPromoUrl());
            s3Service.copy(location.getAudioUrlDraft(), location.getAudioUrl());
            for (int i = 0; i < location.getMediaUrls().size(); i++) {
                s3Service.copy(location.getMediaUrlsDraft().get(i), location.getMediaUrls().get(i));
            }

            location.setPublished(true);
            save(location);
        } catch (Exception e) {
            throw new InternalServerException("Error while publishing location", e);
        }
    }

    public Optional<Location> findById(UUID id) {
        try {
            return locationRepository.findById(id);
        } catch (Exception e) {
            throw new InternalServerException("", e);
        }
    }

    public boolean notHasAccess(Long userId, Location location) {
        return !location.getUser().getId().equals(userId);
    }

    public void update(
            LocationRequestDto dto,
            MultipartFile promo,
            MultipartFile audio,
            List<MultipartFile> media,
            Location location) {
        try {
            s3Service.delete(location.getPromoUrlDraft());
            s3Service.delete(location.getAudioUrlDraft());
            for (var mediaUrl : location.getMediaUrlsDraft()) {
                s3Service.delete(mediaUrl);
            }
            location.setPromoUrlDraft(null);
            location.setAudioUrlDraft(null);
            location.getMediaUrlsDraft().clear();

            location.setTitleDraft(dto.getTitle());
            location.setLatitudeDraft(dto.getLatitude());
            location.setLongitudeDraft(dto.getLongitude());
            location.setDescriptionDraft(dto.getDescription());
            location.setLanguageDraft(dto.getLanguage());

            if (promo != null && !promo.isEmpty()) {
                location.setPromoUrlDraft(s3Service.generatePath("location", location.getId(), "webp"));
                fileProcessingService.processAndUpload(promo, location.getPromoUrlDraft());
            }
            if (audio != null && !audio.isEmpty()) {
                location.setAudioUrlDraft(s3Service.generatePath("location", location.getId(), "aac"));
                fileProcessingService.processAndUpload(audio, location.getAudioUrlDraft());
            }
            if (media != null) {
                location.getMediaUrlsDraft().clear();
                for (var mediaUrl : media) {
                    var array = Objects.requireNonNull(mediaUrl.getOriginalFilename()).split("\\.");
                    var extension = array[array.length - 1];
                    var path = s3Service.generatePath("location", location.getId(), extension);
                    location.getMediaUrlsDraft().add(path);

                    fileProcessingService.processAndUpload(mediaUrl, path);
                }
            }
            System.out.println("save");
            var loc = save(location);
        } catch (Exception e) {
            throw new InternalServerException("Error while updating location", e);
        }
    }

    public void delete(Location location) {
        try {
            s3Service.delete(location.getPromoUrlDraft());
            s3Service.delete(location.getPromoUrl());
            s3Service.delete(location.getAudioUrlDraft());
            s3Service.delete(location.getAudioUrl());
            location.getMediaUrlsDraft().forEach(s3Service::delete);
            location.getMediaUrls().forEach(s3Service::delete);
            locationRepository.delete(location);
        } catch (Exception e) {
            throw new InternalServerException("Error while deleting location", e);
        }
    }

    public LocationResponseDto getDto(Location location, boolean draft) {
        try {
            return convertToResponseDto(location, draft);
        } catch (Exception e) {
            throw new InternalServerException("Error while deleting location", e);
        }
    }

    @Transactional
    public void prepareAndUpdateAsync(
            LocationRequestDto dto,
            Location location,
            MultipartFile promo,
            MultipartFile audio,
            List<MultipartFile> media) {

        Hibernate.initialize(location.getMediaUrlsDraft());
        Hibernate.initialize(location.getMediaUrls());

        updateAsync(dto, location, promo, audio, media);
    }

    @Async
    public void updateAsync(
            LocationRequestDto dto,
            Location location,
            MultipartFile promo,
            MultipartFile audio,
            List<MultipartFile> media) {

        try {
            log.info("Start async update of location: {}", location.getId());

            location.setTitleDraft(dto.getTitle());
            location.setLatitudeDraft(dto.getLatitude());
            location.setLongitudeDraft(dto.getLongitude());
            location.setDescriptionDraft(dto.getDescription());
            location.setLanguageDraft(dto.getLanguage());

            s3Service.delete(location.getPromoUrlDraft());
            s3Service.delete(location.getAudioUrlDraft());

            if (location.getMediaUrlsDraft() != null) {
                for (var url : location.getMediaUrlsDraft()) {
                    s3Service.delete(url);
                }
            }

            location.setPromoUrlDraft(null);
            location.setAudioUrlDraft(null);
            location.getMediaUrlsDraft().clear();

            locationRepository.save(location);
            log.info("Draft fields and old files cleared for location: {}", location.getId());

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            // Обработка promo
            if (promo != null && !promo.isEmpty()) {
                String promoUrl = s3Service.generatePath("location", location.getId(), "webp");
                location.setPromoUrlDraft(promoUrl);
                futures.add(fileProcessingService.processAndUpload(promo, promoUrl));
            }

            // Обработка audio
            if (audio != null && !audio.isEmpty()) {
                String audioUrl = s3Service.generatePath("location", location.getId(), "aac");
                location.setAudioUrlDraft(audioUrl);
                futures.add(fileProcessingService.processAndUpload(audio, audioUrl));
            }

            // Обработка media
            if (media != null && !media.isEmpty()) {
                List<String> mediaUrls = new ArrayList<>();
                for (var mediaFile : media) {
                    var array = Objects.requireNonNull(mediaFile.getOriginalFilename()).split("\\.");
                    var extension = array[array.length - 1];
                    var path = s3Service.generatePath("location", location.getId(), extension);
                    mediaUrls.add(path);
                    futures.add(fileProcessingService.processAndUpload(mediaFile, path));
                }
                location.getMediaUrlsDraft().addAll(mediaUrls);
            }

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        var loc = save(location);
                        template.convertAndSend("/topic/all-messages", Map.of("loc", loc));
                        log.info("Location update completed: {}", location.getId());
                    });

        } catch (Exception e) {
            throw new InternalServerException("Failed to update location", e);
        }
    }

    private ActionResponseDto convertToDto(ActionEntity entity) {
        return new ActionResponseDto(entity.getId(), entity.getTitle(), entity.getDescription(),
                entity.getAnswers(), entity.getCorrectAnswerIndex());
    }

    public List<ActionResponseDto> getActionsByLocationId(UUID locationId) {
        return actionRepository.findByLocation_Id(locationId)
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    public ActionResponseDto createAction(UUID locationId, ActionRequestDto dto) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new NotFoundException("Location not found"));

        ActionEntity action = new ActionEntity();
        action.setTitle(dto.getTitle());
        action.setDescription(dto.getDescription());
        action.setAnswers(dto.getAnswers());
        action.setCorrectAnswerIndex(dto.getCorrectAnswerIndex());
        action.setLocation(location);

        return convertToDto(actionRepository.save(action));
    }

    public ActionResponseDto updateAction(UUID actionId, ActionRequestDto dto) {
        Optional<ActionEntity> location = actionRepository.findById(actionId);

        if (location.isEmpty()) {
            throw new NotFoundException("Action not found");
        }
        ActionEntity action = location.get();
        action.setTitle(dto.getTitle());
        action.setDescription(dto.getDescription());
        action.setAnswers(dto.getAnswers());
        action.setCorrectAnswerIndex(dto.getCorrectAnswerIndex());
        return convertToDto(actionRepository.save(action));
    }

    public void deleteAction(UUID actionId) {
        ActionEntity action = actionRepository.findById(actionId)
                .orElseThrow(() -> new NotFoundException("Action not found"));
        actionRepository.delete(action);
    }
}