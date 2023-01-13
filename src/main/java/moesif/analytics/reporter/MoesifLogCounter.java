package moesif.analytics.reporter;

import com.google.gson.Gson;
import com.moesif.api.MoesifAPIClient;
import com.moesif.api.controllers.APIController;
import com.moesif.api.models.*;
import moesif.analytics.reporter.utils.MoesifConstants;
import moesif.analytics.reporter.utils.UUIDCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.analytics.publisher.exception.MetricReportingException;
import org.wso2.am.analytics.publisher.reporter.CounterMetric;
import org.wso2.am.analytics.publisher.reporter.GenericInputValidator;
import org.wso2.am.analytics.publisher.reporter.MetricEventBuilder;
import org.wso2.am.analytics.publisher.reporter.MetricSchema;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MoesifLogCounter implements CounterMetric {
    static final String UUIDNameSpace = MoesifConstants.NAMESPACE_URL;
    private static final Logger log = LoggerFactory.getLogger(MoesifLogCounter.class);
    private final Gson gson;
    private final Map<String, String> properties;
    private String name;
    private MetricSchema schema;
    private UUIDCreator uuidCreator;

    public MoesifLogCounter(String name, MetricSchema schema, Map<String, String> properties) {
        this.name = name;
        this.schema = schema;
        this.gson = new Gson();
        this.uuidCreator = new UUIDCreator();
        this.properties = properties;
    }

    @Override
    public int incrementCount(MetricEventBuilder metricEventBuilder) throws MetricReportingException {
        Map<String, Object> event = metricEventBuilder.build();
        try {
            publish(event);
        } catch (Throwable e) {
            throw new RuntimeException("Moesif: Not publishing event " + e.toString());
        }
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

    public void publish(Map<String, Object> event) throws Throwable, MetricReportingException {
        // if the orgID_moesifKeyMap is empty refresh it.
        if(MoesifKeyRetriever.orgID_moesifKeyMap.isEmpty()){
            MoesifKeyRetriever.initOrRefreshOrgIDMoesifKeyMap();
        }

        // get the org id from the event
        String org_id = (String) event.get(MoesifConstants.ORGANIZATION_ID);
        // fetch the moesif key from orgID_moesifKeyMap
        String moesif_key;
        if (MoesifKeyRetriever.orgID_moesifKeyMap.containsKey(org_id)) {
            moesif_key = MoesifKeyRetriever.orgID_moesifKeyMap.get(org_id);
        } else {
            moesif_key = MoesifKeyRetriever.getMoesifKey(org_id);
            if (moesif_key == null) {
                throw new MetricReportingException(
                        "Corresponding Moesif key for organization " + org_id + " can't be found.");
            }
        }

        // init moesif client
        MoesifAPIClient client = new MoesifAPIClient(moesif_key);
        APIController api = client.getAPI();
        switch (schema) {
            case RESPONSE:
            case CHOREO_RESPONSE:
                api.createEvent(buildEventResponse(event));
                break;
            case ERROR:
            case CHOREO_ERROR:
                api.createEvent(buildEventFault(event));
                break;
        }
    }

    public EventModel buildEventResponse(Map<String, Object> data) throws IOException, MetricReportingException {
        String jsonString = gson.toJson(data);
        String reqBody = jsonString.replaceAll("[\r\n]", "");

        //      Preprocessing data
        final URL uri = new URL((String) data.get(MoesifConstants.DESTINATION));
        final String hostName = uri.getHost();

        final String userIP = (String) data.get(MoesifConstants.USER_IP);

        Map<String, String> reqHeaders = new HashMap<String, String>();

        reqHeaders.put("User-Agent",
                (String) data.getOrDefault(MoesifConstants.USER_AGENT_HEADER, MoesifConstants.UNKNOWN_VALUE));
        reqHeaders.put("Content-Type", MoesifConstants.MOESIF_CONTENT_TYPE_HEADER);
        reqHeaders.put("Host", hostName);

        Map<String, String> rspHeaders = new HashMap<String, String>();

        rspHeaders.put("Vary", "Accept-Encoding");
        rspHeaders.put("Pragma", "no-cache");
        rspHeaders.put("Expires", "-1");
        rspHeaders.put("Content-Type", "application/json; charset=utf-8");
        rspHeaders.put("Cache-Control", "no-cache");


        EventRequestModel eventReq = new EventRequestBuilder()
                .time(new Date()) // See if you can parse request time stamp to date obj
                .uri(uri.toString())
                .verb((String) data.get(MoesifConstants.API_METHOD))
                .apiVersion((String) data.get(MoesifConstants.API_VERSION))
                .ipAddress(userIP)
                .headers(reqHeaders)
                .body(reqBody)
                .build();


        EventResponseModel eventRsp = new EventResponseBuilder()
                .time(new Date(System.currentTimeMillis() + 1000))
                .status((int) data.get(MoesifConstants.TARGET_RESPONSE_CODE))
                .headers(rspHeaders)
                .build();

        EventModel eventModel = new EventBuilder()
                .request(eventReq)
                .response(eventRsp)
                .userId((String) data.get("userName"))
                .companyId((String) data.get(MoesifConstants.ORGANIZATION_ID))
                .build();

        return eventModel;
    }

    public EventModel buildEventFault(Map<String, Object> data) throws IOException, MetricReportingException {
// Generate the event
        String jsonString = gson.toJson(data);
        String reqBody = jsonString.replaceAll("[\r\n]", "");

        Map<String, String> reqHeaders = new HashMap<String, String>();

        reqHeaders.put("User-Agent", (String) data.get(MoesifConstants.USER_AGENT_HEADER) + " fault");

        Map<String, String> rspHeaders = new HashMap<String, String>();

        final String userIP = (String) data.get(MoesifConstants.USER_IP);

        EventRequestModel eventReq = new EventRequestBuilder()
                .time(new Date()) // See if you can parse request time stamp to date obj
                .uri((String) data.get(MoesifConstants.DESTINATION))
                .verb((String) data.get(MoesifConstants.API_METHOD))
                .apiVersion((String) data.get(MoesifConstants.API_VERSION))
                .ipAddress(userIP)
                .headers(reqHeaders)
                .body(reqBody)
                .build();


        EventResponseModel eventRsp = new EventResponseBuilder()
                .time(new Date(System.currentTimeMillis() + 1000))
                .status((int) data.get(MoesifConstants.TARGET_RESPONSE_CODE))
                .headers(rspHeaders)
                .build();

        EventModel eventModel = new EventBuilder()
                .request(eventReq)
                .response(eventRsp)
                .userId(this.uuidCreator.getUUIDStrFromName(MoesifLogCounter.UUIDNameSpace,
                        userIP))
                .companyId((String) data.get(MoesifConstants.ORGANIZATION_ID))
                .build();

        return eventModel;
    }
}