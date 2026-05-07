package com.coffee.order.domain.kiosk.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.domain.kiosk.dto.request.KioskRequestDto;
import com.coffee.order.domain.kiosk.dto.response.KioskResponseDto;
import com.coffee.order.domain.kiosk.entity.Kiosk;
import com.coffee.order.domain.kiosk.repository.KioskRepository;
import com.coffee.order.domain.store.entity.Store;
import com.coffee.order.domain.store.service.StoreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class KioskAdminServiceTest {

    @InjectMocks
    private KioskAdminService kioskAdminService;

    @Mock
    private KioskRepository kioskRepository;

    @Mock
    private StoreService storeService;

    private Store store;
    private Kiosk kiosk;
    private KioskRequestDto kioskRequest;

    @BeforeEach
    void setUp() {
        store = Store.builder()
                .name("수지네 커피 창원점")
                .address("창원시 의창구")
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(store, "id", 1L);

        kiosk = Kiosk.builder()
                .storeId(1L)
                .kioskUuid("test-uuid-001")
                .secretKey("secret-key-001")
                .name("메인 키오스크")
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(kiosk, "id", 1L);

        kioskRequest = new KioskRequestDto("메인 키오스크");
    }

    // ===== create =====

    @Test
    @DisplayName("키오스크 등록 성공")
    void 키오스크_등록_성공() {
        // given
        given(storeService.getStore(1L)).willReturn(store);
        given(kioskRepository.save(any(Kiosk.class))).willReturn(kiosk);

        // when
        KioskResponseDto response = kioskAdminService.create(1L, kioskRequest);

        // then
        assertThat(response.storeId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("메인 키오스크");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("키오스크 등록 - UUID 자동 부여 검증")
    void 키오스크_등록_UUID_자동부여() {
        // given
        given(storeService.getStore(1L)).willReturn(store);
        given(kioskRepository.save(any(Kiosk.class))).willReturn(kiosk);
        ArgumentCaptor<Kiosk> captor = ArgumentCaptor.forClass(Kiosk.class);

        // when
        kioskAdminService.create(1L, kioskRequest);

        // then: save에 전달된 Kiosk의 UUID가 null이 아님
        then(kioskRepository).should(times(1)).save(captor.capture());
        assertThat(captor.getValue().getKioskUuid()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("키오스크 등록 - SecretKey 자동 부여 검증")
    void 키오스크_등록_SecretKey_자동부여() {
        // given
        given(storeService.getStore(1L)).willReturn(store);
        given(kioskRepository.save(any(Kiosk.class))).willReturn(kiosk);
        ArgumentCaptor<Kiosk> captor = ArgumentCaptor.forClass(Kiosk.class);

        // when
        kioskAdminService.create(1L, kioskRequest);

        // then: save에 전달된 Kiosk의 SecretKey가 null이 아님
        then(kioskRepository).should(times(1)).save(captor.capture());
        assertThat(captor.getValue().getSecretKey()).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("키오스크 등록 - 두 번 생성 시 UUID가 서로 다름")
    void 키오스크_등록_UUID_고유성() {
        // given
        given(storeService.getStore(1L)).willReturn(store);
        given(kioskRepository.save(any(Kiosk.class))).willAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Kiosk> captor = ArgumentCaptor.forClass(Kiosk.class);

        // when
        kioskAdminService.create(1L, kioskRequest);
        kioskAdminService.create(1L, kioskRequest);

        // then: 두 번 저장된 UUID가 다름
        then(kioskRepository).should(times(2)).save(captor.capture());
        List<Kiosk> captured = captor.getAllValues();
        assertThat(captured.get(0).getKioskUuid())
                .isNotEqualTo(captured.get(1).getKioskUuid());
    }

    @Test
    @DisplayName("키오스크 등록 - 존재하지 않는 매장 STORE_NOT_FOUND 예외")
    void 키오스크_등록_매장없음_예외() {
        // given
        given(storeService.getStore(999L))
                .willThrow(new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> kioskAdminService.create(999L, kioskRequest))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }

    @Test
    @DisplayName("키오스크 등록 - 매장 없을 때 kioskRepository.save 미호출")
    void 키오스크_등록_매장없음_저장미호출() {
        // given
        given(storeService.getStore(999L))
                .willThrow(new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // when
        try {
            kioskAdminService.create(999L, kioskRequest);
        } catch (BusinessException ignored) {}

        // then
        then(kioskRepository).should(never()).save(any());
    }

    // ===== findByStoreId =====

    @Test
    @DisplayName("매장별 키오스크 목록 조회 성공")
    void 키오스크_목록조회_성공() {
        // given
        Kiosk kiosk2 = Kiosk.builder()
                .storeId(1L)
                .kioskUuid("test-uuid-002")
                .secretKey("secret-key-002")
                .name("서브 키오스크")
                .isActive(true)
                .build();
        ReflectionTestUtils.setField(kiosk2, "id", 2L);

        given(storeService.getStore(1L)).willReturn(store);
        given(kioskRepository.findAllByStoreId(1L)).willReturn(List.of(kiosk, kiosk2));

        // when
        List<KioskResponseDto> result = kioskAdminService.findByStoreId(1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("메인 키오스크");
        assertThat(result.get(1).name()).isEqualTo("서브 키오스크");
    }

    @Test
    @DisplayName("매장별 키오스크 목록 조회 - 등록된 키오스크 없을 때 빈 목록 반환")
    void 키오스크_목록조회_빈목록() {
        // given
        given(storeService.getStore(1L)).willReturn(store);
        given(kioskRepository.findAllByStoreId(1L)).willReturn(Collections.emptyList());

        // when
        List<KioskResponseDto> result = kioskAdminService.findByStoreId(1L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("매장별 키오스크 목록 조회 - 존재하지 않는 매장 STORE_NOT_FOUND 예외")
    void 키오스크_목록조회_매장없음_예외() {
        // given
        given(storeService.getStore(999L))
                .willThrow(new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> kioskAdminService.findByStoreId(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }

    // ===== delete =====

    @Test
    @DisplayName("키오스크 삭제 성공")
    void 키오스크_삭제_성공() {
        // given
        given(storeService.getStore(1L)).willReturn(store);
        given(kioskRepository.findByIdAndStoreId(1L, 1L)).willReturn(Optional.of(kiosk));
        willDoNothing().given(kioskRepository).delete(kiosk);

        // when
        kioskAdminService.delete(1L, 1L);

        // then: kioskRepository.delete 1회 호출
        then(kioskRepository).should(times(1)).delete(kiosk);
    }

    @Test
    @DisplayName("키오스크 삭제 - 존재하지 않는 매장 STORE_NOT_FOUND 예외")
    void 키오스크_삭제_매장없음_예외() {
        // given
        given(storeService.getStore(999L))
                .willThrow(new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> kioskAdminService.delete(999L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }

    @Test
    @DisplayName("키오스크 삭제 - 존재하지 않는 키오스크 KIOSK_NOT_FOUND 예외")
    void 키오스크_삭제_키오스크없음_예외() {
        // given
        given(storeService.getStore(1L)).willReturn(store);
        given(kioskRepository.findByIdAndStoreId(999L, 1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> kioskAdminService.delete(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.KIOSK_NOT_FOUND);
    }

    @Test
    @DisplayName("키오스크 삭제 - 다른 매장의 키오스크 삭제 시도 KIOSK_NOT_FOUND 예외")
    void 키오스크_삭제_타매장_키오스크_예외() {
        // given: storeId=1로 조회하지만 kioskId=1은 다른 매장 소속
        given(storeService.getStore(1L)).willReturn(store);
        given(kioskRepository.findByIdAndStoreId(1L, 1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> kioskAdminService.delete(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.KIOSK_NOT_FOUND);
    }

    @Test
    @DisplayName("키오스크 삭제 - 매장 없을 때 kioskRepository 미호출")
    void 키오스크_삭제_매장없음_저장소_미호출() {
        // given
        given(storeService.getStore(999L))
                .willThrow(new BusinessException(ErrorCode.STORE_NOT_FOUND));

        // when
        try {
            kioskAdminService.delete(999L, 1L);
        } catch (BusinessException ignored) {}

        // then
        then(kioskRepository).should(never()).delete(any());
    }
}