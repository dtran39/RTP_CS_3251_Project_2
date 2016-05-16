import java.net.*;
import java.io.*;

/**
 * CS-3251
 * dbclientRTP.java
 * Purpose: Simulates a RTP client
 *
 * @author Brian Eason
 * @author Nico de Leon
 * @author Duc Tran
 * @email brianeason92@gmail.com
 * @email nico@ns.gg
 * @version 1.0 4/20/2016
 */
public class dbclientRTP {
	public static void main(String[] args)throws IOException{
		if((args.length < 3) || (args.length > 7)){
			throw new IllegalArgumentException("Parameter(s): dbclientRTP IP:Port ID flags");
		}
		String IP_port = args[0];
		//convert IP_port to IP address and port
		String[] IP_port_array = IP_port.split(":");
		String IP = IP_port_array[0];
		int port = Integer.parseInt(IP_port_array[1]);
				
		//convert args to message
		String message = "";
		for(int i = 1; i < args.length; i++){
			message = message + " " + args[i];
		}		
		
		//Create socket that is connected to server on specified port
		//tries to connect to server first
		try{
			RTPSocket socket = new RTPSocket(IP, port);

			DataInputStream in = new DataInputStream(socket.getInputStream());
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());

			//send the encoded string to server
			out.writeUTF(message);
			
			
			//receive and print message
			System.out.println(in.readUTF());
			
			//send message to terminate connection
			out.writeUTF("disconnect");
			
			//close connection
			socket.close();
			}
		
		//if connection fails, exit
		catch(ConnectException e){
			System.out.println("RTP server is not running");
		}
	}
		
}

