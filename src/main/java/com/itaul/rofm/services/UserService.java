package com.itaul.rofm.services;

import com.itaul.rofm.dto.UserAuthDto;
import com.itaul.rofm.exception.InternalServerException;
import com.itaul.rofm.model.Location;
import com.itaul.rofm.model.Quest;
import com.itaul.rofm.model.User;
import com.itaul.rofm.model.UserProgress;
import com.itaul.rofm.repository.UserRepository;
import com.itaul.rofm.util.TelegramAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Value("${app.telegram.botToken}")
    private String botToken;

    @Value("${app.telegram.expirationIn}")
    private int expirationIn;
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public Optional<User> findById(Long id) {
        try {
            return userRepository.findById(id);
        } catch (Exception e) {
            throw new InternalServerException("Failed to find user", e);
        }
    }

    public User addUser(UserAuthDto userAuthDto) {
        try {
            var user = new User(userAuthDto.getId(), userAuthDto.getFirstName(), userAuthDto.getLastName());
            return userRepository.save(user);
        } catch (Exception e) {
            throw new InternalServerException("Failed to create user", e);
        }
    }

//    protected void saveProtoUrl(String photoUrl) {
//        URL url = null;
//        try {
//            url = new URL(photoUrl);
//        } catch (MalformedURLException e) {
//            throw new RuntimeException(e);
//        }
//        HttpURLConnection httpURLConnection = null;
//        try {
//            httpURLConnection = (HttpURLConnection) url.openConnection();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        httpURLConnection.setInstanceFollowRedirects(true);
//
//        try (BufferedInputStream in = new BufferedInputStream(httpURLConnection.getInputStream());
//             FileOutputStream fileOutputStream = new FileOutputStream(File.createTempFile("downloadedFile", null))) {
//            byte dataBuffer[] = new byte[1024];
//            int bytesRead;
//            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
//                fileOutputStream.write(dataBuffer, 0, bytesRead);
//            }
//            s3Service.processAndUploadFile();
//            return Paths.get(fileOutputStream.getFD().toString());
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        } finally {
//            httpURLConnection.disconnect();
//        }
//        s3Service.processAndUploadFile();
//    }

    public boolean validateUser(UserAuthDto userAuthDto) {
        try {
            Map<String, String> jsonData = new HashMap<>();
            jsonData.put("id", userAuthDto.getId().toString());
            jsonData.put("first_name", userAuthDto.getFirstName());
            if (userAuthDto.getLastName() != null)
                jsonData.put("last_name", userAuthDto.getLastName());
            if (userAuthDto.getUsername() != null)
                jsonData.put("username", userAuthDto.getUsername());
            if (userAuthDto.getPhotoURL() != null)
                jsonData.put("photo_url", userAuthDto.getPhotoURL());
            jsonData.put("auth_date", userAuthDto.getAuthDate().toString());
            jsonData.put("hash", userAuthDto.getHash());

            return TelegramAuth.checkTelegramAuthorization(botToken, expirationIn, jsonData);
        } catch (Exception e) {
            throw new InternalServerException("Failed to validate user", e);
        }
    }

    public void visitLocation(User user, UUID questId, UUID locationId) {
        UserProgress progress = new UserProgress(user, questId, locationId);
        user.getProgress().add(progress);
        try {
            userRepository.save(user);
        } catch (Exception e) {
            throw new InternalServerException("Failed to save user progress", e);
        }
    }

    public boolean hasVisitedLocation(User user, UUID questId, UUID locationId) {
        return user.getProgress().stream()
                .anyMatch(progress -> progress.getQuestId().equals(questId)
                        && progress.getLocationId().equals(locationId));
    }

    public boolean isQuestCompleted(User user, Quest quest) {
        List<Location> questLocations = quest.getLocations();
        if (questLocations == null || questLocations.isEmpty()) {
            return false;
        }

        for (Location location : questLocations) {
            if (!hasVisitedLocation(user, quest.getId(), location.getId())) {
                return false;
            }
        }

        return true;
    }

    public void resetQuestProgress(User user, UUID questId) {
        user.setProgress(
                user.getProgress().stream()
                        .filter(progress -> !progress.getQuestId().equals(questId))
                        .collect(Collectors.toList())
        );

        try {
            userRepository.save(user);
        } catch (Exception e) {
            throw new InternalServerException("Ошибка при сбросе прогресса", e);
        }
    }
}

