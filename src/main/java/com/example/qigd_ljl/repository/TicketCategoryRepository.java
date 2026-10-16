package com.example.qigd_ljl.repository;

import com.example.qigd_ljl.entity.TicketCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketCategoryRepository extends JpaRepository<TicketCategory, Long> {

    List<TicketCategory> findByEnabledTrue();

    List<TicketCategory> findByParentId(Long parentId);

    boolean existsByName(String name);
}