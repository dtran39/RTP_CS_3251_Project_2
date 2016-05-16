
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
/**
 * CS-3251
 * FTAclient.java
 * Purpose: FTAclient simulation for RTP Socket
 *
 * @author Brian Eason
 * @author Nico de Leon
 * @author Duc Tran
 * @email brianeason92@gmail.com
 * @email nico@ns.gg
 * @version 1.0 4/20/2016
 */
public class FTAclient {
	private static BlockingQueue<String> downloadQueue = new ArrayBlockingQueue<>(16);
	private static DataInputStream in;
	private static DataOutputStream out;

	public static void main(String[] args) throws IOException{
		boolean connected = false;
		int port;
		String IP;
		int rwnd;
		Thread downloadThread = new Thread(new Downloader());
		if(args.length<1 && args.length>3){
			throw new IllegalArgumentException("\nCommand: FTAclient H:P W \n");
		}
		else if(args[0].contains(":")){
			String IP_port = args[0];
			//convert IP_port to IP address and port
			String[] IP_port_array = IP_port.split(":");
			IP = IP_port_array[0];
			port = Integer.parseInt(IP_port_array[1]);
			rwnd = Integer.parseInt(args[1]);
			RTPSocket socket = new RTPSocket(IP, port);
			socket.setReceiveBufferSize(rwnd);
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
			connected = true;
			System.out.println("FTAclient is running");
			downloadThread.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

			//loop until disconnect
			while(connected){
				String input = reader.readLine();
				String[] inputArr = input.split(" ");
		
				try {
					if(inputArr[0].equals("get")) {
						if(inputArr.length!=2){
							System.out.println("\nIncorrect \nUse Command: get F\n"
									+ "Command: get-post F G");
						} else {
							out.writeUTF(input);
							downloadQueue.put(inputArr[1]);
						}
					} else if(inputArr[0].equals("get-post")) {
						if(inputArr.length!=3){
							System.out.println("\nIncorrect \nUse Command: get F\n"
									+ "Command: get-post F G");
						} else {
							out.writeUTF(input);
							downloadQueue.put(inputArr[1]);
							upload(out, inputArr);
						}
					} else if(input.equals("disconnect")) {
						//out.writeUTF("disconnect");
						connected = false;
						downloadThread.interrupt();
						socket.close();
						reader.close();
					} else {
						System.out.println("\nIncorrect \nUse Command: get F\n"
								+ "Command: get-post F G");
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
			}
			System.err.println("main thread dead");
		} else {
			throw new IllegalArgumentException("Command line: FTAclient H:P W \n ");
		}
	}
		
	/*
	 * Uploads the file to the server
	 * @param out the DataOutputStream 
	 * @param args[] the arguments from the terminal
	 */
	public static void upload(DataOutputStream out, String[] args) throws IOException {
		//parse message
		String getFileName = args[1];
		String getFileType = getFileName.substring(getFileName.lastIndexOf('.'));
		try {
			Path postFile = Paths.get(args[2]);
			out.writeInt((int) Files.size(postFile));
			Files.copy(postFile, out);
		} catch (FileNotFoundException ex) {
			System.err.println("File not found:");
			ex.printStackTrace();
		}
	}

	/*
	 * Sends the message or file to the server
	 */
	private static class Downloader implements Runnable {
		public void run() {
			try {
				while (true) {
					try {
						String getFileName = downloadQueue.take();
						String getFileType = getFileName.substring(getFileName.lastIndexOf('.'));
						int getFileLength = in.readInt();
						byte[] getFile = new byte[getFileLength];
						in.readFully(getFile);
						Files.write(Paths.get("get_F" + getFileType), getFile, StandardOpenOption.CREATE);
						System.out.println("file written!\n");
					} catch (FileNotFoundException e) {
						System.err.println("File cannot be found" + e);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} catch (InterruptedException e) {
				// disconnect
				System.err.println("download thread dead");
			}
		}
	}
}
