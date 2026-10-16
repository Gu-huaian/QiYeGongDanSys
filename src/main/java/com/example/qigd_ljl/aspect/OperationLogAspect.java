package com.example.qigd_ljl.aspect;

import com.example.qigd_ljl.config.CurrentUserProvider;
import com.example.qigd_ljl.entity.OperationLog;
import com.example.qigd_ljl.entity.User;
import com.example.qigd_ljl.repository.OperationLogRepository;
import com.example.qigd_ljl.repository.UserRepository;
import com.example.qigd_ljl.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogRepository logRepository;
    private final CurrentUserProvider currentUserProvider;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(com.example.qigd_ljl.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = point.getSignature().toShortString();

        OperationLog operationLog = new OperationLog();
        operationLog.setMethod(methodName);

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            operationLog.setRequestUrl(request.getRequestURI());
            operationLog.setRequestMethod(request.getMethod());
            operationLog.setIp(getClientIp(request));
            operationLog.setUserAgent(request.getHeader("User-Agent"));
        }

        CustomUserDetails currentUser = currentUserProvider.getCurrentUser();
        if (currentUser != null) {
            Optional<User> userOpt = userRepository.findById(currentUser.getId());
            userOpt.ifPresent(operationLog::setUser);
        }

        try {
            Object result = point.proceed();
            operationLog.setStatus(1);
            try {
                operationLog.setResponseResult(objectMapper.writeValueAsString(result));
            } catch (Exception ignored) {
            }
            return result;
        } catch (Throwable ex) {
            operationLog.setStatus(0);
            operationLog.setResponseResult(ex.getMessage());
            throw ex;
        } finally {
            operationLog.setCostTime(System.currentTimeMillis() - startTime);
            saveLog(operationLog);
        }
    }

    @Async
    protected void saveLog(OperationLog operationLog) {
        try {
            logRepository.save(operationLog);
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}