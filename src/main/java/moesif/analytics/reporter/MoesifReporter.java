package moesif.analytics.reporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.analytics.publisher.exception.MetricCreationException;
import org.wso2.am.analytics.publisher.reporter.AbstractMetricReporter;
import org.wso2.am.analytics.publisher.reporter.CounterMetric;
import org.wso2.am.analytics.publisher.reporter.MetricSchema;
import org.wso2.am.analytics.publisher.reporter.TimerMetric;

import java.util.Map;

public class MoesifReporter extends AbstractMetricReporter {
    private static final Logger log = LoggerFactory.getLogger(MoesifReporter.class);
    private final Map<String,String> properties;
    public MoesifReporter(Map<String, String> properties) throws MetricCreationException{
        super(properties);
        String ID = "";

        log.info("Successfully initialized");
        this.properties = properties;
        MoesifKeyRetriever keyRetriever = new MoesifKeyRetriever();
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