package moesif.analytics.reporter;

import moesif.analytics.reporter.utils.MoesifConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.analytics.publisher.exception.MetricCreationException;
import org.wso2.am.analytics.publisher.reporter.AbstractMetricReporter;
import org.wso2.am.analytics.publisher.reporter.CounterMetric;
import org.wso2.am.analytics.publisher.reporter.MetricSchema;
import org.wso2.am.analytics.publisher.reporter.TimerMetric;

import java.util.Map;
import moesif.analytics.reporter.EventQueue;


public class MoesifReporter extends AbstractMetricReporter {
    private static final Logger log = LoggerFactory.getLogger(MoesifReporter.class);
    private final Map<String,String> properties;

    private final EventQueue eventQueue;

    public MoesifReporter(Map<String, String> properties) throws MetricCreationException{
        super(properties);
        this.properties = properties;
        MoesifKeyRetriever keyRetriever =  MoesifKeyRetriever.getInstance();
        int queueSize = MoesifConstants.DEFAULT_QUEUE_SIZE;
        int workerThreads = MoesifConstants.DEFAULT_WORKER_THREADS;
        int flushingDelay = MoesifConstants.DEFAULT_FLUSHING_DELAY;
        if (properties.get(MoesifConstants.QUEUE_SIZE) != null) {
            queueSize = Integer.parseInt(properties.get(MoesifConstants.QUEUE_SIZE));
        }
        if (properties.get(MoesifConstants.WORKER_THREAD_COUNT) != null) {
            workerThreads = Integer.parseInt(properties.get(MoesifConstants.WORKER_THREAD_COUNT));
        }
        if (properties.get(MoesifConstants.CLIENT_FLUSHING_DELAY) != null) {
            flushingDelay = Integer.parseInt(properties.get(MoesifConstants.CLIENT_FLUSHING_DELAY));
        }
        eventQueue = new EventQueue(queueSize, workerThreads, keyRetriever, flushingDelay);
    }

    @Override
    protected void validateConfigProperties(Map<String, String> map) throws MetricCreationException {

    }

    @Override
    public CounterMetric createCounter(String name, MetricSchema metricSchema) throws MetricCreationException {
        MoesifLogCounter logCounterMetric = new MoesifLogCounter(name, metricSchema,this.properties);

        return logCounterMetric;
    }

    @Override
    protected TimerMetric createTimer(String s) {
        return null;
    }



}