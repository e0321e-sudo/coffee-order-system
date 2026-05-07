package com.coffee.order.domain.store.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.domain.store.dto.request.StoreRequestDto;
import com.coffee.order.domain.store.dto.response.StoreResponseDto;
import com.coffee.order.domain.store.entity.Store;
import com.coffee.order.domain.store.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @InjectMocks
    private StoreService storeService;

    @Mock
    private StoreRepository storeRepository;

    private Store store;

    @BeforeEach
    void setUp() {
        store = Store.builder()
                .name("수지네 커피 창원점")
                .address("창원시 의창구")
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(store, "id", 1L);
    }

    // ===== create =====

    @Test
    @DisplayName("매장 등록 성공 - isActive true로 생성")
    void 매장_등록_성공() {
        // given
        StoreRequestDto request = new StoreRequestDto("수지네 커피 창원점", "창원시 의창구");
        given(storeRepository.save(any(Store.class))).willReturn(store);

        // when
        StoreResponseDto response = storeService.create(request);

        // then
        assertThat(response.name()).isEqualTo("수지네 커피 창원점");
        assertThat(response.address()).isEqualTo("창원시 의창구");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("매장 등록 시 storeRepository.save 1회 호출")
    void 매장_등록_저장_호출_검증() {
        // given
        StoreRequestDto request = new StoreRequestDto("신규 매장", "부산시 해운대구");
        given(storeRepository.save(any(Store.class))).willReturn(store);

        // when
        storeService.create(request);

        // then
        then(storeRepository).should(times(1)).save(any(Store.class));
    }

    // ===== findAll =====

    @Test
    @DisplayName("매장 전체 조회 성공")
    void 매장_전체조회_성공() {
        // given
        Store store2 = Store.builder()
                .name("수지네 커피 부산점")
                .address("부산시 해운대구")
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(store2, "id", 2L);
        given(storeRepository.findAll()).willReturn(List.of(store, store2));

        // when
        List<StoreResponseDto> result = storeService.findAll();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("수지네 커피 창원점");
        assertThat(result.get(1).name()).isEqualTo("수지네 커피 부산점");
    }

    @Test
    @DisplayName("매장 전체 조회 - 등록된 매장 없을 때 빈 목록 반환")
    void 매장_전체조회_빈목록() {
        // given
        given(storeRepository.findAll()).willReturn(Collections.emptyList());

        // when
        List<StoreResponseDto> result = storeService.findAll();

        // then
        assertThat(result).isEmpty();
    }

    // ===== findById =====

    @Test
    @DisplayName("매장 단건 조회 성공")
    void 매장_단건조회_성공() {
        // given
        given(storeRepository.findById(1L)).willReturn(Optional.of(store));

        // when
        StoreResponseDto response = storeService.findById(1L);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("수지네 커피 창원점");
        assertThat(response.address()).isEqualTo("창원시 의창구");
    }

    @Test
    @DisplayName("매장 단건 조회 - 존재하지 않는 매장 STORE_NOT_FOUND 예외")
    void 매장_단건조회_없음_예외() {
        // given
        given(storeRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> storeService.findById(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }

    // ===== update =====

    @Test
    @DisplayName("매장 정보 수정 성공")
    void 매장_수정_성공() {
        // given
        StoreRequestDto request = new StoreRequestDto("수지네 커피 마산점", "마산시 회원구");
        given(storeRepository.findById(1L)).willReturn(Optional.of(store));

        // when
        StoreResponseDto response = storeService.update(1L, request);

        // then
        assertThat(response.name()).isEqualTo("수지네 커피 마산점");
        assertThat(response.address()).isEqualTo("마산시 회원구");
    }

    @Test
    @DisplayName("매장 수정 - 수정 후에도 isActive 유지")
    void 매장_수정_isActive_유지() {
        // given
        StoreRequestDto request = new StoreRequestDto("변경된 이름", "변경된 주소");
        given(storeRepository.findById(1L)).willReturn(Optional.of(store));

        // when
        StoreResponseDto response = storeService.update(1L, request);

        // then
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("매장 수정 - 존재하지 않는 매장 STORE_NOT_FOUND 예외")
    void 매장_수정_없음_예외() {
        // given
        StoreRequestDto request = new StoreRequestDto("이름", "주소");
        given(storeRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> storeService.update(999L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }

    // ===== delete =====

    @Test
    @DisplayName("매장 삭제 성공 - storeRepository.deleteById 호출")
    void 매장_삭제_성공() {
        // given
        given(storeRepository.findById(1L)).willReturn(Optional.of(store));
        willDoNothing().given(storeRepository).deleteById(1L);

        // when
        storeService.delete(1L);

        // then
        then(storeRepository).should(times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("매장 삭제 - 존재하지 않는 매장 STORE_NOT_FOUND 예외")
    void 매장_삭제_없음_예외() {
        // given
        given(storeRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> storeService.delete(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }

    @Test
    @DisplayName("매장 삭제 - 없는 매장이면 deleteById 미호출")
    void 매장_삭제_없음_deleteById_미호출() {
        // given
        given(storeRepository.findById(999L)).willReturn(Optional.empty());

        // when
        try {
            storeService.delete(999L);
        } catch (BusinessException ignored) {}

        // then
        then(storeRepository).should(never()).deleteById(any());
    }

    // ===== getStore (내부 공통 조회) =====

    @Test
    @DisplayName("getStore - 존재하는 매장 반환")
    void getStore_성공() {
        // given
        given(storeRepository.findById(1L)).willReturn(Optional.of(store));

        // when
        Store result = storeService.getStore(1L);

        // then
        assertThat(result.getName()).isEqualTo("수지네 커피 창원점");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("getStore - 존재하지 않는 매장 STORE_NOT_FOUND 예외")
    void getStore_없음_예외() {
        // given
        given(storeRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> storeService.getStore(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }
}