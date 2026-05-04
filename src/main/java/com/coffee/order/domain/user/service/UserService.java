package com.coffee.order.domain.user.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.domain.user.dto.response.UserResponseDto;
import com.coffee.order.domain.user.entity.User;
import com.coffee.order.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public UserResponseDto getUser(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponseDto.from(user);
    }
}