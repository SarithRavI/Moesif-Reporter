package moesif.analytics.reporter;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.wso2.am.analytics.publisher.client.EventHubClient;
import org.wso2.am.analytics.publisher.reporter.MetricEventBuilder;
import org.wso2.am.analytics.publisher.reporter.cloud.DefaultAnalyticsThreadFactory;

public class EventQueue {
    private final BlockingQueue<MetricEventBuilder> eventQueue;
    private final ExecutorService publisherExecutorService;
    private final ScheduledExecutorService flushingExecutorService;
    private  final AtomicInteger failureCount;

    public EventQueue(int queueSize, int workerThreadCount, MoesifKeyRetriever moesifKeyRetriever ,int flushingDelay){
        publisherExecutorService = Executors.newFixedThreadPool(workerThreadCount,
                new DefaultAnalyticsThreadFactory("Queue-Worker"));
        flushingExecutorService = Executors.newScheduledThreadPool(workerThreadCount,
                new DefaultAnalyticsThreadFactory("Queue-Flusher"));
        eventQueue = new LinkedBlockingQueue<>(queueSize);
        failureCount = new AtomicInteger(0);

    }
    public void put(MetricEventBuilder builder) {
        try {
            if (!eventQueue.offer(builder)) {
                int count = failureCount.incrementAndGet();
                if (count == 1) {
//                    log.error("Event queue is full. Starting to drop analytics events.");
                } else if (count % 1000 == 0) {
//                    log.error("Event queue is full. " + count + " events dropped so far");
                }
            }
        } catch (RejectedExecutionException e) {
//            log.warn("Task submission failed. Task queue might be full", e);
        }

    }
}