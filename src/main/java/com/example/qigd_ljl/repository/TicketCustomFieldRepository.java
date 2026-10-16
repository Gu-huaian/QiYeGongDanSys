package com.example.qigd_ljl.repository;

import com.example.qigd_ljl.entity.TicketCustomField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketCustomFieldRepository extends JpaRepository<TicketCustomField, Long> {

    List<TicketCustomField> findByEnabledTrueOrderBySortOrderAsc();

    boolean existsByFieldKey(String fieldKey);
}