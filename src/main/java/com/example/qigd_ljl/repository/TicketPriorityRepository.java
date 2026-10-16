package com.example.qigd_ljl.repository;

import com.example.qigd_ljl.entity.TicketPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketPriorityRepository extends JpaRepository<TicketPriority, Long> {

    List<TicketPriority> findByEnabledTrueOrderByLevelAsc();

    boolean existsByCode(String code);
}