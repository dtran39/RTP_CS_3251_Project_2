import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Arrays;
import java.util.Deque;

/**
 * CS-3251
 * RTPSocket.java
 * Purpose: Contains the necesesary components for an RTP Socket
 *
 * @author Brian Eason
 * @author Nico de Leon
 * @author Duc Tran
 * @email brianeason92@gmail.com
 * @email nico@ns.gg
 * @version 1.0 4/20/2016
 */
public class RTPSocket {
    public static final    int MAX_BUFFER = 1000,
                            MAX_DATA_SIZE = MAX_BUFFER - RTPPacket.HEADER_SIZE,
                            TIMEOUT_IN_MS = 400;
    // thread-safe. only need 1 because timeouts are IO-blocked
    private static final ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor();

    private final ConcurrentHashMap<Integer, DatagramPacket> unacked = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, RTPPacket> unprocessed = new ConcurrentHashMap<>();

    private final PipedInputStream applicationInputStream = new PipedInputStream();
    private final PipedOutputStream applicationOutputStream = new PipedOutputStream();
    private final InputStream dataFromApplicationStream;
    private final OutputStream dataToApplicationStream;

    private final SocketAddress address;
    private final DatagramSocket socket;
    private final Lock sendLock = new ReentrantLock();

    private final AtomicInteger seq = new AtomicInteger();
    private final AtomicInteger ack = new AtomicInteger(-1);
    private final AtomicInteger receivedAck = new AtomicInteger(-1);

    private Thread listenThread;
    private Thread consumeThread;

	/*
	Constructor
	@param address, the string version of the address the socket will connect only
	@param port, the port number that the client wishes to connect to
	*/
    public RTPSocket(String address, int port) throws IOException {
        this(InetAddress.getByName(address), port);
    }

	/*
	Constructor
	@param address, the InetAddress version of the address the socket will connect only
	@param port, the port number that the client wishes to connect to
	*/
    public RTPSocket(InetAddress address, int port) throws IOException {
        this(new InetSocketAddress(address, port), new DatagramSocket());
        listenThread = new Thread(new Listener());
        listenThread.start();
        // send SYN
        state.set(State.BEGIN);
        RTPPacket synPacket = new RTPPacket();
        synPacket.setSyn(true);
        sendPacket(synPacket);
    }

    /* 
	Constructor for RTPServerSocket use
	@param remote, the socket address
	@param outgoingSocket, the socket that data will be sent to
	*/
    RTPSocket(SocketAddress remote, DatagramSocket outgoingSocket) throws IOException {
        dataFromApplicationStream = new BufferedInputStream(new PipedInputStream(applicationOutputStream));
        dataToApplicationStream = new PipedOutputStream(applicationInputStream);
        address = remote;
        socket = outgoingSocket;
        //System.err.println("before consumer thread start");
        consumeThread = new Thread(new Consumer());
        consumeThread.start();
        //System.err.println("after consumer thread start");
    }
	
	/*
	Gets the input stream
	@return InputStream
	*/
    public InputStream getInputStream() {
        return applicationInputStream;
    }
	
	/*
	Gets the out output stream
	@return OutputStream
	*/
    public OutputStream getOutputStream() {
        return applicationOutputStream;
    }
	
	/*
	Class that listens for the datagram packets
	The connection stays open until the connection is closed
	*/
    private class Listener implements Runnable {
        public void run() {
            final DatagramPacket incomingPacket = new DatagramPacket(new byte[MAX_BUFFER], MAX_BUFFER);
            //System.err.println("Client thread start");
            try {
                while (state.get() != State.CLOSED) {
                    socket.receive(incomingPacket);
                    ////System.err.println("packet received");
                    receivePacket(incomingPacket);
                }
                socket.close();
            } catch (IOException ex) {
                State currentState = state.get();
                if (currentState == State.FINSENT || currentState == State.TIMEWAIT || currentState == State.CLOSED) {
                    // expected pipe broken, no issues
                } else {
                    //System.err.println(currentState);
                    ex.printStackTrace();
                }
            }
            timeoutScheduler.shutdownNow();
            //System.err.println("listen thread dead");
        }
    }
	
