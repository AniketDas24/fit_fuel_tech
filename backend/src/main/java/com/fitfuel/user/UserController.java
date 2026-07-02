package com.fitfuel.user;

import com.fitfuel.common.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
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

    private AppUser currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
