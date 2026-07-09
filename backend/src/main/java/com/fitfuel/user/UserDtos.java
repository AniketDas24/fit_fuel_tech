package com.fitfuel.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

final class UserDtos {
    private UserDtos() {
    }
}

record SignupRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank String phone,
        @Size(min = 8) String password,
        Integer age,
        Double weight
) {
}

record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
}

record AuthResponse(String token, UserProfileResponse user) {
}

record UpdateProfileRequest(String name, String phone, Integer age, Double weight) {
}

record ChangePasswordRequest(@NotBlank String currentPassword, @Size(min = 8) String newPassword) {
}

record UserProfileResponse(Long id, String name, String email, String phone, Integer age, Double weight, Role role) {
    static UserProfileResponse from(AppUser user) {
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getAge(),
                user.getWeight(),
                user.getRole()
        );
    }
}
