import java.io.IOException;
import java.net.ServerSocket;
/**
 * CS-3251
 * FTAserver.java
 * Purpose: FTAsever simulation for RTP Socket
 *
 * @author Brian Eason
 * @author Nico de Leon
 * @author Duc Tran
 * @email brianeason92@gmail.com
 * @email nico@ns.gg
 * @version 1.0 4/20/2016
 */
public class FTAserver {
	public static void main(String[] args) throws IOException{
		if(args.length!=2)
			throw new IllegalArgumentException("Parameters: <Port> <receiver's window>");
		
		int servPort = Integer.parseInt(args[0]);
		int rwnd = Integer.parseInt(args[1]);
		
		//create new server socket
		RTPServerSocket servSock = new RTPServerSocket(servPort);
		servSock.setReceiveBufferSize(rwnd);
		
		System.out.println("FTA Server is running");
			
		//begin server socket infinite loop
		for(;;){
			FTAClientWorker worker;	
			try {
				worker = new FTAClientWorker(servSock.accept());
				Thread thread = new Thread(worker);
				thread.start();
			} catch (IOException e) {
				System.out.println("Error accepting client connection");
			}
		}
	}
}
