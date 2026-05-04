package com.coffee.order.domain.menu.repository;

import com.coffee.order.domain.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    Optional<Menu> findByIdAndIsVisibleTrue(Long id);

    List<Menu> findAllByCategoryIdAndIsVisibleTrue(Long categoryId);

    List<Menu> findAllByIsVisibleTrue();
}