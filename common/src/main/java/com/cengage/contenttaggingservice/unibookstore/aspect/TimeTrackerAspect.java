package com.cengage.contenttaggingservice.unibookstore.aspect;

import com.cengage.contenttaggingservice.unibookstore.annotation.TimeTracker;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;


@Aspect
@Slf4j
@Component
public class TimeTrackerAspect {

    @Around(value = "@annotation(timeTracker)")
    public Object trackTime(ProceedingJoinPoint joinPoint, TimeTracker timeTracker) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String method = joinPoint.getSignature().toShortString();

        log.info("Starting timeTracker for method: {} ", method);

        Object result = joinPoint.proceed();

        log.info("Method has finished its execution: {}, timeMs: {} ", method, stopWatch.getTotalTimeMillis());

        return result;
    }


}
