package moesif.analytics.reporter.utils;

public class MoesifKeyEntry {
    private String uuid;
    private String organization_id;
    private String moesif_key;
    private String env;

    public MoesifKeyEntry() {

    }

    public String getMoesif_key() {
        return moesif_key;
    }

    public String getOrganization_id() {
        return organization_id;
    }

    public String getUuid() {
        return uuid;
    }

    public String getEnv() {
        return env;
    }
}
