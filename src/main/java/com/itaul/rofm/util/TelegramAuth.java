package com.itaul.rofm.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.TreeMap;

public class TelegramAuth {
    public static boolean checkTelegramAuthorization(String botToken, int aliveTime, Map<String, String> mutableAuthData) throws Exception {

        String checkHash = mutableAuthData.remove("hash");

        TreeMap<String, String> sortedAuthData = new TreeMap<>(mutableAuthData);
        StringBuilder dataCheckStringBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedAuthData.entrySet()) {
            dataCheckStringBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        String dataCheckString = dataCheckStringBuilder.toString().trim(); // Remove the last newline

        MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
        byte[] secretKey = sha256Digest.digest(botToken.getBytes(StandardCharsets.UTF_8));

        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "HmacSHA256");
        sha256Hmac.init(secretKeySpec);
        byte[] hashBytes = sha256Hmac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));
        String calculatedHash = bytesToHex(hashBytes);

        if (!calculatedHash.equals(checkHash)) {
            return false;
        }

        long authDate = Long.parseLong(mutableAuthData.get("auth_date"));
        LocalDateTime authDateTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(authDate), ZoneId.systemDefault());
        LocalDateTime now = LocalDateTime.now();
        return ChronoUnit.SECONDS.between(authDateTime, now) <= aliveTime;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}