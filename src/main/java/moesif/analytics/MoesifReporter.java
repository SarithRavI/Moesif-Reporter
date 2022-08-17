package moesif.analytics;

import com.moesif.api.APIHelper;
import com.moesif.api.MoesifAPIClient;
import com.moesif.api.controllers.APIController;
import com.moesif.api.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.am.analytics.publisher.exception.MetricCreationException;
import org.wso2.am.analytics.publisher.exception.MetricReportingException;
import org.wso2.am.analytics.publisher.reporter.*;


import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MoesifReporter implements MetricReporter {
    private MoesifAPIClient client;
    private final APIController api;

    private static final Logger log = LoggerFactory.getLogger(MoesifReporter.class);

    public MoesifReporter(Map<String, String> properties) {
        String ID = "eyJhcHAiOiI2NjA6MzU2IiwidmVyIjoiMi4wIiwib3JnIjoiMjkxOjMyMSIsImlhdCI6MTY1OTMxMjAwMH0.ftR87qJRhnc-GwXdTIJZv5zQ2s08BnUU5UxRLARksZg";
        client = new MoesifAPIClient(ID);

        api = APIController.getInstance();
        log.info("Successfully initialized");
    }

    @Override
    public CounterMetric createCounterMetric(String name, MetricSchema metricSchema) throws MetricCreationException {
        MoesifLogCounter logCounterMetric = new MoesifLogCounter(name, metricSchema);
        MetricEventBuilder event = logCounterMetric.getEventBuilder();
        try {
            publish(event);
        } catch (IOException | MetricReportingException e) {
            throw new RuntimeException(e);
        }
        return logCounterMetric;
    }

    @Override
    public TimerMetric createTimerMetric(String s) {
        return null;
    }

    @Override
    public Map<String, String> getConfiguration() {
        return null;
    }


    public void publish(MetricEventBuilder event) throws IOException, MetricReportingException {

        api.createEventAsync(buildEvent(event), null);
    }

    public EventModel buildEvent(MetricEventBuilder event) throws IOException, MetricReportingException {
        Map<String , Object> data = event.build();
        // Generate the event
        Map<String, String> reqHeaders = new HashMap<String, String>();
        reqHeaders.put("Host", "api.acmeinc.com");
        reqHeaders.put("Accept", "*/*");
        reqHeaders.put("Connection", "Keep-Alive");
        reqHeaders.put("User-Agent", "Apache-HttpClient");
        reqHeaders.put("Content-Type", "application/json");
        reqHeaders.put("Content-Length", "126");
        reqHeaders.put("Accept-Encoding", "gzip");

        Object reqBody = APIHelper.deserialize("{" +
                        "\"items\": [" +
                        "{" +
                        "\"type\": 1," +
                        "\"id\": \"hello\"" +
                "}," +
                        "{" +
                        "\"type\": 2," +
                        "\"id\": \"world\"" +
                        "}" +
                        "]" +
                        "}");

        Map<String, String> rspHeaders = new HashMap<String, String>();
        rspHeaders.put("Vary", "Accept-Encoding");
        rspHeaders.put("Pragma", "no-cache");
        rspHeaders.put("Expires", "-1");
        rspHeaders.put("Content-Type", "application/json; charset=utf-8");
        rspHeaders.put("Cache-Control","no-cache");

        Object rspBody = APIHelper.deserialize("{" +
                "\"Error\": \"InvalidArgumentException\"," +
                "\"Message\": \"Missing field field_a\"" +
                "}");


        EventRequestModel eventReq = new EventRequestBuilder()
                .time(new Date())
                .uri("https://api.acmeinc.com/items/reviews/")
                .verb("PATCH")
                .apiVersion("1.1.0")
                .ipAddress("61.48.220.123")
                .headers(reqHeaders)
                .body(reqBody)
                .build();


        EventResponseModel eventRsp = new EventResponseBuilder()
                .time(new Date(System.currentTimeMillis() + 1000))
                .status(500)
                .headers(rspHeaders)
                .body(rspBody)
                .build();

        EventModel eventModel = new EventBuilder()
                .request(eventReq)
                .response(eventRsp)
                .userId("12345")
                .companyId("67890")
                .sessionToken("XXXXXXXXX")
                .build();

        return eventModel;
    }


}
