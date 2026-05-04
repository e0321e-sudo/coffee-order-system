package com.coffee.order.domain.store.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.domain.store.dto.request.StoreRequestDto;
import com.coffee.order.domain.store.dto.response.StoreResponseDto;
import com.coffee.order.domain.store.entity.Store;
import com.coffee.order.domain.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;

    @Transactional
    public StoreResponseDto create(StoreRequestDto request) {
        Store store = Store.builder()
                .name(request.getName())
                .address(request.getAddress())
                .isActive(true)
                .build();
        return StoreResponseDto.from(storeRepository.save(store));
    }

    @Transactional(readOnly = true)
    public List<StoreResponseDto> findAll() {
        return storeRepository.findAll().stream()
                .map(StoreResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoreResponseDto findById(Long id) {
        return StoreResponseDto.from(getStore(id));
    }

    @Transactional
    public StoreResponseDto update(Long id, StoreRequestDto request) {
        Store store = getStore(id);
        store.update(request.getName(), request.getAddress());
        return StoreResponseDto.from(store);
    }

    @Transactional
    public void delete(Long id) {
        getStore(id);
        storeRepository.deleteById(id);
    }

    public Store getStore(Long id) {
        return storeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.STORE_NOT_FOUND));
    }
}