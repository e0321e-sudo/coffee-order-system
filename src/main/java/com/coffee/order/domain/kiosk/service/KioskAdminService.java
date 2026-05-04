package com.coffee.order.domain.kiosk.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.domain.kiosk.dto.request.KioskRequestDto;
import com.coffee.order.domain.kiosk.dto.response.KioskResponseDto;
import com.coffee.order.domain.kiosk.entity.Kiosk;
import com.coffee.order.domain.kiosk.repository.KioskRepository;
import com.coffee.order.domain.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KioskAdminService {

    private final KioskRepository kioskRepository;
    private final StoreService storeService;

    @Transactional
    public KioskResponseDto create(Long storeId, KioskRequestDto request) {
        storeService.getStore(storeId);
        Kiosk kiosk = Kiosk.builder()
                .storeId(storeId)
                .kioskUuid(UUID.randomUUID().toString())
                .secretKey(UUID.randomUUID().toString())
                .name(request.getName())
                .isActive(true)
                .build();
        return KioskResponseDto.from(kioskRepository.save(kiosk));
    }

    @Transactional(readOnly = true)
    public List<KioskResponseDto> findByStoreId(Long storeId) {
        storeService.getStore(storeId);
        return kioskRepository.findAllByStoreId(storeId).stream()
                .map(KioskResponseDto::from)
                .toList();
    }

    @Transactional
    public void delete(Long storeId, Long kioskId) {
        storeService.getStore(storeId);
        Kiosk kiosk = kioskRepository.findByIdAndStoreId(kioskId, storeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.KIOSK_NOT_FOUND));
        kioskRepository.delete(kiosk);
    }
}