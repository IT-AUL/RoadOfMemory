package com.itaul.rofm.services;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.itaul.rofm.exception.InternalServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class S3Service {

    private final AmazonS3 s3client;

    @Value("${app.bucket.name}")
    private String bucketName;

    @Value("${app.bucket.expirationIn}")
    private int expirationIn;

    public S3Service(@Value("${app.bucket.access}") String access,
                     @Value("${app.bucket.secret}") String secret,
                     @Value("${app.bucket.endpoint}") String endpoint,
                     @Value("${app.bucket.region}") String region) {
        var creds = new BasicAWSCredentials(access, secret);
        this.s3client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(creds))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .build();
    }

    public void upload(File file, String key) {
        try {
            if (file != null && key != null)
                s3client.putObject(new PutObjectRequest(bucketName, key, file));
        } catch (Exception e) {
            throw new InternalServerException("Ошибка при загрузке в S3", e);
        }
    }

    public void delete(String key) {
        try {
            if (key != null)
                s3client.deleteObject(bucketName, key);
        } catch (Exception e) {
            throw new InternalServerException("Ошибка при удалении из S3", e);
        }
    }

    public void copy(String sourceKey, String destinationKey) {
        if (sourceKey != null && destinationKey != null) {
            try {
                s3client.copyObject(bucketName, sourceKey, bucketName, destinationKey);
            } catch (Exception e) {
                throw new InternalServerException("Failed to copy object", e);
            }
        }
    }

    public String generatePath(String prefix, UUID id, String ext) {
        return String.format("%s/%s/%s.%s", prefix, id, UUID.randomUUID(), ext);
    }

    public String generatePresignedUrl(String key) {
        if (key == null)
            return null;
        var expiration = new Date(System.currentTimeMillis() + expirationIn);
        return s3client.generatePresignedUrl(bucketName, key, expiration, HttpMethod.GET).toString();
    }
}

