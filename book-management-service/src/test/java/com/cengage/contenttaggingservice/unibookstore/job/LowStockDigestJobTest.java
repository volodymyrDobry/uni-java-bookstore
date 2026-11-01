package com.cengage.contenttaggingservice.unibookstore.job;

import com.cengage.contenttaggingservice.unibookstore.dto.LowStockDigestResponse;
import com.cengage.contenttaggingservice.unibookstore.service.LowStockDigestService;
import org.junit.jupiter.api.Test;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LowStockDigestJobTest {

    private JobExecutionContext contextWith(int threshold, boolean onlyEnabled, String channel) {
        JobDataMap data = new JobDataMap();
        data.put("threshold", threshold);
        data.put("onlyEnabled", onlyEnabled);
        data.put("channel", channel);
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(data);
        return context;
    }

    private LowStockDigestResponse digest() {
        return new LowStockDigestResponse(1L, Instant.now(), 5, true, "SSE", 0, List.of());
    }

    @Test
    void readsMergedParamsAndRecordsADigest() {
        LowStockDigestJob job = new LowStockDigestJob();
        LowStockDigestService service = mock(LowStockDigestService.class);
        when(service.recordDigest(5, true, "SSE")).thenReturn(digest());
        ReflectionTestUtils.setField(job, "lowStockDigestService", service);

        job.executeInternal(contextWith(5, true, "SSE"));

        verify(service).recordDigest(5, true, "SSE");
    }

    @Test
    void recordsErrorAndWaitsForNextRunInsteadOfPropagating() {
        LowStockDigestJob job = new LowStockDigestJob();
        LowStockDigestService service = mock(LowStockDigestService.class);
        when(service.recordDigest(anyInt(), anyBoolean(), anyString())).thenThrow(new RuntimeException("db down"));
        ReflectionTestUtils.setField(job, "lowStockDigestService", service);

        assertThatCode(() -> job.executeInternal(contextWith(5, true, "LOG"))).doesNotThrowAnyException();
    }

    @Test
    void disallowsConcurrentExecution() {
        assertThat(LowStockDigestJob.class.isAnnotationPresent(DisallowConcurrentExecution.class)).isTrue();
    }
}
