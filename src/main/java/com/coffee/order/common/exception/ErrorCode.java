package com.coffee.order.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "메뉴를 찾을 수 없습니다."),
    MENU_SOLD_OUT(HttpStatus.BAD_REQUEST, "품절된 메뉴입니다."),
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "포인트가 부족합니다."),

    STORE_CLOSED(HttpStatus.BAD_REQUEST, "영업하지 않는 매장입니다."),
    OUT_OF_BUSINESS_HOURS(HttpStatus.BAD_REQUEST, "영업 시간이 아닙니다."),

    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    ORDER_CANCEL_EXPIRED(HttpStatus.BAD_REQUEST, "주문 취소 가능 시간이 지났습니다."),

    INVALID_KIOSK(HttpStatus.UNAUTHORIZED, "유효하지 않은 키오스크입니다."),

    ADMIN_NOT_FOUND(HttpStatus.NOT_FOUND, "관리자를 찾을 수 없습니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "매장을 찾을 수 없습니다."),
    KIOSK_NOT_FOUND(HttpStatus.NOT_FOUND, "키오스크를 찾을 수 없습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "재고를 찾을 수 없습니다."),
    SPECIAL_CLOSE_NOT_FOUND(HttpStatus.NOT_FOUND, "특별 휴무를 찾을 수 없습니다."),
    DUPLICATE_SPECIAL_CLOSE(HttpStatus.BAD_REQUEST, "이미 등록된 특별 휴무일입니다."),
    DUPLICATE_REQUEST(HttpStatus.CONFLICT, "중복 요청입니다. 잠시 후 다시 시도해주세요."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),

    CART_EMPTY(HttpStatus.BAD_REQUEST, "장바구니가 비어있습니다."),
    CART_ITEM_NOT_FOUND(HttpStatus.BAD_REQUEST, "장바구니에 없는 메뉴입니다.");

    private final HttpStatus status;
    private final String message;
}