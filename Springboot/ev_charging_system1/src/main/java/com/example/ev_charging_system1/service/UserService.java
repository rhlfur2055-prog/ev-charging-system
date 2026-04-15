package com.example.ev_charging_system1.service;

import com.example.ev_charging_system1.dto.user.LoginDTO;
import com.example.ev_charging_system1.dto.user.UserProfileDTO;
import com.example.ev_charging_system1.dto.user.UserResponseDTO;
import com.example.ev_charging_system1.dto.user.UserSignupDTO;
import com.example.ev_charging_system1.entity.User;
import com.example.ev_charging_system1.entity.Vehicle;
import com.example.ev_charging_system1.repository.UserRepository;
import com.example.ev_charging_system1.repository.VehicleRepository;
import com.example.ev_charging_system1.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final VehicleService vehicleService;
    private final VehicleRepository vehicleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserResponseDTO signup(UserSignupDTO dto) {
        if (userRepository.findByLoginId(dto.getLoginId()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        User user = new User();
        user.setLoginId(dto.getLoginId());
        // BCrypt hash. Reason: store-only-hashes; existing plain-text rows
        // become unusable on next login — see migration note in V1__init.sql.
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setBuilding(dto.getBuilding());
        user.setUnit(dto.getUnit());
        user.setRole("USER");

        User savedUser = userRepository.save(user);

        Vehicle vehicle = new Vehicle();
        vehicle.setUserPk(savedUser.getUserPk());
        vehicle.setPlateNumber(dto.getPlateNumber());
        vehicle.setVehicleType(dto.getVehicleType());
        vehicleService.saveVehicle(vehicle);

        return new UserResponseDTO(
                savedUser.getUserPk(),
                savedUser.getLoginId(),
                savedUser.getName(),
                savedUser.getRole()
        );
    }

    public UserResponseDTO login(LoginDTO dto) {
        User user = userRepository.findByLoginId(dto.getLoginId())
                .orElseThrow(() -> new RuntimeException("아이디가 존재하지 않습니다."));

        boolean ok;
        String stored = user.getPassword() == null ? "" : user.getPassword().trim();
        if (stored.startsWith("$2a$") || stored.startsWith("$2b$") || stored.startsWith("$2y$")) {
            ok = passwordEncoder.matches(dto.getPassword(), stored);
        } else {
            // Legacy plain-text fallback (one-shot upgrade): if it matches, re-hash and persist.
            ok = stored.equals(dto.getPassword().trim());
            if (ok) {
                user.setPassword(passwordEncoder.encode(dto.getPassword()));
                userRepository.save(user);
            }
        }
        if (!ok) {
            throw new RuntimeException("비밀번호가 틀렸습니다.");
        }

        String token = jwtUtil.issue(user.getUserPk(), user.getLoginId(), user.getRole());
        return new UserResponseDTO(
                user.getUserPk(),
                user.getLoginId(),
                user.getName(),
                user.getRole(),
                token,
                "Bearer"
        );
    }

    public UserProfileDTO getUserProfile(Long userPk) {
        User user = userRepository.findById(userPk)
                .orElseThrow(() -> new RuntimeException("사용자 없음"));

        Vehicle vehicle = vehicleRepository.findByUserPk(userPk)
                .orElseThrow(() -> new RuntimeException("차량 없음"));

        return new UserProfileDTO(
                user.getName(),
                vehicle.getPlateNumber(),
                user.getRole()
        );
    }
}
