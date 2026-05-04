package com.coffee.order.domain.kiosk.repository;

import com.coffee.order.domain.kiosk.entity.Kiosk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KioskRepository extends JpaRepository<Kiosk, Long> {

    Optional<Kiosk> findByKioskUuidAndSecretKey(String kioskUuid, String secretKey);

    Optional<Kiosk> findByIdAndStoreId(Long id, Long storeId);

    List<Kiosk> findAllByStoreId(Long storeId);
}