package com.itaul.rofm.services;

import com.itaul.rofm.exception.BadRequestException;
import com.itaul.rofm.exception.InternalServerException;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class FileProcessingService {

    private final S3Service s3UploadService;
    private final FFmpeg ffmpeg;
    private final FFprobe ffprobe;

    public FileProcessingService(S3Service s3UploadService,
                                 @Value("${app.ffmpeg.ffmpeg}") String ffmpegPath,
                                 @Value("${app.ffmpeg.ffprobe}") String ffprobePath) throws IOException {
        this.s3UploadService = s3UploadService;
        this.ffmpeg = new FFmpeg(ffmpegPath);
        this.ffprobe = new FFprobe(ffprobePath);
    }

    @Async
    public CompletableFuture<Void> processAndUpload(MultipartFile file, String key) {
        return CompletableFuture.runAsync(() -> {
            File converted = null;
            try {
                String ext = FilenameUtils.getExtension(file.getOriginalFilename()).toLowerCase();
                log.info("ext");
                // Используем оригинальные методы конвертации
                converted = switch (ext) {
                    case "jpeg", "jpg", "png", "webp" -> imageConverter(file);
                    case "mp4", "mov", "avi", "webm" -> videoConverter(file);
                    case "mp3", "ogg", "wav", "aac" -> audioConverter(file);
                    default -> throw new BadRequestException("Формат файла не поддерживается");
                };
                log.info("conv");
                s3UploadService.upload(converted, key);
            } catch (Exception e) {
                throw new InternalServerException("Ошибка обработки/загрузки", e);
            } finally {
                if (converted != null) converted.delete();
            }
        });
    }

    private File imageConverter(MultipartFile file) throws Exception {
        Path tempInputFile = Files.createTempFile("temp_image", "." + FilenameUtils.getExtension(file.getOriginalFilename()));
        Files.copy(file.getInputStream(), tempInputFile, StandardCopyOption.REPLACE_EXISTING);

        File outputFile = File.createTempFile("converted_", ".webp");
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(tempInputFile.toString())
                .overrideOutputFiles(true)
                .addOutput(outputFile.getAbsolutePath())
                .setFormat("webp")
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();
        Files.delete(tempInputFile);

        return outputFile;
    }

    private File videoConverter(MultipartFile file) throws Exception {
        Path tempDir = Paths.get("zero-kilometer");

        Files.createDirectories(tempDir);
        Path tempFile = Files.createTempFile(tempDir,
                "temp_video", "." + FilenameUtils.getExtension(file.getOriginalFilename()));
        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

        File outputFile = File.createTempFile("converted_", ".webm");

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(tempFile.toString())
                .overrideOutputFiles(true)
                .addOutput(outputFile.getAbsolutePath())
                .setFormat("webm")
                .setVideoResolution(640, 480)
                .setVideoCodec("libvpx")
                .setAudioCodec("libvorbis")
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();

        Files.delete(tempFile);

        return outputFile;
    }

    private File audioConverter(MultipartFile file) throws Exception {
        Path tempFile = Files.createTempFile(
                "zero-kilometer/temp_audio", "." + FilenameUtils.getExtension(file.getOriginalFilename()));
        Files.copy(file.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);

        File outputFile = File.createTempFile("converted_", ".aac");

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(tempFile.toString())
                .overrideOutputFiles(true)
                .addOutput(outputFile.getAbsolutePath())
                .setAudioCodec("aac")
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();

        Files.delete(tempFile);

        return outputFile;
    }
}