package com.coffee.order.domain.user.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.common.service.IdempotencyService;
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
    private final IdempotencyService idempotencyService;
    private final UserService userService;

    @Transactional
    public PointChargeResponseDto charge(PointChargeRequestDto request) {

        // 중복 충전 방지 - 같은 Idempotency-Key로 5분 내 재요청 시 차단
        if (request.getIdempotencyKey() != null) {
            idempotencyService.checkAndSave(request.getIdempotencyKey(), "point");
        }

        User user = userService.findOrCreateUser(request.getPhoneNumber());

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