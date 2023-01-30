import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class DNSResponse {

    public byte[] Id = new byte[2];
    public byte[] responseData;
    public int AA;
    public int ANCount;
    public int ARCount;
    public int length;
    public DNSQueryType qType;

    public int header_offset = 0;
    public int answer_offset;

    public ArrayList<DNSData> AnRecords = new ArrayList<>();
    public ArrayList<DNSData> ArRecords = new ArrayList<>();


    public DNSResponse(byte[] responseData, int querySize, DNSQueryType queryType) {

        this.responseData = responseData;
        this.length = querySize;
        this.qType = queryType;

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
    void print(){

        /* HEADER */

        //Get ID of response
        this.Id[0] = this.responseData[header_offset++];
        this.Id[1] = this.responseData[header_offset++];

        //Parse second row
        byte[] secondRow = new byte[2];
        secondRow[0] = this.responseData[header_offset++];
        secondRow[1] = this.responseData[header_offset++];

        //Set authoritative bit
        this.AA = (secondRow[0] >> 2) & 1;

        //Get number of AN records
        // skip QDCount
        header_offset += 2;
        byte[] ANCountWord = new byte[2];
        ANCountWord[0] = this.responseData[header_offset++];
        ANCountWord[1] = this.responseData[header_offset++];
        this.ANCount = (ANCountWord[0] << 4) + ANCountWord[1];


        //Get number of AR records
        // skip NSCount
        header_offset += 2;
        byte[] ARCountWord = new byte[2];
        ARCountWord[0] = this.responseData[header_offset++];
        ARCountWord[1] = this.responseData[header_offset++];
        this.ARCount = (ARCountWord[0] << 4) + ARCountWord[1];



        /* ANSWER */
        answer_offset = length;
        for (int i = 0; i < ANCount; i++) {
            DNSData record = new DNSData(this.AA);

            String domain = getString(answer_offset);
            answer_offset += 2;

            DNSQueryType type = getType();

            //Validate class row
            byte[] arr = new byte[2];
            arr[0] = this.responseData[answer_offset++];
            arr[1] = this.responseData[answer_offset++];
            if (arr[1] != 1) {
                throw new RuntimeException("Class row not equal to 0x0001");
            }

            //Get TTL
            arr = new byte[4];
            arr[0] = this.responseData[answer_offset++];
            arr[1] = this.responseData[answer_offset++];
            arr[2] = this.responseData[answer_offset++];
            arr[3] = this.responseData[answer_offset++];
            int ttl = ByteBuffer.wrap(arr).getInt();

            //Get RD Length
            arr = new byte[2];
            arr[0] = this.responseData[answer_offset++];
            arr[1] = this.responseData[answer_offset++];
            int rdLength = (arr[0] << 4) + arr[1];

            String rdata = getRData(type, record);

            record.setQueryType(type);
            record.setrData(domain);
            record.setTTL(ttl);
            record.setrData(rdata);

            this.AnRecords.add(record);

            if (type != DNSQueryType.A) answer_offset += rdLength;
        }

        /* ADDITIONAL */
        for (int i = 0; i < ARCount; i++) {
            DNSData record = new DNSData(this.AA);

            String domain = getString(answer_offset);
            answer_offset += 2;

            DNSQueryType type = getType();

            //Validate class row
            byte[] arr = new byte[2];
            arr[0] = this.responseData[answer_offset++];
            arr[1] = this.responseData[answer_offset++];
            if (arr[1] != 1) {
                throw new RuntimeException("Class row not equal to 0x0001");
            }

            //Get TTL
            arr = new byte[4];
            arr[0] = this.responseData[answer_offset++];
            arr[1] = this.responseData[answer_offset++];
            arr[2] = this.responseData[answer_offset++];
            arr[3] = this.responseData[answer_offset++];
            int ttl = ByteBuffer.wrap(arr).getInt();

            //Get RD Length
            arr = new byte[2];
            arr[0] = this.responseData[answer_offset++];
            arr[1] = this.responseData[answer_offset++];
            int rdLength = (arr[0] << 4) + arr[1];

            String rdata = getRData(type, record);

            record.setQueryType(type);
            record.setrData(domain);
            record.setTTL(ttl);
            record.setrData(rdata);

            this.ArRecords.add(record);

            answer_offset += rdLength;
        }

        //If there are no records
        if(ANCount == 0){
            System.out.println("NOT FOUND");
            return;
        }

        //Print answer section data
        System.out.println("***Answer Section (" + ANCount + " records)***");
        for(int i = 0; i < ANCount; i ++){
            this.AnRecords.get(i).printRecord();
        }


        //Print additional section data
        if (ARCount > 0) {
            System.out.println("***Additional Section (" + ARCount + " records)***");
            for(int i = 0; i < ARCount; i ++){
                this.AnRecords.get(i).printRecord();
            }
        }
    }


    private DNSQueryType getType() {
        byte[] arr = new byte[2];
        arr[0] = this.responseData[answer_offset++];
        arr[1] = this.responseData[answer_offset++];

        if (arr[1] == 1) {
            return DNSQueryType.A;
        } else if (arr[1] == 2) {
            return DNSQueryType.NS;
        } else if (arr[1] == 5) {
            return DNSQueryType.CNAME;
        } else if (arr[1] == 15) {
            return DNSQueryType.MX;
        }else {
            throw new RuntimeException("Unknown type in response record.");
        }
    }


    private String getString(int index) {
        StringBuilder result = new StringBuilder();

        int length = this.responseData[index];

        boolean first = true;
        while(length != 0) {
            if (!first) {
                result.append(".");
            } else first = false;

            if ((length & 0xC0) == 0xC0) {
                // keep the pointer to the data
                byte[] offset = {(byte) (this.responseData[index] & 0x3F), this.responseData[index + 1]};
                ByteBuffer wrapped = ByteBuffer.wrap(offset);
                result.append(getString(wrapped.getShort()));
                break;
            } else {
                for (int i = 0; i < length; i++) {
                    // System.out.println("CHAR : " + (char) dataBuff[index + i + 1]);
                    result.append((char) responseData[index + i + 1]);
                }
                index += length + 1;
                length = this.responseData[index];
            }
        }
        return result.toString();
    }


    private String getRData(DNSQueryType type, DNSData record) {
        String result = "";
        if (type == DNSQueryType.A) {
            byte [] arr = new byte[4];
            for (int i = 0; i < 4; i++) {
                arr[i] = this.responseData[answer_offset++];
            }
            try {
                InetAddress iNetAddress = InetAddress.getByAddress(arr);
                result = iNetAddress.toString().replace("/", "");
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

        } else if (type == DNSQueryType.NS || type == DNSQueryType.CNAME) {
            result = getString(answer_offset);
        }
        else if (type == DNSQueryType.MX) {

            //Get Pref
            byte[] arr = new byte[2];
            arr[0] = this.responseData[answer_offset++];
            arr[1] = this.responseData[answer_offset++];
            int pref =  (arr[0] << 4) + arr[1];

            record.setPref(pref);
            result = getString(answer_offset);
        }

        return result;
    }

}