	/*
	Sends a packet and sets the seq and ack numbers
	@param packet, the packet to be sent
	*/
    private void sendPacket(RTPPacket packet) {
        packet.setSeqNum(seq.getAndIncrement());
        packet.setAckNum(ack.get());
        byte[] rawBytes = packet.getBytes();
        DatagramPacket rawPacket = new DatagramPacket(rawBytes, rawBytes.length, address);
        unacked.put(packet.getSeqNum(), rawPacket);
        ////System.err.println("sent");
        ////System.err.println(packet.flagsToInt());
        setTimeout(packet.getSeqNum(), TIMEOUT_IN_MS);
        sendRawPacket(rawPacket);
    }

    private void sendAckPacket() {
        State currentState = state.get();
        if (currentState == State.FINSENT || currentState == State.TIMEWAIT || currentState == State.CLOSED) return;
        RTPPacket packet = new RTPPacket(-1, ack.get(), new byte[0]);
        byte[] rawBytes = packet.getBytes();
        DatagramPacket rawPacket = new DatagramPacket(rawBytes, rawBytes.length, address);
        sendRawPacket(rawPacket);
    }

	/*
	Sends a raw packet
	@param rawPacket, the packet to be sent
	*/
    private void sendRawPacket(DatagramPacket rawPacket) {
        try {
            socket.send(rawPacket);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

	/*
	Sets the timeout
	@param sequence, the sequence number of a packet
	@param milliseconds, the amount of time for a timeout
	*/
    private void setTimeout(int sequence, int milliseconds) {
        timeoutScheduler.schedule(() -> {
            DatagramPacket unackedPacket = unacked.get(sequence);
            if (unackedPacket != null && state.get() != State.CLOSED) {
                // resend the lost packet
                ////System.err.println("timeout expired");
                ////System.err.println(sequence);
                sendRawPacket(unackedPacket);
                setTimeout(sequence, milliseconds);
            }
        }, milliseconds, TimeUnit.MILLISECONDS);
    }



	/*
	Consumer class that processes the datagram packets
	*/
    private class Consumer implements Runnable {
        public void run() {
            try {
                // create packets by reading from dataFromApplicationStream
                while (true) {
                    do {
                        ////System.err.println("before read");
                        byte[] buffer = new byte[MAX_DATA_SIZE];
                        int bytesRead = dataFromApplicationStream.read(buffer, 0, MAX_DATA_SIZE);
                        if (bytesRead == -1) {
                            throw new IOException("application pipe disconnected");
                        }
                        buffer = Arrays.copyOf(buffer, bytesRead);
                        pendingUnsentPackets.add(new DatagramPacket(buffer, bytesRead, address));
                    } while (dataFromApplicationStream.available() > 0);
                    // maybe combine with shorter-than-maximum-length pending packets
                    sendPendingPackets();
                }
            } catch (IOException ex) {
                State currentState = state.get();
                if (currentState == State.FINSENT || currentState == State.TIMEWAIT || currentState == State.CLOSED) {
                    // expected pipe broken, no issues
                } else {
                    ex.printStackTrace();
                }
            }
            //System.err.println("consume thread dead");
        }
    }

    private final Deque<DatagramPacket> pendingUnsentPackets = new ConcurrentLinkedDeque<>();
    private AtomicInteger window = new AtomicInteger(), windowLimit = new AtomicInteger(100);
    private volatile int receiveBufferSize = 100000;
    // may need an enum for state
    private enum State {
        BEGIN, ESTABLISHED, FINSENT, TIMEWAIT, CLOSED 
    }
    private AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    // add any other variables

	/*
	sets the receive buffers' size
	@param size, the size to set the buffer
	*/
    public void setReceiveBufferSize(int size) {
        receiveBufferSize = size;
    }

	/*
	Closes the RTP Sockets connection
	*/
    public void close() throws IOException {
        // start the connection close process
        // Send FIN packet(no data), transit to FINWAIT1
        if (state.compareAndSet(State.ESTABLISHED, State.FINSENT)) {
            RTPPacket finPacket = new RTPPacket();
            finPacket.setFin(true);
            sendPacket(finPacket);
            dataFromApplicationStream.close();
            dataToApplicationStream.close();
            applicationInputStream.close();
            applicationOutputStream.close();
        }
    }
	
	/*
	Checks to see if the connection is closed
	@return boolean, 1 if closed, 0 if not
	*/
    public boolean isClosed() {
        return state.get() == State.CLOSED;
    }

    /*
	Recives the packet	
    @param packet An incoming packet from the other side of the connection.
     */
    public void receivePacket(DatagramPacket udpPacket) throws IOException {
        // decode the packet
        RTPPacket packet = new RTPPacket(udpPacket.getData(), udpPacket.getLength());
        // if it's invalid ignore
        int oldCheckSum = packet.getChecksum(); packet.updateChecksum(); int newChecksum = packet.getChecksum();
        if (oldCheckSum != newChecksum) {
            //System.err.printf("Checksum failed. expected %d, got %d%n", oldCheckSum, newChecksum);
        }
        // clear timeouts up to the ack number
        receivedAck.accumulateAndGet(packet.getAckNum(), (ackedSeq, limit) -> {
            while (ackedSeq < limit) {
                unacked.remove(++ackedSeq);
            }
            return ackedSeq;
        });
        ////System.err.println("received ack");
        ////System.err.println(packet.getAckNum());
        int packetSeq = packet.getSeqNum();

        if (packetSeq > -1) {
            sendAckPacket();
        }

        if (ack.compareAndSet(packetSeq - 1, packetSeq)) {
            processPacket(packet);
            packetSeq++;
            while (unprocessed.contains(packetSeq) && ack.compareAndSet(packetSeq - 1, packetSeq)) {
                processPacket(unprocessed.get(packetSeq));
                unprocessed.remove(packetSeq);
                packetSeq++;
            }
        } else if (packetSeq > ack.get()) {
            unprocessed.put(packetSeq, packet);
        }
    }

	/*
	Proecesses the packet
	@param packet, the packet to proecess
	*/
    private void processPacket(RTPPacket packet) throws IOException {
        RTPPacket response = new RTPPacket();
        if (packet.isSyn()) {
            response.setAck(true);
            if (state.compareAndSet(State.CLOSED, State.BEGIN)) {
                response.setSyn(true);
            }
        }
        if (packet.isFin()) {
            response.setAck(true);
            if (state.compareAndSet(State.ESTABLISHED, State.FINSENT)) {
                response.setFin(true);
                dataFromApplicationStream.close();
                dataToApplicationStream.close();
            }
        }
        if (packet.isAck()) {
            state.compareAndSet(State.BEGIN, State.ESTABLISHED);
            if (state.compareAndSet(State.FINSENT, State.TIMEWAIT)) {
                timeoutScheduler.schedule(() -> {
                    state.compareAndSet(State.TIMEWAIT, State.CLOSED);
                    if (listenThread != null) socket.close();
                }, 1, TimeUnit.SECONDS);
            }
        }
        if (packet.isFin() || packet.isSyn() || packet.isAck()) {
            sendPacket(response);
            return;
        }
        // update the windows
        if (state.get() == State.ESTABLISHED && packet.getData().length > 0) {
            // if it's data, set ack# (even if duplicate) and write to dataToApplicationStream
            byte[] data = packet.getData();
            dataToApplicationStream.write(data, 0, data.length);
            sendPendingPackets();
        } else {
            sendAckPacket();
        }

    }

    /*
    Send packets until the windows are full or there are no pending packets to send.
    */
    private void sendPendingPackets() {
        // send packets up to the window limit or until no pending packets
        ////System.err.println("sending pending");
        sendLock.lock();
        try {
            while (/*window.get() < windowLimit.get() && */pendingUnsentPackets.size() > 0) {
                DatagramPacket udpPacket = pendingUnsentPackets.poll(); 
                if (udpPacket != null) {
                    RTPPacket packet = new RTPPacket(0, 0, udpPacket.getData());
                    sendPacket(packet);
                    window.getAndIncrement();
                    ////System.err.println("packet sent");
                }
            }
            sendAckPacket();
        } finally {
            sendLock.unlock();
        }
    }
}
