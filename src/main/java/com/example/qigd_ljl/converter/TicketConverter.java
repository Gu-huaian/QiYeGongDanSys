package com.example.qigd_ljl.converter;

import com.example.qigd_ljl.dto.category.CategoryResponse;
import com.example.qigd_ljl.dto.priority.PriorityResponse;
import com.example.qigd_ljl.dto.ticket.*;
import com.example.qigd_ljl.entity.*;

import java.util.List;
import java.util.stream.Collectors;

public class TicketConverter {

    public static TicketResponse toResponse(Ticket ticket) {
        if (ticket == null) return null;

        TicketResponse response = new TicketResponse();
        response.setId(ticket.getId());
        response.setTicketNo(ticket.getTicketNo());
        response.setTitle(ticket.getTitle());
        response.setDescription(ticket.getDescription());
        response.setStatus(ticket.getStatus());
        response.setStatusLabel(ticket.getStatus() != null ? ticket.getStatus().getLabel() : null);
        response.setAssignmentMode(ticket.getAssignmentMode());
        response.setAssignmentModeLabel(ticket.getAssignmentMode() != null ? ticket.getAssignmentMode().getLabel() : null);
        response.setDueTime(ticket.getDueTime());
        response.setResolvedTime(ticket.getResolvedTime());
        response.setClosedTime(ticket.getClosedTime());
        response.setOverdue(ticket.getOverdue());
        response.setCustomFields(ticket.getCustomFields());
        response.setCreatedAt(ticket.getCreatedAt());
        response.setUpdatedAt(ticket.getUpdatedAt());

        if (ticket.getCategory() != null) {
            response.setCategory(new TicketResponse.CategorySimpleResponse(
                    ticket.getCategory().getId(), ticket.getCategory().getName()));
        }

        if (ticket.getPriority() != null) {
            response.setPriority(new TicketResponse.PrioritySimpleResponse(
                    ticket.getPriority().getId(), ticket.getPriority().getName(),
                    ticket.getPriority().getColor(), ticket.getPriority().getLevel()));
        }

        if (ticket.getCreator() != null) {
            response.setCreator(new TicketResponse.UserSimpleResponse(
                    ticket.getCreator().getId(), ticket.getCreator().getUsername(), ticket.getCreator().getRealName()));
        }

        if (ticket.getAssignee() != null) {
            response.setAssignee(new TicketResponse.UserSimpleResponse(
                    ticket.getAssignee().getId(), ticket.getAssignee().getUsername(), ticket.getAssignee().getRealName()));
        }

        if (ticket.getCollaborators() != null && !ticket.getCollaborators().isEmpty()) {
            response.setCollaborators(ticket.getCollaborators().stream()
                    .map(u -> new TicketResponse.UserSimpleResponse(
                            u.getId(), u.getUsername(), u.getRealName()))
                    .collect(Collectors.toList()));
        }

        return response;
    }

    public static CommentResponse toCommentResponse(TicketComment comment) {
        if (comment == null) return null;

        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setInternal(comment.getInternal());
        response.setCreatedAt(comment.getCreatedAt());

        if (comment.getUser() != null) {
            response.setUser(new CommentResponse.UserSimpleResponse(
                    comment.getUser().getId(), comment.getUser().getUsername(), comment.getUser().getRealName()));
        }

        return response;
    }

    public static List<CommentResponse> toCommentResponseList(List<TicketComment> comments) {
        if (comments == null) return List.of();
        return comments.stream().map(TicketConverter::toCommentResponse).collect(Collectors.toList());
    }

    public static AttachmentResponse toAttachmentResponse(TicketAttachment attachment) {
        if (attachment == null) return null;

        AttachmentResponse response = new AttachmentResponse();
        response.setId(attachment.getId());
        response.setFileName(attachment.getFileName());
        response.setOriginalName(attachment.getOriginalName());
        response.setContentType(attachment.getContentType());
        response.setFileSize(attachment.getFileSize());
        response.setFilePath(attachment.getFilePath());
        response.setCreatedAt(attachment.getCreatedAt());

        if (attachment.getUploader() != null) {
            response.setUploader(new AttachmentResponse.UserSimpleResponse(
                    attachment.getUploader().getId(), attachment.getUploader().getUsername(), attachment.getUploader().getRealName()));
        }

        return response;
    }

    public static PriorityResponse toPriorityResponse(TicketPriority priority) {
        if (priority == null) return null;

        PriorityResponse response = new PriorityResponse();
        response.setId(priority.getId());
        response.setName(priority.getName());
        response.setCode(priority.getCode());
        response.setLevel(priority.getLevel());
        response.setColor(priority.getColor());
        response.setDescription(priority.getDescription());
        response.setEnabled(priority.getEnabled());
        response.setCreatedAt(priority.getCreatedAt());
        response.setUpdatedAt(priority.getUpdatedAt());
        return response;
    }

    public static CategoryResponse toCategoryResponse(TicketCategory category) {
        if (category == null) return null;

        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setCode(category.getCode());
        response.setDescription(category.getDescription());
        response.setParentId(category.getParentId());
        response.setEnabled(category.getEnabled());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());
        return response;
    }
}