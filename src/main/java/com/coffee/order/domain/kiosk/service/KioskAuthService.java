package com.coffee.order.domain.kiosk.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.domain.kiosk.entity.Kiosk;
import com.coffee.order.domain.kiosk.repository.KioskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KioskAuthService {

    private final KioskRepository kioskRepository;

    @Transactional(readOnly = true)
    public Kiosk authenticate(String kioskUuid, String secretKey) {
        if (kioskUuid == null || secretKey == null) {
            throw new BusinessException(ErrorCode.INVALID_KIOSK);
        }
        return kioskRepository.findByKioskUuidAndSecretKey(kioskUuid, secretKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_KIOSK));
    }
}