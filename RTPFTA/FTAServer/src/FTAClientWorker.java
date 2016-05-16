import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * CS-3251
 * FTAClientWorker.java
 * Purpose: FTAClientWorker communicates with the client and enables multithreading
 *
 * @author Brian Eason
 * @author Nico de Leon
 * @author Duc Tran
 * @email brianeason92@gmail.com
 * @email nico@ns.gg
 * @version 1.0 4/20/2016
 */
public class FTAClientWorker implements Runnable {
	private RTPSocket clntSock;
	private BlockingQueue<Path> uploadQueue = new ArrayBlockingQueue<>(16);
	private DataInputStream in;
	private DataOutputStream out;
	
	public FTAClientWorker(RTPSocket client) throws IOException {
		clntSock = client;
		in = new DataInputStream(clntSock.getInputStream());
		out = new DataOutputStream(clntSock.getOutputStream());
	}

	/*
	 * Receives the message from the client, parses the message
	 * Sends the file back to the client
	 * If the client requested a get-post, the server downloads and copys the file
	 */
	public void run(){
		(new Thread(new Uploader())).start();
		try {
			while (true) {
				String message = in.readUTF();
				String[] args = message.split(" ");
				uploadQueue.put(Paths.get(args[1]));
				System.out.println("Received message: " + message);

				//check to see if this request wants us to download a file
				if ("get-post".equals(args[0])) {
					String postFileName = args[2];
					String fileType = postFileName.substring(postFileName.lastIndexOf('.'));
					int postFileLength = in.readInt();
					byte[] postFile = new byte[postFileLength];
					in.readFully(postFile);
					Files.write(Paths.get("post_F" + fileType), postFile, StandardOpenOption.CREATE);
					System.out.println("File Written!");
				}
			}
		} catch(EOFException ex) {
			// connection closed
		} catch(Exception ex) {
			ex.printStackTrace();
			System.out.println("Connection Failed");
		}
	}

	/*
	 * Sends the requested file back to client
	 */
	private class Uploader implements Runnable {
		public void run() {
			while (true) {
				try {
					Path getFile = uploadQueue.take();
					out.writeInt((int) Files.size(getFile));
					Files.copy(getFile, out);
					System.out.println("Byte[] for file " + getFile.toString() + " sent!\n");
				} catch (FileNotFoundException e) {
					System.err.println("File cannot be found" + e);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}
}
