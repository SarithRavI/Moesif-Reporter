package moesif.analytics.reporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.analytics.publisher.exception.MetricReportingException;
import org.wso2.am.analytics.publisher.reporter.CounterMetric;
import org.wso2.am.analytics.publisher.reporter.GenericInputValidator;
import org.wso2.am.analytics.publisher.reporter.MetricEventBuilder;
import org.wso2.am.analytics.publisher.reporter.MetricSchema;

import java.util.Map;

public class MoesifLogCounter implements CounterMetric {
    private static final Logger log = LoggerFactory.getLogger(MoesifLogCounter.class);
    private String name;
    private MetricSchema schema;
    private  EventQueue queue;


    public MoesifLogCounter(String name, EventQueue queue,MetricSchema schema) {
        this.name = name;
        this.schema = schema;
        this.queue = queue;
    }

    @Override
    public int incrementCount(MetricEventBuilder metricEventBuilder) throws MetricReportingException {
        queue.put(metricEventBuilder);
        return 0;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public MetricSchema getSchema() {
        return this.schema;
    }

    @Override
    public MetricEventBuilder getEventBuilder() {
        switch (schema) {
            case RESPONSE:
            default:
                return new MoesifResponseMetricEventBuilder(GenericInputValidator.getInstance().getEventProperties(MetricSchema.RESPONSE));
            case ERROR:
                return new MoesifResponseMetricEventBuilder(GenericInputValidator.getInstance().getEventProperties(MetricSchema.ERROR));
            case CHOREO_RESPONSE:
                return new MoesifResponseMetricEventBuilder(GenericInputValidator.getInstance().getEventProperties(MetricSchema.CHOREO_RESPONSE));
            case CHOREO_ERROR:
                return new MoesifResponseMetricEventBuilder(GenericInputValidator.getInstance().getEventProperties(MetricSchema.CHOREO_ERROR));
        }
    }

}