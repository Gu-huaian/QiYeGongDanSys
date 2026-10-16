package com.example.qigd_ljl.dto.ticket;

import com.example.qigd_ljl.entity.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResponse {

    private Long totalCount;
    private Map<TicketStatus, Long> statusCount;
    private Long overdueCount;
    private Double averageResolveHours;
    private Map<String, Long> categoryCount;
    private Map<String, Long> priorityCount;
    private Map<String, Long> assigneeCount;
    private List<Map<String, Object>> dailyTrend;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStat {
        private String date;
        private Long created;
        private Long resolved;
    }
}