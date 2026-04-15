package com.example.ev_charging_system1.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {

    private Long userPk;      // 추가
    private String loginId;
    private String name;
    private String role;
    private String accessToken;   // JWT (login response only; null on signup)
    private String tokenType;     // "Bearer" when accessToken present

    public UserResponseDTO(Long userPk, String loginId, String name, String role) {
        this(userPk, loginId, name, role, null, null);
    }
}