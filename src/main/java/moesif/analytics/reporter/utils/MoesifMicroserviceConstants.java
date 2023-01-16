package moesif.analytics.reporter.utils;

public class MoesifMicroserviceConstants {
    public final static String DETAIL_URL = "http://microservice:8080/moesif_key/";
    public final static String LIST_URL = "http://microservice:8080/moesif_key";
    public final static String CONTENT_TYPE = "application/json";
    public final static String QUERY_PARAM = "org_id";
    public final static int NUM_RETRY_ATTEMPTS = 3;
    public final static long TIME_TO_WAIT = 10;
    public final static int NUM_RETRY_ATTEMPTS_PUBLISH = 3;
    public final static long TIME_TO_WAIT_PUBLISH = 10;



}
