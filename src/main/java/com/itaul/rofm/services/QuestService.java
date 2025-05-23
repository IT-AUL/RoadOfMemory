package com.itaul.rofm.services;

import com.itaul.rofm.dto.QuestRequestDto;
import com.itaul.rofm.dto.QuestResponseDto;
import com.itaul.rofm.exception.BadRequestException;
import com.itaul.rofm.exception.InternalServerException;
import com.itaul.rofm.exception.NotFoundException;
import com.itaul.rofm.model.Location;
import com.itaul.rofm.model.Quest;
import com.itaul.rofm.repository.LocationRepository;
import com.itaul.rofm.repository.QuestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service
@Slf4j
public class QuestService {

    private final QuestRepository questRepository;
    private final LocationRepository locationRepository;
    private final S3Service s3Service;
    private final FileProcessingService fileProcessingService;

    public List<QuestResponseDto> getQuests(int offset, int limit) {
        try {
            limit = Math.min(20, Math.abs(limit));
            offset = Math.max(0, Math.abs(offset));
            var quests = questRepository.findByPublished(true, PageRequest.of(offset, limit));

            return quests.stream()
                    .map(quest -> questToDto(quest, false))
                    .toList();

        } catch (
                Exception e) {
            throw new InternalServerException("Failed to get quests list", e);
        }
    }

    public List<QuestResponseDto> getUserQuests(Long userId) {
        try {
            var quests = questRepository.findByUser_Id(userId);

            return quests.stream()
                    .map(quest -> questToDto(quest, true))
                    .toList();


        } catch (
                Exception e) {
            throw new InternalServerException("Failed to get user quests list", e);

        }
    }

    public QuestResponseDto save(Quest quest) {
        try {
            return questToDto(questRepository.save(quest), true);
        } catch (Exception e) {
            throw new InternalServerException("Failed to save quest", e);
        }
    }

    public Optional<Quest> findById(UUID id) {
        try {
            return questRepository.findById(id);
        } catch (Exception e) {
            throw new InternalServerException("Failed to find quest", e);
        }
    }

    public boolean notHasAccess(Long userId, Quest quest) {
        return !quest.getUser().getId().equals(userId);
    }

    @Transactional
    public void prepareAndUpdateAsync(
            QuestRequestDto questRequestDTO,
            Quest quest,
            MultipartFile promo,
            MultipartFile audio) {

        Hibernate.initialize(quest.getLocationsDraft());
        Hibernate.initialize(quest.getLocations());

        updateAsync(questRequestDTO, quest, promo, audio);
    }

