package moesif.analytics.exception;

public class APICallingException extends  Exception{
    public APICallingException(String msg) {
        super(msg);
    }

    public APICallingException(String msg, Throwable e) {
        super(msg, e);
    }
}
