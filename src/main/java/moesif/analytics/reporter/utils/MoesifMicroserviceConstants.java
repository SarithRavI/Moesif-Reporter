package moesif.analytics.reporter.utils;

public class MoesifMicroserviceConstants {
    public final static String DETAIL_URL = "http://microservice:8080/moesif_key/";
    public final static String LIST_URL = "http://microservice:8080/moesif_key";
    public static final String GA_USERNAME_CONFIG_KEY = "gaAuthUsername";
    public static final String GA_PWD_CONFIG_KEY = "gaAuthPwd";
    public final static String CONTENT_TYPE = "application/json";
    public final static String QUERY_PARAM = "org_id";
    public final static int NUM_RETRY_ATTEMPTS = 3;
    public final static long TIME_TO_WAIT = 10000;
    public final static int NUM_RETRY_ATTEMPTS_PUBLISH = 3;
    public final static long TIME_TO_WAIT_PUBLISH = 10000;
    public final static int REQUEST_READ_TIMEOUT = 10000;



}
