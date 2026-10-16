package com.example.qigd_ljl.repository;

import com.example.qigd_ljl.entity.TicketAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {

    List<TicketAttachment> findByTicket_IdOrderByCreatedAtDesc(Long ticketId);
}