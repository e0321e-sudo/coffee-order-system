package com.coffee.order.domain.store.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.domain.store.dto.request.SpecialCloseRequestDto;
import com.coffee.order.domain.store.dto.response.SpecialCloseResponseDto;
import com.coffee.order.domain.store.entity.SpecialClose;
import com.coffee.order.domain.store.repository.SpecialCloseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SpecialCloseService {

    private final SpecialCloseRepository specialCloseRepository;
    private final StoreService storeService;

    @Transactional
    public SpecialCloseResponseDto create(Long storeId, SpecialCloseRequestDto request) {
        storeService.getStore(storeId);
        if (specialCloseRepository.existsByStoreIdAndCloseDate(storeId, request.getCloseDate())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SPECIAL_CLOSE);
        }
        SpecialClose specialClose = SpecialClose.builder()
                .storeId(storeId)
                .closeDate(request.getCloseDate())
                .reason(request.getReason())
                .build();
        return SpecialCloseResponseDto.from(specialCloseRepository.save(specialClose));
    }

    @Transactional(readOnly = true)
    public List<SpecialCloseResponseDto> findByStoreId(Long storeId) {
        storeService.getStore(storeId);
        return specialCloseRepository.findByStoreId(storeId).stream()
                .map(SpecialCloseResponseDto::from)
                .toList();
    }

    @Transactional
    public void delete(Long storeId, Long specialCloseId) {
        storeService.getStore(storeId);
        SpecialClose specialClose = specialCloseRepository.findById(specialCloseId)
                .filter(sc -> sc.getStoreId().equals(storeId))
                .orElseThrow(() -> new BusinessException(ErrorCode.SPECIAL_CLOSE_NOT_FOUND));
        specialCloseRepository.delete(specialClose);
    }
}