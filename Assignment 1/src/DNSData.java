public class DNSData {

    private int bytes;
    private String domain;

    public int getNumOfBytes() {
        return bytes;
    }

    public String getDomainName() {
        return domain;
    }

    public void setNumOfBytes(int numBytes) {
        this.bytes = numBytes;
    }

    public void setDomainName(String domain) {
        this.domain = domain;
    }
}
