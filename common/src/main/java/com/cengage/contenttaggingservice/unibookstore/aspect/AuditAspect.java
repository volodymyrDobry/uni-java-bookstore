package com.cengage.contenttaggingservice.unibookstore.aspect;

import com.cengage.contenttaggingservice.unibookstore.annotation.Auditable;
import com.cengage.contenttaggingservice.unibookstore.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private static final String ANONYMOUS = "anonymous";

    private final SecurityUtils securityUtils;

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void auditSuccess(JoinPoint joinPoint, Auditable auditable, Object result) {
        log.info("AUDIT action={} status=SUCCESS user={} method={}",
                auditable.value(), resolveUser(), joinPoint.getSignature().toShortString());
    }

    @AfterThrowing(pointcut = "@annotation(auditable)", throwing = "ex")
    public void auditFailure(JoinPoint joinPoint, Auditable auditable, Throwable ex) {
        log.warn("AUDIT action={} status=FAILURE user={} method={} error={}",
                auditable.value(), resolveUser(), joinPoint.getSignature().toShortString(), ex.toString());
    }

    private String resolveUser() {
        try {
            return securityUtils.getCurrentUser().username();
        } catch (RuntimeException ex) {
            return ANONYMOUS;
        }
    }
}
