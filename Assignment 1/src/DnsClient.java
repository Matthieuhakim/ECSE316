import java.io.*;
import java.net.*;
import java.util.Random;

public class DnsClient {

	static int timeout = 5000;
	static int maxRetries = 3;
	static int port = 53;
	static DNSQueryType queryType = DNSQueryType.A;

	static String serverIP = "";
	static String domainName = "";
	private static byte[] server = new byte[4];

	// Fields necessary for request
    static int HEADER_SIZE = 12;
    static int QTYE_QCLASS_SIZE = 4;
    
	public static void main(String[] args) {

		// Input processing
		try {
			processInputs(args);
			byte[] request = getRequest();
			// Display a summary of the query
			printQuerySummary();
			
			// TODO: Send the request
			byte[] response = sendRequest(request, 1);
		} catch (Exception e) {
			System.out.println("ERROR: \t " + e.getMessage());
		}

		// TODO: Receive response

		// TODO: Process response

		// TODO: Print results/errors

	}

	// Helper methods

	/*
	 * Helper method to process input TODO: check that server IP and domainName are
	 * in a correct format (Don't know if it should be here or it will be done
	 * automatically later when doing the request)
	 */
	static void processInputs(String[] args) throws Exception {

		int n = args.length;
		if (n < 2) {
			throw new Exception(
					"Please use the format: java DnsClient [-t timeout] [-r max-retries] [-p port] [-mx|-ns] @server name");
		}
		boolean changedRequestType = false;

		for (int i = 0; i < n; i++) {

			if (args[i].equals("-t")) {
				try {
					timeout = Integer.parseInt(args[i + 1]) * 1000; // Convert seconds to milliseconds
					i++;
				} catch (Exception e) {
					throw new Exception("Timeout value not given after -t argument");
				}
			} else if (args[i].equals("-r")) {

				try {
					maxRetries = Integer.parseInt(args[i + 1]);
					i++;

				} catch (Exception e) {
					throw new Exception("Max-retries value not given after -r argument");
				}
			} else if (args[i].equals("-p")) {

				try {
					port = Integer.parseInt(args[i + 1]);
					i++;

				} catch (Exception e) {
					throw new Exception("Port value not given after -p argument");
				}
			} else if (args[i].equals("-mx")) {

				if (!changedRequestType) {
					queryType = DNSQueryType.MX;
					changedRequestType = true;
				} else {
					throw new Exception("Cannot have both -ns and -mx flags");
				}
			} else if (args[i].equals("-ns")) {

				if (!changedRequestType) {
					queryType = DNSQueryType.NS;
					changedRequestType = true;
				} else {
					throw new Exception("Cannot have both -mx and -ns flags");
				}
			} else if (args[i].charAt(0) == '@') {
				// Check that domain name is the last argument
				if ((i + 1) != (n - 1)) {
					throw new Exception("Server IP and Domain Name should be the last 2 arguments");
				}

				// Take IP address without the @
				serverIP = args[i].substring(1);
				String[] serverBytes = serverIP.split("\\.");
				int index = 0;
				if (serverBytes.length != 4) {
					throw new Exception("Wrong length of ip! Please input valid IP address");
				} else {
					for (String strByte : serverBytes) {
						int ipByte = Integer.parseInt(strByte);
						if (ipByte < 0 || ipByte > 255) {
							throw new Exception("Invalid IP address, not between 0 and 255.");
						} else {
							server[index++] = (byte) ipByte;
						}
					}
				}
				// Domain name is the last argument
				domainName = args[i + 1];
				return;

			} else {
				throw new Exception("Found an invalid argument");
			}
		}

		throw new Exception("Server IP not found");
	}

	static byte[] getRequest() throws Exception {
        int length = domainName.length();
        // Create a byte array for the request 
        byte[] request = new byte[HEADER_SIZE + QTYE_QCLASS_SIZE + length];
        
        // Setup header
        byte[] flags = new byte[]{0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        // Generate randomized id 
        byte[] id = new byte[2];
        new Random().nextBytes(id);
        
        // Set randomly generated id and flags
		System.arraycopy(id, 0, request, 0, 2);
		System.arraycopy(flags, 2, request, 0, 10);

		// Header is set, now split domain name in labels and add them to the request
		int current_index = HEADER_SIZE;
		for (String label : domainName.split("\\.")) {
			// Set a byte in the request for the length of the next label
			request[current_index++] = (byte) label.length();
			// Iterate through the chars in each label and convert them to bytes in ascii
			for (int i = 0; i < label.length(); i++) {
				request[current_index++] = (byte) ((int)label.charAt(i));
			}
		}
	
		// Add an end of name byte 
		request[current_index++] = (byte)0x00;
		
		// Set query type
		request[current_index++] = (byte)0x00;

        switch (queryType){
	        case A:
	            request[current_index++] = (byte)0x01;
	            break;
	        case MX:
	        	request[current_index++] = (byte)0x0f;
	            break;
	        case NS:
	        	request[current_index++] = (byte)0x02;
	            break;
	    }
        
        request[current_index++] = (byte)0x00;
        request[current_index++] = (byte)0x01;

		return request;
	}
	
	static DatagramPacket sendRequest(byte[] request, int numOfRetries) throws Exception {
		if (numOfRetries > maxRetries) {
			throw new Exception("Maximum number of retries (" + maxRetries+ ") exceeded.");
		}
		try {
			// Create socket
            DatagramSocket clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(timeout); // Set up timeout limit
            
            // Create InetAddress used by datagram packet
            InetAddress ipAddress = InetAddress.getByAddress(server);
            
            // Create an array to receive the data
            byte[] response = new byte[1024];
            
            //create packets
            DatagramPacket sentPacket = new DatagramPacket(request, request.length, ipAddress, port);
            DatagramPacket receivedPacket = new DatagramPacket(response, response.length);
            long startTime = System.currentTimeMillis();
            clientSocket.send(sentPacket);
            clientSocket.receive(receivedPacket);
            long endTime = System.currentTimeMillis();
            
            long requestTime = (endTime-startTime) / 1000;
            System.out.println("Time to get the response:  " + requestTime);
            
            // Check the ids are the same
    		if (request[0] != response[0] || request[1] != response[1]) {
    			throw new Exception("Received response ID does not match the Request ID.");
    		}
    		
    		validateAndGetResponse(response);
    			        
	    } catch (SocketException e) {
			throw new Exception("Failed to create the socket.");
		} catch (UnknownHostException e) {
			throw new Exception("The host is unknown.");
		} catch (SocketTimeoutException e) {
			System.out.println("The socket has exceeded the timeout.");
			System.out.println("Retryinh...");
			sendRequest(request, numOfRetries++);
		} 
	
	}

	static void validateAndGetResponse(byte[] response) throws Exception {
		 if (((response[2]>>7)&1) != 1) {
	            throw new Exception("Received response is a query, not a response.");
	        }
	        if (((response[3]>>7)&1) != 1) {
	            throw new Exception("Server does not support recursive queries.");
	        }
	        switch(response[3] & 0x0F){
	            case 1:
	                throw new Exception("Format error: the name server was unable to interpret the query.");
	            case 2:
	                throw new Exception("Server failure: the name server was unable to process this query due to a problem with the name server.");
	            case 3:
	                throw new Exception("Name error: meaningful only for responses from an authoritative name server, the code signifies that the domain name referenced in the query does not exist.");
	            case 4:
	                throw new Exception("Not implemented: the name server does not support the requested kind of query.");
	            case 5:
	                throw new Exception("Refused: the name server refuses to perform the requested operation for policy reasons.");
	            default:
	                break;
	        }
	        
	        
			int ANCount = ((response[6] & 0xff) << 8) + (response[7] & 0xff);
			int NSCount = ((response[8] & 0xff) << 8) + (response[9] & 0xff);
			int ARCount = ((response[10] & 0xff) << 8) + (response[11] & 0xff);

			
	}
	static void printQuerySummary() {
		System.out.println("DnsClient sending request for " + domainName + "\n" + "Server: " + serverIP + "\n"
				+ "Request type: " + queryType.name() + "\n");
	}
}
