import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.HashMap;
import java.util.Map;
/**
 * CS-3251
 * RTPServerSocket.java
 * Purpose: Contains the necesesary components for an RTP Server Socket
 *
 * @author Brian Eason
 * @author Nico de Leon
 * @author Duc Tran
 * @email brianeason92@gmail.com
 * @email nico@ns.gg
 * @version 1.0 4/20/2016
 */
public class RTPServerSocket {
    private static final int MAX_CONNECTIONS = 256;

    private final BlockingQueue<RTPSocket> pendingConnections = new ArrayBlockingQueue<>(MAX_CONNECTIONS);
    private final DatagramSocket socket;
    private final AtomicBoolean hasListeningThread = new AtomicBoolean(false);
    private final Map<SocketAddress, RTPSocket> connections = new HashMap<>();
    private Thread listenThread;
    private int receiveBufferSize = 100000;

	/*
	Constructor
	@param port, the port number the server will use
	*/
    public RTPServerSocket(int port) throws IOException {
        socket = new DatagramSocket(port);
    }
	
	/*
	Method for accepting a new connection
	@return RTPSocket, the socket that has established a new connection
	*/
    public RTPSocket accept() throws IOException {
        if (hasListeningThread.compareAndSet(false, true)) {
            listenThread = new Thread(new Listener());
            listenThread.start();
        }
        try {
            return pendingConnections.take();
        } catch (InterruptedException ex) {
            throw new IOException(ex);
        }
    }

	/*
	Closes the connection
	*/
    public void close() {
        listenThread.interrupt();
    }
	
	/*
	Sets the Receive Buffers' Size
	@param size, the size of the receive buffer
	*/
    public void setReceiveBufferSize(int size) {
        receiveBufferSize = size;
        connections.forEach((address, connection) -> connection.setReceiveBufferSize(receiveBufferSize));
    }

	/*
	Class that handles the datagram packets on the server side
	The connection stays open until the connection is closed
	*/
    private class Listener implements Runnable {
        public void run() {
            final DatagramPacket incomingPacket = new DatagramPacket(new byte[RTPSocket.MAX_BUFFER], RTPSocket.MAX_BUFFER);

            try {
                while (!Thread.interrupted()) {
                    socket.receive(incomingPacket);
                    RTPSocket connection = connections.get(incomingPacket.getSocketAddress());
                    if (connection == null) {
                        connection = new RTPSocket(incomingPacket.getSocketAddress(), socket);
                        connection.setReceiveBufferSize(receiveBufferSize);
                        connections.put(incomingPacket.getSocketAddress(), connection);
                        pendingConnections.put(connection);
                    }
                    connection.receivePacket(incomingPacket);
                }
            } catch (IOException ex) {
                // close() called or something strange happened
                ex.printStackTrace();
            } catch (InterruptedException ex) {
                // close() called or something strange happened
                ex.printStackTrace();
            }

            try {
                for (RTPSocket connection : connections.values()) {
                    connection.close();
                }
                while (!connections.isEmpty()) {
                    socket.receive(incomingPacket);
                    RTPSocket connection = connections.get(incomingPacket.getSocketAddress());
                    if (connections != null) {
                        connection.receivePacket(incomingPacket);
                        if (connection.isClosed()) {
                            connections.remove(incomingPacket.getSocketAddress());
                        }
                    }
                }
            } catch (IOException ex) {
                // something strange happened
                ex.printStackTrace();
            }
        }
    }
}
