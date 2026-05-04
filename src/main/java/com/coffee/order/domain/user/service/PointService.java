package com.coffee.order.domain.user.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.domain.user.dto.request.PointChargeRequestDto;
import com.coffee.order.domain.user.dto.response.PointChargeResponseDto;
import com.coffee.order.domain.user.entity.User;
import com.coffee.order.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserRepository userRepository;

    @Transactional
    public PointChargeResponseDto charge(PointChargeRequestDto request) {
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 비관적 락으로 동시 충전 직렬화
        User lockedUser = userRepository.findByIdWithLock(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        lockedUser.chargePoint(request.getAmount());

        return new PointChargeResponseDto(
                lockedUser.getPhoneNumber(),
                request.getAmount(),
                lockedUser.getPoint()
        );
    }
}