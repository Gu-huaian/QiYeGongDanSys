package com.example.qigd_ljl.controller;

import com.example.qigd_ljl.annotation.OperationLog;
import com.example.qigd_ljl.common.PageResult;
import com.example.qigd_ljl.common.Result;
import com.example.qigd_ljl.dto.ticket.*;
import com.example.qigd_ljl.entity.AssignmentMode;
import com.example.qigd_ljl.entity.TicketStatus;
import com.example.qigd_ljl.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Tag(name = "工单管理", description = "工单全流程管理：创建、分配、处理、跟踪、关闭")
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    @Operation(summary = "搜索工单（支持多条件筛选、分页、排序）")
    public Result<PageResult<TicketResponse>> searchTickets(
            @Parameter(description = "关键词（标题/描述/工单号）") @RequestParam(required = false) String keyword,
            @Parameter(description = "工单状态") @RequestParam(required = false) TicketStatus status,
            @Parameter(description = "分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "优先级ID") @RequestParam(required = false) Long priorityId,
            @Parameter(description = "处理人ID") @RequestParam(required = false) Long assigneeId,
            @Parameter(description = "创建人ID") @RequestParam(required = false) Long creatorId,
            @Parameter(description = "是否逾期") @RequestParam(required = false) Boolean overdue,
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "排序方向 asc/desc") @RequestParam(defaultValue = "desc") String sortDirection) {
        TicketQueryRequest request = new TicketQueryRequest();
        request.setKeyword(keyword);
        request.setStatus(status);
        request.setCategoryId(categoryId);
        request.setPriorityId(priorityId);
        request.setAssigneeId(assigneeId);
        request.setCreatorId(creatorId);
        request.setOverdue(overdue);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);
        return Result.success(ticketService.search(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取工单详情")
    public Result<TicketResponse> getTicket(@PathVariable Long id) {
        return Result.success(ticketService.getById(id));
    }

    @PostMapping
    @Operation(summary = "创建工单")
    @OperationLog(module = "工单管理", operation = "创建工单")
    public Result<TicketResponse> createTicket(@Valid @RequestBody TicketCreateRequest request) {
        return Result.success(ticketService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新工单信息")
    @OperationLog(module = "工单管理", operation = "更新工单")
    public Result<TicketResponse> updateTicket(@PathVariable Long id, @RequestBody TicketUpdateRequest request) {
        return Result.success(ticketService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除工单")
    @OperationLog(module = "工单管理", operation = "删除工单")
    public Result<Void> deleteTicket(@PathVariable Long id) {
        ticketService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "分配工单（手动/自动）")
    @OperationLog(module = "工单管理", operation = "分配工单")
    public Result<TicketResponse> assignTicket(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long assigneeId = Long.valueOf(body.get("assigneeId").toString());
        AssignmentMode mode = body.containsKey("mode")
                ? AssignmentMode.valueOf(body.get("mode").toString())
                : AssignmentMode.MANUAL;
        return Result.success(ticketService.assign(id, assigneeId, mode));
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "变更工单状态")
    @OperationLog(module = "工单管理", operation = "变更工单状态")
    public Result<TicketResponse> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        TicketStatus newStatus = TicketStatus.valueOf(body.get("status"));
        return Result.success(ticketService.changeStatus(id, newStatus));
    }

    @PostMapping("/{id}/transfer")
    @Operation(summary = "工单转出（转派给其他处理人）")
    @OperationLog(module = "工单管理", operation = "工单转出")
    public Result<TicketResponse> transfer(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long newAssigneeId = Long.valueOf(body.get("assigneeId").toString());
        return Result.success(ticketService.transfer(id, newAssigneeId));
    }

    @PostMapping("/{id}/collaborators")
    @Operation(summary = "邀请协作（邀请他人协助处理）")
    @OperationLog(module = "工单管理", operation = "邀请协作")
    public Result<TicketResponse> inviteCollaborator(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long userId = Long.valueOf(body.get("userId").toString());
        return Result.success(ticketService.inviteCollaborator(id, userId));
    }

    @DeleteMapping("/{id}/collaborators/{userId}")
    @Operation(summary = "移除协作成员")
    @OperationLog(module = "工单管理", operation = "移除协作成员")
    public Result<TicketResponse> removeCollaborator(@PathVariable Long id, @PathVariable Long userId) {
        return Result.success(ticketService.removeCollaborator(id, userId));
    }

    @PostMapping("/{id}/escalate")
    @Operation(summary = "向上级反馈工单")
    @OperationLog(module = "工单管理", operation = "向上级反馈")
    public Result<TicketResponse> escalate(@PathVariable Long id) {
        return Result.success(ticketService.escalate(id));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "添加工单评论")
    @OperationLog(module = "工单管理", operation = "添加评论")
    public Result<CommentResponse> addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest request) {
        return Result.success(ticketService.addComment(id, request));
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "获取工单评论列表")
    public Result<java.util.List<CommentResponse>> listComments(@PathVariable Long id) {
        return Result.success(ticketService.listComments(id));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "删除评论（仅作者本人或管理员）")
    @OperationLog(module = "工单管理", operation = "删除评论")
    public Result<Void> deleteComment(@PathVariable Long commentId) {
        ticketService.deleteComment(commentId);
        return Result.success();
    }

    @PostMapping("/{id}/attachments")
    @Operation(summary = "上传附件")
    @OperationLog(module = "工单管理", operation = "上传附件")
    public Result<AttachmentResponse> uploadAttachment(@PathVariable Long id,
                                                       @RequestParam("file") MultipartFile file) {
        return Result.success(ticketService.uploadAttachment(id, file));
    }

    @GetMapping("/{id}/attachments")
    @Operation(summary = "获取工单附件列表")
    public Result<java.util.List<AttachmentResponse>> listAttachments(@PathVariable Long id) {
        return Result.success(ticketService.listAttachments(id));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @Operation(summary = "删除附件")
    @OperationLog(module = "工单管理", operation = "删除附件")
    public Result<Void> deleteAttachment(@PathVariable Long attachmentId) {
        ticketService.deleteAttachment(attachmentId);
        return Result.success();
    }

    @GetMapping("/statistics")
    @Operation(summary = "工单统计分析", description = "多维度统计：状态分布、分类分布、优先级分布、处理人负载、逾期数量、平均解决时长")
    public Result<StatisticsResponse> getStatistics(
            @Parameter(description = "分类ID筛选") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "优先级ID筛选") @RequestParam(required = false) Long priorityId,
            @Parameter(description = "处理人ID筛选") @RequestParam(required = false) Long assigneeId,
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        return Result.success(ticketService.getStatistics(categoryId, priorityId, assigneeId, startTime, endTime));
    }
}