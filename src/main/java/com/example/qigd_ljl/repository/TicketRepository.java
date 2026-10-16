package com.example.qigd_ljl.repository;

import com.example.qigd_ljl.entity.Ticket;
import com.example.qigd_ljl.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    Optional<Ticket> findByTicketNo(String ticketNo);

    boolean existsByTicketNo(String ticketNo);

    Page<Ticket> findByCreator_Id(Long creatorId, Pageable pageable);

    Page<Ticket> findByAssignee_Id(Long assigneeId, Pageable pageable);

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE t.creator.id = :userId OR t.assignee.id = :userId")
    Page<Ticket> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE t.overdue = true AND t.status NOT IN :closedStatuses")
    List<Ticket> findOverdueTickets(@Param("closedStatuses") List<TicketStatus> closedStatuses);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = :status")
    Long countByStatus(@Param("status") TicketStatus status);

    @Query("SELECT t.status, COUNT(t) FROM Ticket t GROUP BY t.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.creator.id = :creatorId AND t.createdAt BETWEEN :start AND :end")
    Long countByCreatorIdAndCreatedAtBetween(@Param("creatorId") Long creatorId,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignee.id = :assigneeId AND t.status IN :statuses")
    Long countByAssigneeIdAndStatusIn(@Param("assigneeId") Long assigneeId,
                                      @Param("statuses") List<TicketStatus> statuses);
}