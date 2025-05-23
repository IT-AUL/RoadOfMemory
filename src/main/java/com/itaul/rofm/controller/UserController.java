package com.itaul.rofm.controller;

import com.itaul.rofm.dto.RefreshTokenDto;
import com.itaul.rofm.dto.UserAuthDto;
import com.itaul.rofm.exception.BadRequestException;
import com.itaul.rofm.services.JwtService;
import com.itaul.rofm.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/auth")
    public ResponseEntity<Map<String, String>> createUser(
            @Valid @RequestBody UserAuthDto userAuthDto) {
        var valid = userService.validateUser(userAuthDto);
        if (!valid)
            throw new BadRequestException("User data is not valid");
        var user = userService.findById(userAuthDto.getId());
        if (user.isEmpty())
            user = Optional.of(userService.addUser(userAuthDto));
        System.out.println(userAuthDto.getUsername());
        var tokens = jwtService.generateJwt(user.get().getId());
        return ResponseEntity.ok(tokens);

    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refreshToken(
            @RequestBody RefreshTokenDto refreshTokenDto) {
        var refreshToken = jwtService.refreshJwt(refreshTokenDto.refreshToken());
        return ResponseEntity.ok(refreshToken);
    }


}
