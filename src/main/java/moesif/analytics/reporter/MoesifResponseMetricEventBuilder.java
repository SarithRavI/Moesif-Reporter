package moesif.analytics.reporter;

import org.wso2.am.analytics.publisher.exception.MetricReportingException;
import org.wso2.am.analytics.publisher.reporter.AbstractMetricEventBuilder;
import org.wso2.am.analytics.publisher.reporter.MetricEventBuilder;
import org.wso2.am.analytics.publisher.reporter.MetricSchema;
import org.wso2.am.analytics.publisher.reporter.GenericInputValidator;
import org.wso2.am.analytics.publisher.util.EventMapAttributeFilter;

import java.util.HashMap;
import java.util.Map;

public class MoesifResponseMetricEventBuilder extends AbstractMetricEventBuilder {
    protected Map<String, Class> requiredAttributes;
    private Map<String, Object> eventMap;
    private Boolean isBuilt = false;

    public MoesifResponseMetricEventBuilder() {
        requiredAttributes = GenericInputValidator.getInstance().getEventProperties(MetricSchema.RESPONSE);
        eventMap = new HashMap<>();
    }

    public MoesifResponseMetricEventBuilder(Map<String, Class> requiredAttributes) {
        this.requiredAttributes = requiredAttributes;
        eventMap = new HashMap<>();
    }

    @Override
    protected Map<String, Object> buildEvent() {
        if (!isBuilt) {
            // util function to filter required attributes
            eventMap = EventMapAttributeFilter.getInstance().filter(eventMap, requiredAttributes);

            isBuilt = true;
        }
        return eventMap;
    }

    @Override
    public boolean validate() throws MetricReportingException {
        return true;
    }

    @Override
    public MetricEventBuilder addAttribute(String key, Object value) throws MetricReportingException {
        eventMap.put(key, value);
        return this;
    }
}
