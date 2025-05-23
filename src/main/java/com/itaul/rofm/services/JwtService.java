package com.itaul.rofm.services;

import com.itaul.rofm.exception.BadRequestException;
import com.itaul.rofm.exception.InternalServerException;
import com.itaul.rofm.security.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class JwtService {
    private final JwtUtil jwtUtil;


    public Map<String, String> generateJwt(Long id) {
        try {
            String accessToken = jwtUtil.generateAccessToken(id);
            String refreshToken = jwtUtil.generateRefreshToken(id);

            Map<String, String> map = new HashMap<>();
            map.put("access_token", accessToken);
            map.put("refresh_token", refreshToken);
            return map;
        } catch (Exception e) {
            throw new InternalServerException("Failed to generate JWT", e);
        }
    }

    public Long getUserId(String token) {
        try {
            return jwtUtil.getUserIdFromAccessToken(token.substring(7));
        } catch (ExpiredJwtException e) {
            throw new BadRequestException("Token is expired", e);
        } catch (UnsupportedJwtException e) {
            throw new BadRequestException("Token is unsupported", e);
        } catch (MalformedJwtException e) {
            throw new BadRequestException("Token is malformed", e);
        } catch (SignatureException e) {
            throw new BadRequestException("Token is invalid", e);
        } catch (Exception e) {
            throw new InternalServerException("Internal server error", e);
        }
    }

    public HashMap<String, String> refreshJwt(String refreshToken) {
        try {
            Long userId = jwtUtil.getUserIdFromRefreshToken(refreshToken);
            String accessToken = jwtUtil.generateAccessToken(userId);
            var map = new HashMap<String, String>();
            map.put("access_token", accessToken);
            return map;
        } catch (ExpiredJwtException e) {
            throw new BadRequestException("Token is expired", e);
        } catch (UnsupportedJwtException e) {
            throw new BadRequestException("Token is unsupported", e);
        } catch (MalformedJwtException e) {
            throw new BadRequestException("Token is malformed", e);
        } catch (SignatureException e) {
            throw new BadRequestException("Token is invalid", e);
        } catch (Exception e) {
            throw new InternalServerException("Internal server error", e);
        }
    }
}
