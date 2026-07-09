package com.fitfuel.user;

import com.fitfuel.common.NotFoundException;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/me")
    UserProfileResponse me(Authentication authentication) {
        return UserProfileResponse.from(currentUser(authentication));
    }

    @PutMapping("/me")
    UserProfileResponse updateMe(Authentication authentication, @RequestBody UpdateProfileRequest request) {
        AppUser user = currentUser(authentication);
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.age() != null) {
            user.setAge(request.age());
        }
        if (request.weight() != null) {
            user.setWeight(request.weight());
        }
        return UserProfileResponse.from(userRepository.save(user));
    }

    @PutMapping("/me/password")
    void changePassword(Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        AppUser user = currentUser(authentication);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    private AppUser currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
