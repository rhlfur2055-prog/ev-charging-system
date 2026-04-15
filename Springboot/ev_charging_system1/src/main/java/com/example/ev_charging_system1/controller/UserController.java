package com.example.ev_charging_system1.controller;

import com.example.ev_charging_system1.dto.user.LoginDTO;
import com.example.ev_charging_system1.dto.user.UserProfileDTO;
import com.example.ev_charging_system1.dto.user.UserResponseDTO;
import com.example.ev_charging_system1.dto.user.UserSignupDTO;
import com.example.ev_charging_system1.security.UserPrincipal;
import com.example.ev_charging_system1.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public UserResponseDTO signup(@Valid @RequestBody UserSignupDTO dto) {
        return userService.signup(dto);
    }

    @PostMapping("/login")
    public UserResponseDTO login(@Valid @RequestBody LoginDTO dto) {
        return userService.login(dto);
    }

    /**
     * Legacy: profile by userPk query param. Kept for backward-compat with the
     * existing dashboard which still passes ?userPk=N. Will be deprecated once
     * frontend migrates fully to /api/users/me.
     */
    @GetMapping("/profile")
    public UserProfileDTO getProfile(@RequestParam(required = false) Long userPk,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        Long id = userPk;
        if (id == null && principal != null) {
            id = principal.userPk();
        }
        if (id == null) {
            throw new IllegalArgumentException("userPk 또는 Authorization 헤더가 필요합니다.");
        }
        return userService.getUserProfile(id);
    }

    /**
     * Token-based: resolves the caller from the JWT in the Authorization header.
     * Frontend can rely on this once Authorization is sent on every request.
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(userService.getUserProfile(principal.userPk()));
    }
}
