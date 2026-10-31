package com.cengage.contenttaggingservice.unibookstore.job;

import com.cengage.contenttaggingservice.unibookstore.service.LowStockService;
import org.junit.jupiter.api.Test;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    void readsMergedParamsAndCallsTheService() {
        LowStockDigestJob job = new LowStockDigestJob();
        LowStockService service = mock(LowStockService.class);
        ReflectionTestUtils.setField(job, "lowStockService", service);

        job.executeInternal(contextWith(5, true, "SSE"));

        verify(service).reportLowStock(5, true, "SSE");
    }

    @Test
    void recordsErrorAndWaitsForNextRunInsteadOfPropagating() {
        LowStockDigestJob job = new LowStockDigestJob();
        LowStockService service = mock(LowStockService.class);
        when(service.reportLowStock(anyInt(), anyBoolean(), anyString()))
                .thenThrow(new RuntimeException("db down"));
        ReflectionTestUtils.setField(job, "lowStockService", service);

        assertThatCode(() -> job.executeInternal(contextWith(5, true, "LOG"))).doesNotThrowAnyException();
    }

    @Test
    void disallowsConcurrentExecution() {
        assertThat(LowStockDigestJob.class.isAnnotationPresent(DisallowConcurrentExecution.class)).isTrue();
    }
}
