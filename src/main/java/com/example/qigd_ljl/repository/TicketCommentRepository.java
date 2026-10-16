package com.example.qigd_ljl.repository;

import com.example.qigd_ljl.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

    List<TicketComment> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);

    int countByTicket_Id(Long ticketId);
}