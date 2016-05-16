Brian Eason
brianeason92@gmail.com

Nico de Leon
nico@ns.gg

Duc Tran
trananhduc@gatech.edu

------Name and Description of Files------
/dbRTP: directory containing java and class files for the database application
/RTPFTA: directory containing java and class files for the file transfer application
Design Report.pdf: contains the high level report about the RTP class and how it works
RTPPacket.java: Java file containing code for an individual RTP packet
RTPServerSocket.java: Java file containing code for the RTP Server Socket
RTPSocket.java: Java file containing code for a RTP Socket

--/dbRTP/src--
dbclientRTP.java: Java file containing the code for the client side of the database application
dbengineRTP.java: Java file containing the code for the server side of the database application
RTPPacket.java: Java file containing code for an individual RTP packet
RTPServerSocket.java: Java file containing code for the RTP Server Socket
RTPSocket.java: Java file containing code for a RTP Socket

--/dbRTP/bin--
Contains all the runnable class files for the database application

--/RTPFTA/FTAClient/src--
FTAclient.java: Java file containing code for the client side of the FTA application
RTPPacket.java: Java file containing code for an individual RTP packet
RTPServerSocket.java: Java file containing code for the RTP Server Socket
RTPSocket.java: Java file containing code for a RTP Socket
3251.jpg: The jpg file that will be transfered

--/RTPFTA/FTAServer/bin--
Contains all the runnable class files for the client side of the FTA
FTAserver.java: Java file containing code for the server side of the FTA application
RTPPacket.java: Java file containing code for an individual RTP packet
RTPServerSocket.java: Java file containing code for the RTP Server Socket
RTPSocket.java: Java file containing code for a RTP Socket
3251.jpg: The jpg file that will be transfered

------How to Run and Compile------
The java files have already been compiled.

To run the database, go to /dbRTP/bin:
To start the server open the cmd prompt here and do: 
	java dbengineRTP <port number>

To start the client, open another cmd prompt and do: 
	java dbclientRTP 127.0.0.1:<port number> <student id> <parameters>
	Applicable parameters are: "first_name", "last_name", "quality_hours", "gpa_hours", "gpa"

To run the FTA, go to /RTPFTA/bin:
To start the server open the cmd prompt here and do:
	java FTAserver <port number> <receiver window>

To start the client open the cmd prompt here and do:
	java FTAclient 127.0.0.1:<port number> <receiver window>
	Then, you may do the following commands:
	get <file name>
		This will get the requested file name
	get-post <request file name> <send file name>
		This will simultaneously get the requested file name and send the requested file name
	disconnect
		This will disconnect the client from the server

Documentation:
	See Design Report.pdf for the report

Known Bugs:

