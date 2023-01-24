import java.io.*;
import java.net.*;

public class DnsClient {

    //Not sure if they should be float or int
    static float timeout = 5f;
    static float maxRetries = 3f;
    static float port = 53f;

    static String requestType = "A";
    static String serverIP = "";
    static String domainName = "";


    public static void main(String[] args) {

        //Input processing
        processInputs(args);

        //Display a summary of the query
        printQuerySummary();


        //TODO: Send the request

        //TODO: Receive response

        //TODO: Process response

        //TODO: Print results/errors

    }

    //Helper methods

    /*
     * Helper method to process input
     * TODO: check that server IP and domainName are in a correct format
     *  (Don't know if it should be here or it will be done automatically later when doing the request)
     */
    static void processInputs(String[] args){

        int n = args.length;
        boolean changedRequestType = false;

        for (int i = 0; i < n; i++) {

            if (args[i].equals("-t")){
                try{
                    timeout = Float.parseFloat(args[i + 1]);
                    i++;

                }catch (Exception e){
                    displayInputError("Timeout value not given after -t argument");
                }
            }else if (args[i].equals("-r")) {

                try{
                    maxRetries = Float.parseFloat(args[i + 1]);
                    i++;

                }catch (Exception e){
                    displayInputError("Max-retries value not given after -r argument");
                }
            }else if (args[i].equals("-p")) {

                try{
                    port = Float.parseFloat(args[i + 1]);
                    i++;

                }catch (Exception e){
                    displayInputError("Port value not given after -p argument");
                }
            }else if (args[i].equals("-mx")) {

                if(!changedRequestType){
                    requestType = "MX";
                }else{
                    displayInputError("Cannot have both -ns and -mx flags");
                }
            }else if (args[i].equals("-ns")) {

                if(!changedRequestType){
                    requestType = "NS";
                }else{
                    displayInputError("Cannot have both -mx and -ns flags");
                }
            }else if (args[i].charAt(0) == '@'){

                    //Check that domain name is the last argument
                    if( (i+1) != (n-1)){
                        displayInputError("Server IP and Domain Name should be the last 2 arguments");
                    }

                    //Take IP address without the @
                    serverIP = args[i].substring(1);

                    //Domain name is the last argument
                    domainName = args[i+1];
                    return;

            }else{
                displayInputError("Found an invalid argument");
            }
        }

        displayInputError("Server IP not found");
    }


    static void displayError(String errorMessage){
        String error = "ERROR \t " + errorMessage;

        System.out.println(error);
        System.exit(1);
    }

    static void displayInputError(String description){
        String error = "Incorrect input syntax: " + description;
        displayError(error);
    }

    static void printQuerySummary(){
        System.out.println("DnsClient sending request for " + domainName+  "\n" +
                "Server: " + serverIP + "\n" +
                "Request type: " + requestType + "\n");
    }
}
