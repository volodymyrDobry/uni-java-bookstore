package com.cengage.contenttaggingservice.unibookstore.config;

import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;

import static org.assertj.core.api.Assertions.assertThat;

class QuartzConfigTest {

    private final QuartzConfig config = new QuartzConfig();

    @Test
    void receiptRecoveryJobAndTriggerHaveExplicitNameAndGroupAndTriggerPointsToTheJob() {
        JobDetail receiptJob = config.receiptRecoveryJobDetail();
        CronTrigger receiptTrigger = (CronTrigger) config.receiptRecoveryTrigger(receiptJob);

        assertThat(receiptJob.getKey().getName()).isEqualTo("receiptRecoveryJob");
        assertThat(receiptJob.getKey().getGroup()).isEqualTo("receipts");
        assertThat(receiptJob.getJobDataMap().getInt("maxAttempts")).isEqualTo(3);

        assertThat(receiptTrigger.getKey().getName()).isEqualTo("receiptRecoveryTrigger");
        assertThat(receiptTrigger.getKey().getGroup()).isEqualTo("receipts");
        assertThat(receiptTrigger.getJobKey()).isEqualTo(receiptJob.getKey());
    }

    @Test
    void receiptRecoveryTriggerDefinesMisfireInstructionAndExplicitTimeZone() {
        CronTrigger receipt = (CronTrigger) config.receiptRecoveryTrigger(config.receiptRecoveryJobDetail());

        assertThat(receipt.getMisfireInstruction()).isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
        assertThat(receipt.getTimeZone().getID()).isEqualTo("UTC");
    }
}
