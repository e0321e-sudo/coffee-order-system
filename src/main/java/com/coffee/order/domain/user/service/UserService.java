package com.coffee.order.domain.user.service;

import com.coffee.order.domain.user.dto.response.UserResponseDto;
import com.coffee.order.domain.user.entity.User;
import com.coffee.order.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User findOrCreateUser(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .phoneNumber(phoneNumber)
                                .point(0L)
                                .build()
                ));
    }

    @Transactional
    public UserResponseDto getUser(String phoneNumber) {
        Optional<User> existing = userRepository.findByPhoneNumber(phoneNumber);
        if (existing.isPresent()) {
            return UserResponseDto.from(existing.get(), false);
        }
        User newUser = userRepository.save(User.builder()
                .phoneNumber(phoneNumber)
                .point(0L)
                .build());
        return UserResponseDto.from(newUser, true);
    }
}