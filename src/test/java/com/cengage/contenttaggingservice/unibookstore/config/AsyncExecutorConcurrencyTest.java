package com.cengage.contenttaggingservice.unibookstore.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AsyncExecutorConcurrencyTest {

    private static final long TASK_MILLIS = 300L;

    private AnnotationConfigApplicationContext context;
    private SlowTasks slowTasks;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.registerBean(PropertySourcesPlaceholderConfigurer.class);
        context.register(AsyncConfig.class, SlowTasks.class);
        context.refresh();
        slowTasks = context.getBean(SlowTasks.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void twoIndependentAsyncCallsRunConcurrentlyOnTheNamedExecutor() throws Exception {
        long start = System.nanoTime();

        CompletableFuture<String> first = slowTasks.slow("A");
        CompletableFuture<String> second = slowTasks.slow("B");
        String combined = first.thenCombine(second, (a, b) -> a + "+" + b).get(3, TimeUnit.SECONDS);

        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(combined).isEqualTo("A+B");
        assertThat(elapsedMillis).isLessThan(2 * TASK_MILLIS);
        assertThat(slowTasks.threadNames()).hasSize(2);
        assertThat(slowTasks.threadNames()).allSatisfy(name -> assertThat(name).startsWith("ubs-order-"));
    }

    @Component
    static class SlowTasks {

        private final List<String> threadNames = new CopyOnWriteArrayList<>();

        @Async(AsyncConfig.ORDERING_EXECUTOR)
        public CompletableFuture<String> slow(String value) throws InterruptedException {
            threadNames.add(Thread.currentThread().getName());
            Thread.sleep(TASK_MILLIS);
            return CompletableFuture.completedFuture(value);
        }

        List<String> threadNames() {
            return threadNames;
        }
    }
}
