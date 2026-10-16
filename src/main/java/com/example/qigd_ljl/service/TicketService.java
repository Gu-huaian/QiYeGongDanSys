package com.example.qigd_ljl.service;

import com.example.qigd_ljl.common.BusinessException;
import com.example.qigd_ljl.common.ErrorCode;
import com.example.qigd_ljl.common.PageResult;
import com.example.qigd_ljl.converter.TicketConverter;
import com.example.qigd_ljl.config.CurrentUserProvider;
import com.example.qigd_ljl.dto.ticket.*;
import com.example.qigd_ljl.entity.*;
import com.example.qigd_ljl.repository.*;
import com.example.qigd_ljl.security.CustomUserDetails;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketCategoryRepository categoryRepository;
    private final TicketPriorityRepository priorityRepository;
    private final TicketCommentRepository commentRepository;
    private final TicketAttachmentRepository attachmentRepository;
    private final TicketCustomFieldRepository customFieldRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    private static final java.util.Map<TicketStatus, java.util.Set<TicketStatus>> VALID_TRANSITIONS = java.util.Map.of(
            TicketStatus.PENDING, java.util.Set.of(TicketStatus.ASSIGNED, TicketStatus.PROCESSING, TicketStatus.CLOSED),
            TicketStatus.ASSIGNED, java.util.Set.of(TicketStatus.PROCESSING, TicketStatus.PENDING, TicketStatus.CLOSED),
            TicketStatus.PROCESSING, java.util.Set.of(TicketStatus.PAUSED, TicketStatus.RESOLVED, TicketStatus.CLOSED),
            TicketStatus.PAUSED, java.util.Set.of(TicketStatus.PROCESSING, TicketStatus.CLOSED),
            TicketStatus.RESOLVED, java.util.Set.of(TicketStatus.CLOSED, TicketStatus.REOPENED),
            TicketStatus.REOPENED, java.util.Set.of(TicketStatus.PROCESSING, TicketStatus.CLOSED)
    );

    public PageResult<TicketResponse> search(TicketQueryRequest request) {
        CustomUserDetails currentUser = currentUserProvider.getCurrentUser();

        Specification<Ticket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(request.getKeyword())) {
                String keyword = "%" + request.getKeyword() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("title"), keyword),
                        cb.like(root.get("description"), keyword),
                        cb.like(root.get("ticketNo"), keyword)
                ));
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }
            if (request.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), request.getCategoryId()));
            }
            if (request.getPriorityId() != null) {
                predicates.add(cb.equal(root.get("priority").get("id"), request.getPriorityId()));
            }
            if (request.getAssigneeId() != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), request.getAssigneeId()));
            }
            if (request.getCreatorId() != null) {
                predicates.add(cb.equal(root.get("creator").get("id"), request.getCreatorId()));
            }
            if (request.getOverdue() != null && request.getOverdue()) {
                predicates.add(cb.equal(root.get("overdue"), true));
                predicates.add(root.get("status").in(java.util.Arrays.asList(TicketStatus.CLOSED, TicketStatus.RESOLVED)).not());
            }
            if (request.getStartTime() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.getStartTime()));
            }
            if (request.getEndTime() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.getEndTime()));
            }

            // 数据权限过滤：非管理员只能看到"自己创建"、"被分配给自己处理"或"自己参与协作"的工单
            if (!isAdmin(currentUser)) {
                Predicate mine = cb.or(
                        cb.equal(root.get("creator").get("id"), currentUser.getId()),
                        cb.equal(root.get("assignee").get("id"), currentUser.getId()),
                        cb.equal(root.join("collaborators", JoinType.LEFT).get("id"), currentUser.getId())
                );
                predicates.add(mine);
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(request.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                request.getSortBy() != null ? request.getSortBy() : "createdAt"
        );

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        Page<Ticket> page = ticketRepository.findAll(spec, pageable);

        List<TicketResponse> responses = page.getContent().stream()
                .map(TicketConverter::toResponse)
                .collect(Collectors.toList());

        return PageResult.of(responses, page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
    }

    public TicketResponse getById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));

        // 数据权限：非管理员只能查看自己创建或被分配处理的工单
        CustomUserDetails currentUser = currentUserProvider.getCurrentUser();
        if (!isAdmin(currentUser)) {
            boolean related = (ticket.getCreator() != null && ticket.getCreator().getId().equals(currentUser.getId()))
                    || (ticket.getAssignee() != null && ticket.getAssignee().getId().equals(currentUser.getId()))
                    || (ticket.getCollaborators() != null && ticket.getCollaborators().stream()
                    .anyMatch(u -> u.getId().equals(currentUser.getId())));
            if (!related) {
                throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "无权查看此工单");
            }
        }
        return TicketConverter.toResponse(ticket);
    }

    @Transactional
    public TicketResponse create(TicketCreateRequest request) {
        CustomUserDetails currentUser = currentUserProvider.getCurrentUser();
        User creator = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Ticket ticket = new Ticket();
        ticket.setTicketNo(generateTicketNo());
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setStatus(TicketStatus.PENDING);
        ticket.setCreator(creator);

        if (request.getCategoryId() != null) {
            TicketCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
            ticket.setCategory(category);
        }

        if (request.getPriorityId() != null) {
            TicketPriority priority = priorityRepository.findById(request.getPriorityId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRIORITY_NOT_FOUND));
            ticket.setPriority(priority);
        }

        ticket.setDueTime(request.getDueTime());
        ticket.setCustomFields(request.getCustomFields());

        if (request.getAssignmentMode() == null) {
            request.setAssignmentMode(AssignmentMode.MANUAL);
        }
        ticket.setAssignmentMode(request.getAssignmentMode());

        if (request.getAssignmentMode() == AssignmentMode.MANUAL && request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            ticket.setAssignee(assignee);
            ticket.setStatus(TicketStatus.ASSIGNED);
        } else if (request.getAssignmentMode() == AssignmentMode.AUTO) {
            autoAssign(ticket);
        }

        Ticket saved = ticketRepository.save(ticket);
        log.info("工单创建成功: {}", saved.getTicketNo());
        return TicketConverter.toResponse(saved);
    }

    @Transactional
    public TicketResponse update(Long id, TicketUpdateRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessException(ErrorCode.TICKET_ALREADY_CLOSED);
        }

        if (request.getTitle() != null) ticket.setTitle(request.getTitle());
        if (request.getDescription() != null) ticket.setDescription(request.getDescription());
        if (request.getDueTime() != null) ticket.setDueTime(request.getDueTime());
        if (request.getCustomFields() != null) ticket.setCustomFields(request.getCustomFields());

        if (request.getCategoryId() != null) {
            TicketCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
            ticket.setCategory(category);
        }

        if (request.getPriorityId() != null) {
            TicketPriority priority = priorityRepository.findById(request.getPriorityId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRIORITY_NOT_FOUND));
            ticket.setPriority(priority);
        }

        Ticket saved = ticketRepository.save(ticket);
        return TicketConverter.toResponse(saved);
    }

    @Transactional
    public TicketResponse assign(Long id, Long assigneeId, AssignmentMode mode) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));

        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessException(ErrorCode.TICKET_ALREADY_CLOSED);
        }

        User assignee = userRepository.findById(assigneeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ticket.setAssignee(assignee);
        ticket.setAssignmentMode(mode != null ? mode : AssignmentMode.MANUAL);

        if (ticket.getStatus() == TicketStatus.PENDING) {
            ticket.setStatus(TicketStatus.ASSIGNED);
        }

        Ticket saved = ticketRepository.save(ticket);
        return TicketConverter.toResponse(saved);
    }

    private static final java.util.Map<TicketStatus, String> STATUS_CN = java.util.Map.of(
            TicketStatus.PENDING, "待处理",
            TicketStatus.ASSIGNED, "已分配",
            TicketStatus.PROCESSING, "处理中",
            TicketStatus.PAUSED, "已暂停",
            TicketStatus.RESOLVED, "已解决",
            TicketStatus.CLOSED, "已关闭",
            TicketStatus.REOPENED, "已重新打开");

    private static String statusCn(TicketStatus status) {
        return status != null ? STATUS_CN.getOrDefault(status, status.name()) : "";
    }

    private static boolean isAdmin(CustomUserDetails user) {
        if (user == null || user.getAuthorities() == null) return false;
        return user.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    public TicketResponse changeStatus(Long id, TicketStatus newStatus) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));

        if (newStatus == null) {
            throw new BusinessException("目标状态不能为空，请选择要变更到的状态");
        }

        java.util.Set<TicketStatus> allowed = VALID_TRANSITIONS.getOrDefault(ticket.getStatus(), java.util.Set.of());
        if (!allowed.contains(newStatus)) {
            // 人性化提示：用中文状态名展示"从哪到哪"，并在末尾附带可选目标供用户参考
            String candidates = allowed.stream()
                    .map(TicketService::statusCn)
                    .collect(Collectors.joining("、"));
            String hint = candidates.isEmpty()
                    ? "当前状态下无法继续操作"
                    : "当前状态可流转到：" + candidates;
            throw new BusinessException(ErrorCode.TICKET_STATUS_ERROR.getCode(),
                    "工单状态不允许从【" + statusCn(ticket.getStatus()) + "】变更为【" + statusCn(newStatus) +
                            "】。" + hint);
        }

        ticket.setStatus(newStatus);

        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedTime(LocalDateTime.now());
        }
        if (newStatus == TicketStatus.CLOSED) {
            ticket.setClosedTime(LocalDateTime.now());
        }

        Ticket saved = ticketRepository.save(ticket);
        return TicketConverter.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!ticketRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.TICKET_NOT_FOUND);
        }
        ticketRepository.deleteById(id);
    }

    @Transactional
    public CommentResponse addComment(Long ticketId, CommentRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));

        CustomUserDetails currentUser = currentUserProvider.getCurrentUser();
        User user = userRepository.findById(currentUser.getId()).orElse(null);

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setUser(user);
        comment.setContent(request.getContent());
        comment.setInternal(request.getInternal() != null ? request.getInternal() : false);

        TicketComment saved = commentRepository.save(comment);
        return TicketConverter.toCommentResponse(saved);
    }

    public List<CommentResponse> listComments(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new BusinessException(ErrorCode.TICKET_NOT_FOUND);
        }
        return TicketConverter.toCommentResponseList(commentRepository.findByTicket_IdOrderByCreatedAtAsc(ticketId));
    }

    @Transactional
    public void deleteComment(Long commentId) {
        TicketComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException("评论不存在或已被删除"));

        CustomUserDetails currentUser = currentUserProvider.getCurrentUser();
        // 权限校验：评论作者本人或管理员可删除
        boolean isCommentOwner = comment.getUser() != null
                && currentUser != null && comment.getUser().getId().equals(currentUser.getId());
        if (!isCommentOwner && !isAdmin(currentUser)) {
            throw new BusinessException(ErrorCode.FORBIDDEN.getCode(), "只能删除自己发布的评论");
        }
        commentRepository.deleteById(commentId);
    }

    @Transactional
    public AttachmentResponse uploadAttachment(Long ticketId, MultipartFile file) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));

        // 严谨性校验：文件非空、大小上限（默认 10MB）、拦截危险可执行扩展名
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件，不能为空");
        }
        if (file.getSize() > 10L * 1024 * 1024) {
            throw new BusinessException("附件大小不能超过 10MB，请压缩后重新上传");
        }

        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "未命名文件";
        String ext = originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf(".")).toLowerCase()
                : "";
        java.util.Set<String> blockedExt = java.util.Set.of(
                ".exe", ".bat", ".cmd", ".sh", ".dll", ".jar", ".jsp", ".jspx",
                ".php", ".php3", ".php4", ".php5", ".phtml", ".asp", ".aspx", ".py");
        if (blockedExt.contains(ext)) {
            throw new BusinessException("不支持上传 " + ext + " 格式的文件，请选择文档、图片或压缩包");
        }

        CustomUserDetails currentUser = currentUserProvider.getCurrentUser();
        User uploader = userRepository.findById(currentUser.getId()).orElse(null);

        try {
            String baseDir = System.getProperty("user.dir", ".");
            String uploadDir = baseDir + "/uploads/tickets/" + ticketId;
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;

            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            TicketAttachment attachment = new TicketAttachment();
            attachment.setTicket(ticket);
            attachment.setUploader(uploader);
            attachment.setFileName(fileName);
            attachment.setOriginalName(originalName);
            attachment.setContentType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setFilePath(uploadDir + "/" + fileName);

            TicketAttachment saved = attachmentRepository.save(attachment);
            return TicketConverter.toAttachmentResponse(saved);

        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }

    public List<AttachmentResponse> listAttachments(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new BusinessException(ErrorCode.TICKET_NOT_FOUND);
        }
        return attachmentRepository.findByTicket_IdOrderByCreatedAtDesc(ticketId).stream()
                .map(TicketConverter::toAttachmentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteAttachment(Long id) {
        TicketAttachment attachment = attachmentRepository.findById(id).orElse(null);
        if (attachment == null) {
            throw new BusinessException("附件不存在或已被删除");
        }
        // 同步删除磁盘上的实际文件，避免留下无法访问的孤儿文件
        if (StringUtils.hasText(attachment.getFilePath())) {
            try {
                Path filePath = Paths.get(attachment.getFilePath()).normalize();
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.warn("删除附件磁盘文件失败: {}", attachment.getFilePath(), e);
            }
        }
        attachmentRepository.deleteById(id);
    }

    public StatisticsResponse getStatistics(Long categoryId, Long priorityId, Long assigneeId,
                                            LocalDateTime startTime, LocalDateTime endTime) {
        CustomUserDetails currentUser = currentUserProvider.getCurrentUser();
        Specification<Ticket> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (priorityId != null) {
                predicates.add(cb.equal(root.get("priority").get("id"), priorityId));
            }
            if (assigneeId != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), assigneeId));
            }
            if (startTime != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startTime));
            }
            if (endTime != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endTime));
            }
            // 数据隔离：非管理员只看与自己相关的工单
            if (!isAdmin(currentUser)) {
                Predicate mine = cb.or(
                        cb.equal(root.get("creator").get("id"), currentUser.getId()),
                        cb.equal(root.get("assignee").get("id"), currentUser.getId()),
                        cb.equal(root.join("collaborators", JoinType.LEFT).get("id"), currentUser.getId())
                );
                predicates.add(mine);
                query.distinct(true);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Ticket> tickets = ticketRepository.findAll(spec);

        long totalCount = tickets.size();
        Map<TicketStatus, Long> statusCount = tickets.stream()
                .collect(Collectors.groupingBy(Ticket::getStatus, Collectors.counting()));
        long overdueCount = tickets.stream()
                .filter(t -> Boolean.TRUE.equals(t.getOverdue()))
                .count();

        Map<String, Long> categoryCount = tickets.stream()
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(t -> t.getCategory().getName(), Collectors.counting()));

        Map<String, Long> priorityCount = tickets.stream()
                .filter(t -> t.getPriority() != null)
                .collect(Collectors.groupingBy(t -> t.getPriority().getName(), Collectors.counting()));

        Map<String, Long> assigneeCount = tickets.stream()
                .filter(t -> t.getAssignee() != null)
                .collect(Collectors.groupingBy(t -> t.getAssignee().getRealName(), Collectors.counting()));

        double avgResolveHours = tickets.stream()
                .filter(t -> t.getResolvedTime() != null && t.getCreatedAt() != null)
                .mapToLong(t -> java.time.Duration.between(t.getCreatedAt(), t.getResolvedTime()).toHours())
                .average()
                .orElse(0.0);

        // 近7天每日新增/解决趋势（用于折线图）
        List<Map<String, Object>> dailyTrend = buildDailyTrend(tickets);

        StatisticsResponse resp = new StatisticsResponse(
                totalCount, statusCount, overdueCount,
                Math.round(avgResolveHours * 100.0) / 100.0,
                categoryCount, priorityCount, assigneeCount, dailyTrend);
        return resp;
    }

    private List<Map<String, Object>> buildDailyTrend(List<Ticket> tickets) {
        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            String key = day.toString();
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime dayEnd = day.plusDays(1).atStartOfDay();

            long created = tickets.stream()
                    .filter(t -> t.getCreatedAt() != null
                            && !t.getCreatedAt().isBefore(dayStart) && t.getCreatedAt().isBefore(dayEnd))
                    .count();
            long resolved = tickets.stream()
                    .filter(t -> t.getResolvedTime() != null
                            && !t.getResolvedTime().isBefore(dayStart) && t.getResolvedTime().isBefore(dayEnd))
                    .count();

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", day.format(fmt));
            item.put("created", created);
            item.put("resolved", resolved);
            result.add(item);
        }
        return result;
    }

    @Transactional
    public TicketResponse transfer(Long id, Long newAssigneeId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessException(ErrorCode.TICKET_ALREADY_CLOSED);
        }
        User newAssignee = userRepository.findById(newAssigneeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        CustomUserDetails currentUser = currentUserProvider.getCurrentUser();
        User operator = currentUser != null ? userRepository.findById(currentUser.getId()).orElse(null) : null;

        String oldName = ticket.getAssignee() != null ? ticket.getAssignee().getRealName() : "未分配";
        if (ticket.getAssignee() != null && ticket.getAssignee().getId().equals(newAssignee.getId())) {
            throw new BusinessException("目标处理人与当前处理人相同，无需转出");
        }
        ticket.setAssignee(newAssignee);
        ticket.setAssignmentMode(AssignmentMode.MANUAL);
        if (ticket.getStatus() == TicketStatus.PENDING) {
            ticket.setStatus(TicketStatus.ASSIGNED);
        }

        String remark = "【工单转出】处理人由【" + oldName + "】转出至【" + newAssignee.getRealName() + "】";
        if (operator != null) {
            remark += "（操作人：" + operator.getRealName() + "）";
        }
        commentRepository.save(buildSystemComment(ticket, remark));

        Ticket saved = ticketRepository.save(ticket);
        return TicketConverter.toResponse(saved);
    }

    @Transactional
    public TicketResponse inviteCollaborator(Long id, Long userId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessException(ErrorCode.TICKET_ALREADY_CLOSED);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Set<User> collaborators = ticket.getCollaborators();
        if (collaborators == null) {
            collaborators = new HashSet<>();
            ticket.setCollaborators(collaborators);
        }
        boolean isAssignee = ticket.getAssignee() != null && ticket.getAssignee().getId().equals(userId);
        boolean isCreator = ticket.getCreator() != null && ticket.getCreator().getId().equals(userId);
        if (isAssignee || isCreator) {
            throw new BusinessException("该用户已是工单的处理人或创建人，无需协助");
        }
        if (collaborators.stream().anyMatch(u -> u.getId().equals(userId))) {
            throw new BusinessException("该用户已在该工单的协作成员中");
        }
        collaborators.add(user);

        commentRepository.save(buildSystemComment(ticket, "【邀请协助】已邀请【" + user.getRealName() + "】协助处理本工单"));

        Ticket saved = ticketRepository.save(ticket);
        return TicketConverter.toResponse(saved);
    }

    @Transactional
    public TicketResponse removeCollaborator(Long id, Long userId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));
        Set<User> collaborators = ticket.getCollaborators();
        if (collaborators != null) {
            collaborators.removeIf(u -> u.getId().equals(userId));
        }
        commentRepository.save(buildSystemComment(ticket, "【协作管理】已移除协作成员"));

        Ticket saved = ticketRepository.save(ticket);
        return TicketConverter.toResponse(saved);
    }

    @Transactional
    public TicketResponse escalate(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessException(ErrorCode.TICKET_ALREADY_CLOSED);
        }
        CustomUserDetails currentUser = currentUserProvider.getCurrentUser();
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        User operator = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        User supervisor = operator.getSupervisor();
        if (supervisor == null) {
            throw new BusinessException("您没有配置直属上级，无法向上级反馈，请联系管理员设置上级");
        }
        if (ticket.getAssignee() != null && ticket.getAssignee().getId().equals(supervisor.getId())) {
            throw new BusinessException("该工单已由您的直属上级处理，无需再次反馈");
        }

        String oldName = ticket.getAssignee() != null ? ticket.getAssignee().getRealName() : "未分配";
        ticket.setAssignee(supervisor);
        ticket.setAssignmentMode(AssignmentMode.MANUAL);
        if (ticket.getStatus() == TicketStatus.PENDING) {
            ticket.setStatus(TicketStatus.ASSIGNED);
        }

        commentRepository.save(buildSystemComment(ticket,
                "【向上级反馈】由【" + operator.getRealName() + "】向直属上级【" + supervisor.getRealName() + "】反馈本工单（原处理人：" + oldName + "）"));

        Ticket saved = ticketRepository.save(ticket);
        return TicketConverter.toResponse(saved);
    }

    private TicketComment buildSystemComment(Ticket ticket, String content) {
        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setUser(null);
        comment.setContent(content);
        comment.setInternal(true);
        return comment;
    }

    private String generateTicketNo() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", new Random().nextInt(10000));
        String no = "TK" + datePart + randomPart;
        if (ticketRepository.existsByTicketNo(no)) {
            return generateTicketNo();
        }
        return no;
    }

    private void autoAssign(Ticket ticket) {
        // 智能分配：优先分配给"有工单处理能力"的角色（AGENT/MANAGER），
        // 并在候选者间做负载均衡，选择当前未完成处理中工单最少的用户；
        // 若没有符合角色的处理人，再随机兜底到任一启用用户。
        List<String> handlerRoleCodes = List.of("AGENT", "MANAGER");
        List<User> candidates = userRepository.findByEnabledAndRolesIn(true, handlerRoleCodes);
        User selected = null;

        if (candidates == null || candidates.isEmpty()) {
            // 兜底：从所有启用用户中随机选择
            List<User> fallback = userRepository.findAll().stream()
                    .filter(User::getEnabled)
                    .collect(Collectors.toList());
            if (!fallback.isEmpty()) {
                selected = fallback.get(new Random().nextInt(fallback.size()));
            }
        } else if (candidates.size() == 1) {
            selected = candidates.get(0);
        } else {
            // 负载均衡：比较各自正在进行中（处理中、待处理、已分配）的工单数量
            List<TicketStatus> activeStatuses = List.of(
                    TicketStatus.PENDING, TicketStatus.ASSIGNED, TicketStatus.PROCESSING);
            User leastBusy = null;
            Long minLoad = Long.MAX_VALUE;
            for (User candidate : candidates) {
                Long load = ticketRepository.countByAssigneeIdAndStatusIn(candidate.getId(), activeStatuses);
                if (leastBusy == null || load < minLoad) {
                    leastBusy = candidate;
                    minLoad = load;
                }
            }
            selected = leastBusy;
        }

        if (selected != null) {
            ticket.setAssignee(selected);
        }
    }
}