import java.io.*;
import java.net.*;
import java.util.Random;

public class DnsClient {

	//Request parameters
	static int timeout = 5000;
	static int maxRetries = 3;
	static int port = 53;
	static DNSQueryType queryType = DNSQueryType.A;

	static String serverString = "";
	private static byte[] serverIP = new byte[4];
	static String domainName = "";


	// Fields necessary for request
    static int HEADER_SIZE = 12;
    static int QTYE_QCLASS_SIZE = 4;
    
	public static void main(String[] args) {

		//Just for debugging
		args = new String[2];
		args[0] = "@8.8.8.8";
		args[1] = "www.amazon.com";

		try {

			// Initialise the request parameters
			processInputs(args);

			// Display a summary of the query
			printQuerySummary();

			//Create request
			byte[] request = createRequest();


			sendRequest(request, 0);

		} catch (Exception e) {
			System.out.println("ERROR: \t " + e.getMessage());
		}

	}

	// Helper methods

	/*
	 * Initializes timeout, maxRetries, port, queryType, serverIP, domainName
	 *
	 * The method takes user input, handles errors and sets up the request parameter
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

					if(timeout > 60000){
						throw new Exception("The timeout option must be at most 60 seconds");
					}
					i++;
				} catch (Exception e) {
					throw new Exception("Timeout value not given after -t argument");
				}
			} else if (args[i].equals("-r")) {

				try {
					maxRetries = Integer.parseInt(args[i + 1]);

					if(maxRetries < 0 || maxRetries > 100){
						throw new Exception("The max retries option must be at least 0 and at most 100");
					}
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
				serverString = args[i].substring(1);
				String[] serverBytes = serverString.split("\\.");
				int index = 0;
				if (serverBytes.length != 4) {
					throw new Exception("Wrong length of ip! Please input valid IP address");
				} else {

					//Insert IP address in array server
					for (String strByte : serverBytes) {

						int ipByte;
						try {
							ipByte = Integer.parseInt(strByte);
						}catch (Exception e){
							throw new Exception("Invalid IP address");
						}
						if (ipByte < 0 || ipByte > 255) {
							throw new Exception("Invalid IP address, not between 0 and 255.");
						} else {
							serverIP[index++] = (byte) ipByte;
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


	/*
	 * Creates the request header and questions
	 *
	 * The method returns the request bytes
	 */
	static byte[] createRequest() throws Exception {

		//Calculate the length of the QNAME parameter
		int qNameLength = 0;
		for (String label : domainName.split("\\.")) {

			//For each label, add 1 for the length octet, and then 1 for every character in it
			qNameLength += 1 + label.length();
		}

		// the zero-length octet, representing the null label of the root
		qNameLength++;


		// Create a byte array for the request
        byte[] request = new byte[HEADER_SIZE + QTYE_QCLASS_SIZE + qNameLength];
        
        // Setup header
        byte[] flags = new byte[]{0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

        // Generate randomized id 
        byte[] id = new byte[2];
        new Random().nextBytes(id);
        
        // Set randomly generated id
		System.arraycopy(id, 0, request, 0, id.length);

		//Set flags into request header
		System.arraycopy(flags, 0, request, 2, flags.length);


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

		// Set first 2 octets for query type
		request[current_index++] = (byte)0x00;

		// Set query type
		switch (queryType) {
			case A -> request[current_index++] = (byte) 0x01;
			case MX -> request[current_index++] = (byte) 0x0f;
			case NS -> request[current_index++] = (byte) 0x02;
		}

		//Set QCLASS
        request[current_index++] = (byte)0x00;
        request[current_index] = (byte)0x01;

		return request;
	}

	/*
	 * Sends the request and waits to receive a response
	 * The response is then sent to the validateAndPrintResponse method
	 *
	 * The method takes the request as argument, and the numOfRetries which should be initialized at 0
	 */
	static void sendRequest(byte[] request, int numOfRetries) throws Exception {

		// Create an array to receive the data
		byte[] response = new byte[1024];

		try {

			// Create socket
            DatagramSocket clientSocket = new DatagramSocket();
            clientSocket.setSoTimeout(timeout); // Set up timeout limit

            // Create InetAddress used by datagram packet
            InetAddress ipAddress = InetAddress.getByAddress(serverIP);


            //create packets
            DatagramPacket sentPacket = new DatagramPacket(request, request.length, ipAddress, port);
            DatagramPacket receivedPacket = new DatagramPacket(response, response.length);

			long startTime = System.currentTimeMillis();
            clientSocket.send(sentPacket);
            clientSocket.receive(receivedPacket);
            long endTime = System.currentTimeMillis();

            float requestTime = (endTime-startTime) / 1000f;
			System.out.println("Response received after " + requestTime + " seconds (" + numOfRetries + " retries)");

            // Check the ids are the same
    		if (request[0] != response[0] || request[1] != response[1]) {
    			throw new Exception("Received response ID does not match the Request ID.");
    		}

			DNSResponse r = new DNSResponse(response, request.length, queryType);
			r.validate();
			r.print();

	    } catch (SocketException e) {
			throw new Exception("Failed to create the socket.");

		} catch (UnknownHostException e) {
			throw new Exception("The host is unknown.");

		} catch (SocketTimeoutException e) {

			System.out.println("The socket has exceeded the timeout.");

			if (numOfRetries >= maxRetries) {
				throw new Exception("Maximum number of retries (" + maxRetries + ") exceeded.");

			}else{
				System.out.println("Retrying...");
				sendRequest(request, numOfRetries + 1);
			}
		}
	}



	static void printQuerySummary() {
		System.out.println("DnsClient sending request for " + domainName + "\n" + "Server: " + serverString + "\n"
				+ "Request type: " + queryType.name() + "\n");
	}



}
