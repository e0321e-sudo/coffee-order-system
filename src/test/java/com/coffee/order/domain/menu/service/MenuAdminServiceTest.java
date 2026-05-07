package com.coffee.order.domain.menu.service;

import com.coffee.order.common.exception.BusinessException;
import com.coffee.order.common.exception.ErrorCode;
import com.coffee.order.domain.menu.dto.request.MenuAdminRequestDto;
import com.coffee.order.domain.menu.dto.request.StockAddRequestDto;
import com.coffee.order.domain.menu.dto.response.MenuResponseDto;
import com.coffee.order.domain.menu.entity.Category;
import com.coffee.order.domain.menu.entity.Menu;
import com.coffee.order.domain.menu.entity.MenuStock;
import com.coffee.order.domain.menu.repository.CategoryRepository;
import com.coffee.order.domain.menu.repository.MenuRepository;
import com.coffee.order.domain.menu.repository.MenuStockRepository;
import com.coffee.order.domain.stock.kafka.StockProducer;
import com.coffee.order.domain.stock.repository.StockHistoryRepository;
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
class MenuAdminServiceTest {

    @InjectMocks
    private MenuAdminService menuAdminService;

    @Mock private MenuRepository menuRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private MenuStockRepository menuStockRepository;
    @Mock private StockHistoryRepository stockHistoryRepository;
    @Mock private StockProducer stockProducer;
    @Mock private StoreRepository storeRepository;

    private Menu menu;
    private Category category;
    private MenuStock menuStock;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .name("커피")
                .displayOrder(1)
                .isVisible(true)
                .build();
        ReflectionTestUtils.setField(category, "id", 1L);

        menu = Menu.builder()
                .categoryId(1L)
                .name("아메리카노")
                .price(4500)
                .isVisible(true)
                .build();
        ReflectionTestUtils.setField(menu, "id", 1L);

