# ECSE316
Program was written and tested using Java version 17 
### To run the project, open the terminal in the source folder and execute the following:
#### 1. Compile the code with:
javac DnsClient.java
#### 2. Then, run with the following syntax:
java DnsClient [-t timeout] [-r max-retries] [-p port] [-mx|-ns] @server name

Where the arguments are defined as follows:
- **timeout** (optional) gives how long to wait, in seconds, before retransmitting an
unanswered query. Default value: 5.
- **max-retries**(optional) is the maximum number of times to retransmit an
unanswered query before giving up. Default value: 3.
- **port** (optional) is the UDP port number of the DNS server. Default value: 53.
- **-mx or -ns flags** (optional) indicate whether to send a MX (mail server) or NS (name server)
query. At most one of these can be given, and if neither is given then the client will send a
type A (IP address) query.
- **server** (required) is the IPv4 address of the DNS server, in a.b.c.d. format
- **name** (required) is the domain name to query for.
  
