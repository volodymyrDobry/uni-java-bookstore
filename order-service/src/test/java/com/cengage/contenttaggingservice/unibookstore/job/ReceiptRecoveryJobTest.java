package com.cengage.contenttaggingservice.unibookstore.job;

import com.cengage.contenttaggingservice.unibookstore.service.ReceiptRecoveryService;
import org.junit.jupiter.api.Test;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReceiptRecoveryJobTest {

    @Test
    void readsParamsAndMakesASingleServiceCall() {
        ReceiptRecoveryJob job = new ReceiptRecoveryJob();
        ReceiptRecoveryService service = mock(ReceiptRecoveryService.class);
        ReflectionTestUtils.setField(job, "receiptRecoveryService", service);

        JobDataMap data = new JobDataMap();
        data.put("maxAgeMinutes", 15);
        data.put("batchSize", 50);
        data.put("maxAttempts", 3);
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(data);

        job.executeInternal(context);

        verify(service).recoverPendingReceipts(15, 50, 3);
    }

    @Test
    void missingJobDataKeyRaisesExplicitError() {
        ReceiptRecoveryJob job = new ReceiptRecoveryJob();
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(new JobDataMap());

        assertThatThrownBy(() -> job.executeInternal(context)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void disallowsConcurrentExecution() {
        assertThat(ReceiptRecoveryJob.class.isAnnotationPresent(DisallowConcurrentExecution.class)).isTrue();
    }
}
