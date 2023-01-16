package moesif.analytics.reporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.analytics.publisher.exception.MetricCreationException;
import org.wso2.am.analytics.publisher.reporter.AbstractMetricReporter;
import org.wso2.am.analytics.publisher.reporter.CounterMetric;
import org.wso2.am.analytics.publisher.reporter.MetricSchema;
import org.wso2.am.analytics.publisher.reporter.TimerMetric;
import moesif.analytics.reporter.utils.MoesifConstants;

import java.util.Map;


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
        if (properties.get(MoesifConstants.QUEUE_SIZE) != null) {
            queueSize = Integer.parseInt(properties.get(MoesifConstants.QUEUE_SIZE));
        }
        if (properties.get(MoesifConstants.WORKER_THREAD_COUNT) != null) {
            workerThreads = Integer.parseInt(properties.get(MoesifConstants.WORKER_THREAD_COUNT));
        }
        eventQueue = new EventQueue(queueSize, workerThreads, keyRetriever);
    }

    @Override
    protected void validateConfigProperties(Map<String, String> map) throws MetricCreationException {

    }

    @Override
    public CounterMetric createCounter(String name, MetricSchema metricSchema) throws MetricCreationException {
        MoesifLogCounter logCounterMetric = new MoesifLogCounter(name, eventQueue,metricSchema);

        return logCounterMetric;
    }

    @Override
    protected TimerMetric createTimer(String s) {
        return null;
    }



}