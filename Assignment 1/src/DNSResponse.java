public class DNSResponse {

    private byte[] responseData;
    private int ANCount, NSCount, ARCount;
    private boolean isAuth;
    private int index;


    public DNSResponse(byte[] responseData, int querySize, DNSQueryType queryType) {

        this.responseData = responseData;
        this.index = querySize;
        this.isAuth = getBit(responseData[2], 2) == 1;

    }

    /*
     * The method validates the response
     */
    void validate() throws Exception{
        if (((responseData[2]>>7)&1) != 1) {
            throw new Exception("Received response is a query, not a response.");
        }
        if (((responseData[3]>>7)&1) != 1) {
            throw new Exception("Server does not support recursive queries.");
        }
        switch (responseData[3] & 0x0F) {
            case 1 -> throw new Exception("Format error: the name server was unable to interpret the query.");
            case 2 -> throw new Exception("Server failure: the name server was unable to process this query due to a problem with the name server.");
            case 3 -> throw new Exception("Name error: meaningful only for responses from an authoritative name server, the code signifies that the domain name referenced in the query does not exist.");
            case 4 -> throw new Exception("Not implemented: the name server does not support the requested kind of query.");
            case 5 -> throw new Exception("Refused: the name server refuses to perform the requested operation for policy reasons.");
            default -> {
            }
        }
    }


    /*
     * The method parses the response to get the required information and prints it
     */
    void print() throws Exception {

        //Get the number of records for each section
        ANCount = ((responseData[6] & 0xff) << 8) + (responseData[7] & 0xff);
        NSCount = ((responseData[8] & 0xff) << 8) + (responseData[9] & 0xff);
        ARCount = ((responseData[10] & 0xff) << 8) + (responseData[11] & 0xff);

        //If there are no records
        if(ANCount == 0 && ARCount == 0){
            System.out.println("NOT FOUND");
            return;
        }

        //Answer section data
        if (ANCount > 0) {
            System.out.println("***Answer Section (" + ANCount + " records)***");
            for(int i = 0; i < ANCount; i ++){
                printRecordAtIndex(index, true);

            }
        }

        if (NSCount > 0) {
            for(int i = 0; i < NSCount; i ++){
                printRecordAtIndex(index, false);

            }
        }

        //Additional section data
        if (ARCount > 0) {
            System.out.println("***Additional Section (" + ARCount + " records)***");
            for(int i = 0; i < ARCount; i ++){
                printRecordAtIndex(index, true);

            }
        }
    }

    private void printRecordAtIndex(int index, boolean print) {
        DNSData domainData = this.parseDomain(this.index);
        this.index = domainData.getNumOfBytes();
        long cacheSeconds = 0;
        int rdLength = 0;
        int parseType = this.parseType();
        switch (parseType) {
            case 1 -> {
                this.validateClassCode();
                cacheSeconds = this.getCacheSeconds();
                rdLength = this.getRdLength();
                DNSData ipEntry = this.parseIp(this.index, rdLength);
                if (print)
                    System.out.print("IP\t" + ipEntry.getDomainName() + "\t" + cacheSeconds + "\t" + (this.isAuth ? "auth" : "nonauth") + "\n");
                this.index = ipEntry.getNumOfBytes();
                break;
            }
            case 2 -> {
                this.validateClassCode();
                cacheSeconds = this.getCacheSeconds();
                rdLength = this.getRdLength();
                DNSData nsEntry = parseDomain(this.index);
                if (print)
                    System.out.print("NS\t" + domainData.getDomainName() + "\t" + cacheSeconds + "\t" + (this.isAuth ? "auth" : "nonauth") + "\n");
                this.index = nsEntry.getNumOfBytes();
                break;
            }
            case 5 -> {
                this.validateClassCode();
                cacheSeconds = this.getCacheSeconds();
                rdLength = this.getRdLength();
                DNSData cNameEntry = parseDomain(this.index);
                if (print)
                    System.out.print("CNAME\t" + domainData.getDomainName() + "\t" + cacheSeconds + "\t" + (this.isAuth ? "auth" : "nonauth") + "\n");
                this.index = cNameEntry.getNumOfBytes();
                break;
            }
            case 15 -> {
                this.validateClassCode();
                cacheSeconds = this.getCacheSeconds();
                rdLength = this.getRdLength();
                int pref = this.getPref();
                DNSData mxEntry = parseDomain(this.index);
                if (print)
                    System.out.print("CNAME\t" + domainData.getDomainName() + "\t" + pref + "\t" + cacheSeconds + "\t" + (this.isAuth ? "auth" : "nonauth") + "\n");
                this.index = mxEntry.getNumOfBytes();
                break;
            }
            default -> throw new RuntimeException("Unexpected record type (" + parseType + "), could not process the server response.");
        }

    }

    private DNSData parseDomain(int index) {
        DNSData domainData = new DNSData();
        StringBuilder domain = new StringBuilder();
        int storedIndex = index;
        int length = -1;
        boolean compressed = false;

        while(this.responseData[index] != 0x00) {


            //If the domain name is somewhere else (compressed)
            if((this.responseData[index] & 0xC0) == 0xC0 && length <= 0) {

                byte[] domainIndex = {(byte) (this.responseData[index++] & 0x3f), this.responseData[index]};
                storedIndex = index;
                compressed = true;
                index +=2;
//                index = getWord(domainIndex);

            } else {
                if (length == 0) {
                    domain.append(".");
                    length = this.responseData[index];
                } else if (length < 0) {
                    length = this.responseData[index];
                } else {
                    domain.append((char) (this.responseData[index] & 0xFF));
                    length--;
                }

                index++;
            }
        }

        if (compressed) {
            domainData.setNumOfBytes(++storedIndex);
        }
        else {
            domainData.setNumOfBytes(index);
        }

        domainData.setDomainName(domain.toString());

        return domainData;
    }


    private DNSData parseIp(int index, int length) {
        DNSData ipData = new DNSData();
        StringBuilder ip = new StringBuilder();
        int storedIndex = index;

        while(length > 0) {
            ip.append(this.responseData[index] & 0xff);
            length--;
            if (length != 0) {
                ip.append(".");
            }
            index++;
        }

        ipData.setNumOfBytes(++storedIndex);
        ipData.setDomainName(ip.toString());

        return ipData;
    }

    private int parseType() {
        byte[] type = {this.responseData[this.index++], this.responseData[this.index++]};
        return getWord(type);
    }

    private void validateClassCode() {
        byte[] classCode = {this.responseData[this.index++], this.responseData[this.index++]};
        if (getWord(classCode) != 1) {
            throw new RuntimeException("Unexpected class code, could not process the server response.");
        }
    }

    private long getCacheSeconds() {
        byte[] LMB = {this.responseData[this.index++], this.responseData[this.index++]};
        byte[] RMB = {this.responseData[this.index++], this.responseData[this.index++]};
        return getWord(LMB) * 65536L + getWord(RMB);
    }

    private int getRdLength() {
        byte[] rdLength = {this.responseData[this.index++], this.responseData[this.index++]};
        return getWord(rdLength);
    }

    private int getPref() {
        byte[] pref = {this.responseData[this.index++], this.responseData[this.index++]};
        return getWord(pref);
    }

    private static int getWord(byte[] bytes) {
        return ((bytes[0] & 0xff) << 8) + (bytes[1] & 0xff);
    }

    private static int getBit(byte b, int p) {
        return (b >> p) & 1;
    }
}
