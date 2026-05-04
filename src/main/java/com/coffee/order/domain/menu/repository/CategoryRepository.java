package com.coffee.order.domain.menu.repository;

import com.coffee.order.domain.menu.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}