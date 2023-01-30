public class DNSData {

    private int ttl;
    private int pref;
    private String rData;
    private DNSQueryType qType;
    private int auth;

    /**
     * Constructor.
     *
     * @param auth
     */
    public DNSData(int auth) {
        this.auth = auth;
    }

    /**
     * Print the record depending on the query type.
     */
    public void printRecord() {
        String authoritative;
        if (auth == 1) {
            authoritative = "auth";
        } else {
            authoritative = "nonauth";
        }
        if (qType == DNSQueryType.A) {
            System.out.println("IP\t" + rData + "\t" + ttl + "\t" + authoritative);
        } else if (qType == DNSQueryType.NS) {
            System.out.println("NS\t" + rData + "\t" + ttl + "\t" + authoritative);
        } else if (qType == DNSQueryType.MX) {
            System.out.println("MX\t" + rData + "\t" + pref + "\t" + ttl + "\t" + authoritative);
        } else if (qType == DNSQueryType.CNAME) {
            System.out.println("CNAME\t" + rData + "\t" + ttl + "\t" + authoritative);
        }
    }

    /* SETTERS */

    public void setTTL(int ttl) {
        this.ttl = ttl;
    }

    public void setPref(int pref) {
        this.pref = pref;
    }

    public void setrData(String rData) {
        this.rData = rData;
    }

    public void setQueryType(DNSQueryType queryType) {
        this.qType = queryType;
    }

}
