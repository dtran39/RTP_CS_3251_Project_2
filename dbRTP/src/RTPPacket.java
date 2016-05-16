import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
/**
 * CS-3251
 * RTPPacket.java
 * Purpose: Contains the necesesary components for an RTP Packet
 *
 * @author Brian Eason
 * @author Nico de Leon
 * @author Duc Tran
 * @email brianeason92@gmail.com
 * @email nico@ns.gg
 * @version 1.0 4/20/2016
 */
public class RTPPacket implements Comparable<RTPPacket> {
	public static final int HEADER_SIZE = 16;
	private int seqNum, ackNum; // 8
	private int checksum; // 4
	private boolean isAck, isSyn, isFin; // 4
	private byte[] data = new byte[0];

	/*
	Constructor with no arguments
	*/
	public RTPPacket(){}
	
	/*
	Constructor
	@param seqNum, the sequence number of the packet
	@param ackNum, the acknowledge number of the packet
	@param data, the byte array for all the data the packet will contain
	*/
	public RTPPacket(int seqNum, int ackNum, byte[] data){
		this.seqNum = seqNum; this.ackNum = ackNum;
		this.data = data;
		// Initially, checksum is 0 -> no need to update
		this.checksum = calculateChecksum(this.packetToBytes());
	}
	
	/*
	Constructor
	@param bytes, the byte array for all the data the packet will contain
	@param length, the amount of data in the packet
	*/
	public RTPPacket(byte[] bytes, int length) {
		this.seqNum = bytesToInt(bytes, 0);
		this.ackNum = bytesToInt(bytes, 4);
		this.checksum = bytesToInt(bytes, 8);
		// Flags
		int flagInt = bytesToInt(bytes, 12);
		this.isAck = getFlag(flagInt, 0);
		this.isSyn = getFlag(flagInt, 1);
		this.isFin = getFlag(flagInt, 2);
		this.data = Arrays.copyOfRange(bytes, 16, length);
	}

	/*
	Convert from 4 bytes to an integer
	@param bytes, the byte array to be converted
	@param offset, how much the byte array should be offset
	@return int, the int value of the bytes
	*/
	public static int bytesToInt(byte[] bytes, int offset) {
		byte[] buffer = new byte[4];
		for (int i = 0; i < 4; i++)
			buffer[i] = bytes[offset + i];
		return ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN).getInt();
	}
	
	/*
	Get the flag value from an integer	
	@param flagInt, the flag integer
	@param index, the amount the flag will be right shifted
	@return boolean, whether the flagInt after right shifted is 12
	*/
	public static boolean getFlag(int flagInt, int index) {
		return ((flagInt >> index) & 1) == 1;
	}

	/*
	Updates the checksum using the packets' bytes
	*/
	public void updateChecksum() {
		this.checksum = 0;
		byte[] bytes = packetToBytes();
		this.checksum = calculateChecksum(this.packetToBytes());
	}
	
	/*
	Calculates the checksum from a byte array
	@param bytes, the byte array to be used for the checksum
	@return int, the checksum
	*/
	public static int calculateChecksum(byte[] bytes) {
		Checksum checksumObj = new CRC32();
		checksumObj.reset();
		checksumObj.update(bytes, 0, bytes.length);
		return (int) checksumObj.getValue();
	}

	/*
	Compares two packets' based upon their sequence numbers
	@param other, the other packet to compare to
	@return, the difference in the two sequence numbers
	*/
	public int compareTo(RTPPacket other) {
		return seqNum - other.seqNum;
	}

	/*
	Function to convert from packet to byte array, along with its helper function(s)
	@return byte[],the byte array version of the packet
	*/
		public byte[] getBytes() {
		updateChecksum();
		return packetToBytes();
	}
	
	/*
	Helper function for getBytes()
	It converts the packet into the byte array
	@return byte[], the byte array version of the packet
	*/
	private byte[] packetToBytes(){
		ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + data.length);
		// Number
		buffer.putInt(this.seqNum); 
		buffer.putInt(this.ackNum); 
		// Checksum
		buffer.putInt(this.checksum);
		// Flags
		buffer.putInt(flagsToInt());
		// data
		buffer.put(data);
		return buffer.array();
	}

	/*
	Convert from the value in the integer to the boolean value.
	@return int, the appropriate flag for Ack, Syn, or Fin
	*/
	/*private*/ int flagsToInt() {
		int flagInt = 0;
		if (isAck) flagInt |= (1 << 0);
		if (isSyn) flagInt |= (1 << 1);
		if (isFin) flagInt |= (1 << 2);				
		return flagInt;
	}


	/*
		All of these methods are getters and setters
	*/
	// sequence number getter, setter
	public void setSeqNum(int seqNum) {this.seqNum = seqNum;}
	public int getSeqNum() {return this.seqNum;}
	public void setAckNum(int ackNum) {this.ackNum = ackNum;}
	public int getAckNum() {return this.ackNum;}	
	// checksum getter, setter
	// public void setChecksum(int checksum) {this.checksum = checksum;}	
	public int getChecksum(){return checksum;}
	// flags getter, setter
	public void setFin(boolean isFin) {this.isFin = isFin;}
	public boolean isFin() {return this.isFin;}
	public void setSyn(boolean isSyn) {this.isSyn = isSyn;}
	public boolean isSyn() {return this.isSyn;}
	public void setAck(boolean isAck) {this.isAck = isAck;}
	public boolean isAck() {return this.isAck;}
	// data setter, getter
	// public void setData(byte[] data) {this.data = data;}
	public byte[] getData(){return data;}	
}