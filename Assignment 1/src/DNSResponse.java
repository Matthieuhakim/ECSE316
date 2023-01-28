import java.io.*;
import java.net.*;

public class DNSResponse {

    private byte[] responseData;
    private DNSQueryType queryType;
    private int querySize;
    private int ANCount, NSCount, ARCount;
    private boolean AA;
    private int index;

    public DNSResponse(byte[] responseData, int querySize, DNSQueryType queryType) {

        this.responseData = responseData;
        this.querySize = querySize;
        this.index = querySize;
        this.queryType = queryType;
        this.AA = getBit(responseData[2], 2) == 1;

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
        int ANCount = ((responseData[6] & 0xff) << 8) + (responseData[7] & 0xff);
        int NSCount = ((responseData[8] & 0xff) << 8) + (responseData[9] & 0xff);
        int ARCount = ((responseData[10] & 0xff) << 8) + (responseData[11] & 0xff);

        if(ANCount == 0 && ARCount == 0){
            System.out.println("NOT FOUND");
        }

        //Answer section data
        if (ANCount > 0) {
            System.out.println("***Answer Section (" + ANCount + " records)***");
            for(int i = 0; i < ANCount; i ++){
                printRecordAtIndex(index, true);

            }
        }

        //TODO not sure what this is
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


    }


    private int parseType() {
        byte[] type = {this.responseData[this.index++], this.responseData[this.index++]};
        return getWord(type);
    }

    private void validateClassCode() {
        byte[] classCode = {this.responseData[this.index++], this.responseData[this.index++]};
        if (getWord(classCode) != 1) {
            throw new RuntimeException("ERROR\tUnexpected class code, could not process the server response.");
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

    private static int getRCode(byte b) {
        return ((b >> 0) & 1) + ((b >> 1) & 1) * 2 +((b >> 2) & 1) * 4 + ((b >> 3) & 1) * 8;
    }
}