    @Async
    public void updateAsync(QuestRequestDto questRequestDTO, Quest quest, MultipartFile promo, MultipartFile audio) {
        try {
            var locations = new ArrayList<Location>();
            if (questRequestDTO.getLocations() != null) {
                for (var locId : questRequestDTO.getLocations()) {
                    var loc = locationRepository.findById(locId);
                    if (loc.isEmpty())
                        throw new NotFoundException("Failed to find location with id: " + locId);
                    locations.add(loc.get());
                }
            }

            log.info("Add new locations");

            quest.setTitleDraft(questRequestDTO.getTitle());
            quest.setDescriptionDraft(questRequestDTO.getDescription());
            quest.setLanguageDraft(questRequestDTO.getLanguage());
            quest.setTypeDraft(questRequestDTO.getType());
            quest.getLocationsDraft().clear();
            quest.getLocationsDraft().addAll(locations);
            questRepository.save(quest);

            s3Service.delete(quest.getPromoUrlDraft());
            s3Service.delete(quest.getAudioUrlDraft());
            quest.setPromoUrlDraft(null);
            quest.setAudioUrlDraft(null);
            questRepository.save(quest);
            log.info("saved");
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            if (promo != null && !promo.isEmpty()) {
                String promoUrl = s3Service.generatePath("quest", quest.getId(), "webp");
                quest.setPromoUrlDraft(promoUrl);
                log.info("promo");
                futures.add(fileProcessingService.processAndUpload(promo, promoUrl));
            }

            if (audio != null && !audio.isEmpty()) {
                String audioUrl = s3Service.generatePath("quest", quest.getId(), "aac");
                quest.setAudioUrlDraft(audioUrl);
                futures.add(fileProcessingService.processAndUpload(audio, audioUrl));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        save(quest);
                        System.out.println("Quest update completed.");
                    });

        } catch (Exception e) {
            log.info(e.getMessage());
            throw new InternalServerException("Failed to update quest", e);
        }
    }

    @Async
    public CompletableFuture<QuestResponseDto> update(QuestRequestDto questRequestDTO, Quest quest, MultipartFile promo, MultipartFile audio) {
        try {
            var locations = new ArrayList<Location>();
            if (questRequestDTO.getLocations() != null) {
                for (var locId : questRequestDTO.getLocations()) {
                    var loc = locationRepository.findById(locId);
                    if (loc.isEmpty())
                        throw new NotFoundException("Failed to find location with id: " + locId);
                    locations.add(loc.get());
                }
            }
            quest.setTitleDraft(questRequestDTO.getTitle());
            quest.setDescriptionDraft(questRequestDTO.getDescription());
            quest.setLanguageDraft(questRequestDTO.getLanguage());
            quest.setTypeDraft(questRequestDTO.getType());
            quest.getLocationsDraft().clear();
            quest.getLocationsDraft().addAll(locations);
            System.out.println("Fields updated");

            s3Service.delete(quest.getPromoUrlDraft());
            s3Service.delete(quest.getAudioUrlDraft());
            quest.setPromoUrlDraft(null);
            quest.setAudioUrlDraft(null);

            questRepository.save(quest);

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            if (promo != null && !promo.isEmpty()) {
                quest.setPromoUrlDraft(s3Service.generatePath("quest", quest.getId(), "webp"));
                futures.add(fileProcessingService.processAndUpload(promo, quest.getPromoUrlDraft()));
            }
            if (audio != null && !audio.isEmpty()) {
                quest.setAudioUrlDraft(s3Service.generatePath("quest", quest.getId(), "aac"));
                futures.add(fileProcessingService.processAndUpload(audio, quest.getAudioUrlDraft()));
            }
            System.out.println("Files processed");

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            questRepository.save(quest);
            System.out.println("Updated");
        } catch (Exception e) {
            throw new InternalServerException("Failed to update quest", e);
        }
        return null;
    }


    private QuestResponseDto questToDto(Quest quest, boolean draft) {
        var dto = new QuestResponseDto();
        dto.setId(quest.getId());
        dto.setTitle(quest.getTitle());
        dto.setPromoUrl(s3Service.generatePresignedUrl(quest.getPromoUrl()));
        dto.setAudioUrl(s3Service.generatePresignedUrl(quest.getAudioUrl()));
        dto.setDescription(quest.getDescription());
        dto.setLocations(quest.getLocations().stream().map(Location::getId).toList());
        dto.setLanguage(quest.getLanguage());
        dto.setType(quest.getType());

        dto.setRating(quest.getRating());
        dto.setRatingCount(quest.getRatingCount());
        dto.setAuthor(quest.getUser().getUsername());

        if (draft) {
            dto.setTitleDraft(quest.getTitleDraft());
            dto.setPromoUrlDraft(s3Service.generatePresignedUrl(quest.getPromoUrlDraft()));
            dto.setAudioUrlDraft(s3Service.generatePresignedUrl(quest.getAudioUrlDraft()));
            dto.setDescriptionDraft(quest.getDescriptionDraft());
            dto.setLocationsDraft(quest.getLocationsDraft().stream().map(Location::getId).toList());
            dto.setLanguageDraft(quest.getLanguageDraft());
            dto.setTypeDraft(quest.getTypeDraft());
        }

        return dto;
    }

    public boolean checkForPublish(Quest quest) {
        return quest.getTitleDraft() != null &&
                quest.getDescriptionDraft() != null &&
                quest.getLanguageDraft() != null &&
                quest.getTypeDraft() != null &&
                quest.getPromoUrlDraft() != null &&
                quest.getLocationsDraft().stream().allMatch(Location::isPublished);
    }

    public void publicQuest(Quest quest) {
        if (!checkForPublish(quest))
            throw new BadRequestException("Not all fields are filled");
        try {
            s3Service.delete(quest.getPromoUrl());
            s3Service.delete(quest.getAudioUrl());
            quest.setTitle(quest.getTitleDraft());
            quest.setDescription(quest.getDescriptionDraft());
            quest.setLanguage(quest.getLanguageDraft());
            quest.setType(quest.getTypeDraft());
            quest.getLocations().clear();
            quest.getLocations().addAll(quest.getLocationsDraft());
            quest.setPromoUrl(s3Service.generatePath("quest", quest.getId(), "webp"));
            if (quest.getAudioUrlDraft() != null)
                quest.setAudioUrl(s3Service.generatePath("quest", quest.getId(), "aac"));
            s3Service.copy(quest.getAudioUrlDraft(), quest.getAudioUrl());
            s3Service.copy(quest.getPromoUrlDraft(), quest.getPromoUrl());
            quest.setPublished(true);
            save(quest);
        } catch (Exception e) {
            throw new InternalServerException("Failed to publish quest", e);
        }
    }

    public void delete(Quest quest) {
        try {
            s3Service.delete(quest.getPromoUrl());
            s3Service.delete(quest.getAudioUrl());
            s3Service.delete(quest.getPromoUrlDraft());
            s3Service.delete(quest.getAudioUrlDraft());
            questRepository.delete(quest);
        } catch (Exception e) {
            throw new InternalServerException("Failed to delete quest", e);
        }
    }

    public QuestResponseDto getQuests(Quest quest, boolean draft) {
        try {
            return questToDto(quest, draft);
        } catch (Exception e) {
            throw new InternalServerException("Failed to get quest", e);
        }
    }

    public List<Quest> findPublished() {
        try {
            return questRepository.findByPublished(true);
        } catch (Exception e) {
            throw new InternalServerException("Failed to get published quests", e);
        }
    }

    public boolean existsById(UUID id) {
        try {
            return questRepository.existsById(id);
        } catch (Exception e) {
            throw new InternalServerException("Failed to check if quest exists", e);
        }
    }
}
