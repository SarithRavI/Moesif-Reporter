package moesif.analytics.reporter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import moesif.analytics.client.ClientContextHolder;
import moesif.analytics.client.MoesifClient;
import moesif.analytics.keyRetriever.MoesifKeyRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.analytics.publisher.exception.MetricReportingException;
import org.wso2.am.analytics.publisher.reporter.MetricEventBuilder;

public class ParallelQueueWorker implements Runnable {
    private  static  final Logger log = LoggerFactory.getLogger(ParallelQueueWorker.class);
    private BlockingQueue<MetricEventBuilder> eventQueue;
    private MoesifKeyRetriever keyRetriever;


    public ParallelQueueWorker(BlockingQueue<MetricEventBuilder> queue, MoesifKeyRetriever keyRetriever) {
        this.eventQueue = queue;
        this.keyRetriever = keyRetriever;
    }

    public void run() {

        while (true) {
            MetricEventBuilder eventBuilder;
            try {
                eventBuilder = eventQueue.take();
                if (eventBuilder != null) {
                    // init local moesif client
                    MoesifClient client = new MoesifClient(keyRetriever);
                    Map<String, Object> eventMap = eventBuilder.build();
                    client.publish(eventMap);
                }
            } catch (MetricReportingException e) {
                log.error("Builder instance is not duly filled. Event building failed", e);
                continue;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Analytics event sending failed. Event will be dropped", e);
            }

        }
    }


}