        menuStock = MenuStock.builder()
                .storeId(1L)
                .menuId(1L)
                .stock(10)
                .build();
        ReflectionTestUtils.setField(menuStock, "id", 1L);
    }

    // ===== create =====

    @Test
    @DisplayName("메뉴 등록 성공")
    void 메뉴_등록_성공() {
        // given
        MenuAdminRequestDto request = new MenuAdminRequestDto(1L, "아메리카노", 4500);
        given(menuRepository.save(any(Menu.class))).willReturn(menu);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

        // when
        MenuResponseDto response = menuAdminService.create(request);

        // then
        assertThat(response.name()).isEqualTo("아메리카노");
        assertThat(response.price()).isEqualTo(4500);
        assertThat(response.categoryName()).isEqualTo("커피");
    }

    @Test
    @DisplayName("메뉴 등록 성공 - menuRepository.save 1회 호출")
    void 메뉴_등록_저장_호출_검증() {
        // given
        MenuAdminRequestDto request = new MenuAdminRequestDto(1L, "라떼", 5000);
        given(menuRepository.save(any(Menu.class))).willReturn(menu);
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

        // when
        menuAdminService.create(request);

        // then
        then(menuRepository).should(times(1)).save(any(Menu.class));
    }

    @Test
    @DisplayName("메뉴 등록 - 카테고리 없을 때 categoryName null 처리")
    void 메뉴_등록_카테고리없음_카테고리명_null() {
        // given
        MenuAdminRequestDto request = new MenuAdminRequestDto(99L, "신메뉴", 6000);
        given(menuRepository.save(any(Menu.class))).willReturn(menu);
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());

        // when
        MenuResponseDto response = menuAdminService.create(request);

        // then
        assertThat(response.categoryName()).isNull();
    }

    // ===== findAll =====

    @Test
    @DisplayName("메뉴 전체 조회 성공")
    void 메뉴_전체조회_성공() {
        // given
        given(menuRepository.findAllByIsVisibleTrue()).willReturn(List.of(menu));
        given(categoryRepository.findAllById(any())).willReturn(List.of(category));

        // when
        List<MenuResponseDto> result = menuAdminService.findAll();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("아메리카노");
        assertThat(result.get(0).categoryName()).isEqualTo("커피");
    }

    @Test
    @DisplayName("메뉴 전체 조회 - 등록된 메뉴 없을 때 빈 목록 반환")
    void 메뉴_전체조회_빈목록() {
        // given
        given(menuRepository.findAllByIsVisibleTrue()).willReturn(Collections.emptyList());

        // when
        List<MenuResponseDto> result = menuAdminService.findAll();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("메뉴 전체 조회 - 여러 메뉴 반환")
    void 메뉴_전체조회_여러개() {
        // given
        Menu latte = Menu.builder().categoryId(1L).name("라떼").price(5000).isVisible(true).build();
        ReflectionTestUtils.setField(latte, "id", 2L);

        given(menuRepository.findAllByIsVisibleTrue()).willReturn(List.of(menu, latte));
        given(categoryRepository.findAllById(any())).willReturn(List.of(category));

        // when
        List<MenuResponseDto> result = menuAdminService.findAll();

        // then
        assertThat(result).hasSize(2);
    }

    // ===== update =====

    @Test
    @DisplayName("메뉴 수정 성공")
    void 메뉴_수정_성공() {
        // given
        MenuAdminRequestDto request = new MenuAdminRequestDto(1L, "콜드브루", 5500);
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

        // when
        MenuResponseDto response = menuAdminService.update(1L, request);

        // then
        assertThat(response.name()).isEqualTo("콜드브루");
        assertThat(response.price()).isEqualTo(5500);
    }

    @Test
    @DisplayName("메뉴 수정 - 존재하지 않는 메뉴 MENU_NOT_FOUND 예외")
    void 메뉴_수정_메뉴없음_예외() {
        // given
        MenuAdminRequestDto request = new MenuAdminRequestDto(1L, "콜드브루", 5500);
        given(menuRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> menuAdminService.update(999L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MENU_NOT_FOUND);
    }

    @Test
    @DisplayName("메뉴 수정 - 카테고리 없을 때 categoryName null 처리")
    void 메뉴_수정_카테고리없음_카테고리명_null() {
        // given
        MenuAdminRequestDto request = new MenuAdminRequestDto(99L, "콜드브루", 5500);
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu));
        given(categoryRepository.findById(99L)).willReturn(Optional.empty());

        // when
        MenuResponseDto response = menuAdminService.update(1L, request);

        // then
        assertThat(response.categoryName()).isNull();
    }

    // ===== hide =====

    @Test
    @DisplayName("메뉴 숨김 처리 성공 - isVisible false 전환")
    void 메뉴_숨김_성공() {
        // given
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

        // when
        menuAdminService.hide(1L);

        // then: menu.hide() 호출로 isVisible = false
        assertThat(menu.isVisible()).isFalse();
    }

    @Test
    @DisplayName("메뉴 숨김 처리 성공 - MenuResponseDto 반환")
    void 메뉴_숨김_응답_검증() {
        // given
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category));

        // when
        MenuResponseDto response = menuAdminService.hide(1L);

        // then
        assertThat(response.menuId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("아메리카노");
    }

    @Test
    @DisplayName("메뉴 숨김 - 존재하지 않는 메뉴 MENU_NOT_FOUND 예외")
    void 메뉴_숨김_메뉴없음_예외() {
        // given
        given(menuRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> menuAdminService.hide(999L))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MENU_NOT_FOUND);
    }

    // ===== addStock =====

    @Test
    @DisplayName("재고 추가 성공 - 재고 증가 및 이력 저장")
    void 재고_추가_성공() {
        // given
        StockAddRequestDto request = new StockAddRequestDto(1L, 20);
        Store store = Store.builder().name("테스트 매장").address("서울시").isActive(true).build();

        given(menuRepository.findById(1L)).willReturn(Optional.of(menu));
        given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(menuStock));
        given(stockHistoryRepository.save(any())).willReturn(null);
        given(storeRepository.findById(1L)).willReturn(Optional.of(store));

        // when
        menuAdminService.addStock(1L, request);

        // then: 10 + 20 = 30
        assertThat(menuStock.getStock()).isEqualTo(30);
    }

    @Test
    @DisplayName("재고 추가 - 재고 이력 1회 저장")
    void 재고_추가_이력_저장() {
        // given
        StockAddRequestDto request = new StockAddRequestDto(1L, 10);
        Store store = Store.builder().name("테스트 매장").address("서울시").isActive(true).build();

        given(menuRepository.findById(1L)).willReturn(Optional.of(menu));
        given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(menuStock));
        given(stockHistoryRepository.save(any())).willReturn(null);
        given(storeRepository.findById(1L)).willReturn(Optional.of(store));

        // when
        menuAdminService.addStock(1L, request);

        // then
        then(stockHistoryRepository).should(times(1)).save(any());
    }

    @Test
    @DisplayName("재고 추가 - 재입고 Kafka 이벤트 발행")
    void 재고_추가_재입고_카프카이벤트_발행() {
        // given
        StockAddRequestDto request = new StockAddRequestDto(1L, 5);
        Store store = Store.builder().name("테스트 매장").address("서울시").isActive(true).build();

        given(menuRepository.findById(1L)).willReturn(Optional.of(menu));
        given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(menuStock));
        given(stockHistoryRepository.save(any())).willReturn(null);
        given(storeRepository.findById(1L)).willReturn(Optional.of(store));

        // when
        menuAdminService.addStock(1L, request);

        // then
        then(stockProducer).should(times(1)).sendStockRestocked(any());
    }

    @Test
    @DisplayName("재고 추가 - 품절 상태에서 재입고 시 품절 해제")
    void 재고_추가_품절상태_품절해제() {
        // given
        MenuStock soldOutStock = MenuStock.builder().storeId(1L).menuId(1L).stock(0).build();
        StockAddRequestDto request = new StockAddRequestDto(1L, 10);
        Store store = Store.builder().name("테스트 매장").address("서울시").isActive(true).build();

        given(menuRepository.findById(1L)).willReturn(Optional.of(menu));
        given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(soldOutStock));
        given(stockHistoryRepository.save(any())).willReturn(null);
        given(storeRepository.findById(1L)).willReturn(Optional.of(store));

        // when
        menuAdminService.addStock(1L, request);

        // then: 품절 해제 + 재고 10
        assertThat(soldOutStock.getStock()).isEqualTo(10);
        assertThat(soldOutStock.isSoldOut()).isFalse();
    }

    @Test
    @DisplayName("재고 추가 - 존재하지 않는 메뉴 MENU_NOT_FOUND 예외")
    void 재고_추가_메뉴없음_예외() {
        // given
        StockAddRequestDto request = new StockAddRequestDto(1L, 10);
        given(menuRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> menuAdminService.addStock(999L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MENU_NOT_FOUND);
    }

    @Test
    @DisplayName("재고 추가 - 재고 레코드 없음 STOCK_NOT_FOUND 예외")
    void 재고_추가_재고레코드없음_예외() {
        // given
        StockAddRequestDto request = new StockAddRequestDto(1L, 10);
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu));
        given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> menuAdminService.addStock(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STOCK_NOT_FOUND);
    }

    @Test
    @DisplayName("재고 추가 - 매장 이름 없을 때 '알 수 없는 매장' 사용")
    void 재고_추가_매장없을때_기본매장명_사용() {
        // given
        StockAddRequestDto request = new StockAddRequestDto(1L, 5);

        given(menuRepository.findById(1L)).willReturn(Optional.of(menu));
        given(menuStockRepository.findByStoreIdAndMenuIdWithLock(1L, 1L)).willReturn(Optional.of(menuStock));
        given(stockHistoryRepository.save(any())).willReturn(null);
        given(storeRepository.findById(1L)).willReturn(Optional.empty());

        // when & then: 예외 없이 처리 (기본값 "알 수 없는 매장" 사용)
        assertThatCode(() -> menuAdminService.addStock(1L, request)).doesNotThrowAnyException();
        then(stockProducer).should(times(1)).sendStockRestocked(any());
    }
}