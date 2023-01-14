import java.io.*;
import java.net.*;

public class DnsClient {

    //Not sure if they should be float
    static float timeout = 5f;
    static float maxRetries = 3f;
    static float port = 53f;


    public static void main(String[] args) {

        //Input processing
        processInputs(args);

        System.out.println("DnsClient sending request for [name] \n" +
                "Server: [server IP address] \n" +
                "Request type: [A | MX | NS]");

    }

    //Helper methods

    /*
     * Helper method to process input
     * To do:
     * -mx and -ns flags
     * @server
     * name
     *
     */
    static void processInputs(String[] args){
        for (int i = 0; i < args.length; i++) {

            if (args[i].equals("-t")){
                try{
                    timeout = Float.parseFloat(args[i + 1]);
                    i++;

                }catch (Exception e){
                    displayError("Incorrect input syntax: Timeout value not given after -t argument");
                }
            }else if (args[i].equals("-r")) {

                try{
                    maxRetries = Float.parseFloat(args[i + 1]);
                    i++;

                }catch (Exception e){
                    displayError("Incorrect input syntax: Max-retries value not given after -r argument");
                }
            }else if (args[i].equals("-p")) {

                try{
                    port = Float.parseFloat(args[i + 1]);
                    i++;

                }catch (Exception e){
                    displayError("Incorrect input syntax: Port value not given after -p argument");
                }
            }

        }
    }


    static void displayError(String errorMessage){
        String error = "ERROR \t " + errorMessage;

        System.out.println(error);
        System.exit(1);
    }
}
