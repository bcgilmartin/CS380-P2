import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Scanner;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.zip.CRC32;
import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.lang.String;
import java.lang.Byte;
import java.math.BigInteger;

public final class PhysLayerClient {

    public static void main(String[] args) throws Exception {
		
		//Making 4B/5B conversion hashtable
		Hashtable<String, String> Bit4B5B = new Hashtable<String, String>(16);
		Bit4B5B.put("11110", "0000");
		Bit4B5B.put("01001", "0001");
		Bit4B5B.put("10100", "0010");
		Bit4B5B.put("10101", "0011");
		Bit4B5B.put("01010", "0100");
		Bit4B5B.put("01011", "0101");
		Bit4B5B.put("01110", "0110");
		Bit4B5B.put("01111", "0111");
		Bit4B5B.put("10010", "1000");
		Bit4B5B.put("10011", "1001");
		Bit4B5B.put("10110", "1010");
		Bit4B5B.put("10111", "1011");
		Bit4B5B.put("11010", "1100");
		Bit4B5B.put("11011", "1101");
		Bit4B5B.put("11100", "1110");
		Bit4B5B.put("11101", "1111");
		
		
		//connecting to socket and setup
        try (Socket socket = new Socket("codebank.xyz", 38002)) {
			System.out.println("\nConnected to server.");
			OutputStream os = socket.getOutputStream();
			InputStream is = socket.getInputStream();
			
			
			
			//receive preamble and get baseline
			int preambleInt = 0;
			double baseline = 0;
			for(int i = 0; i < 64; i++) {
				preambleInt += is.read();
			}
			baseline = (double)preambleInt / 64.00;
			System.out.println("Baseline established from preamble: " + baseline);
			
			
			
			//make int[] of the 320 signals
			int messageInt[] = new int[320];
			int testInt = 0;
			for(int i = 0; i < 320; i++) {
				testInt = is.read();
				if((double)testInt > baseline) {
					messageInt[i] = 1;
				}
				else {
					messageInt[i] = 0;
				}
			}
		
			
			
			//reverse NRZI conversion
			int tracker = 0;
			String returnMessageString5B = "";
			for(int i = 0; i < 320; i++) {
				if(tracker == 0 && messageInt[i] == 0) {
					returnMessageString5B += "0";
				}
				else if(tracker == 0 && messageInt[i] == 1) {
					returnMessageString5B += "1";
					tracker = 1;
				}
				else if(tracker == 1 && messageInt[i] == 0) {
					returnMessageString5B += "1";
					tracker = 0;
				}
				else {
					returnMessageString5B += "0";
				}
			}
			
			
			
			//convert 5B string to 4B string
			String returnMessageString4B = "";
			for(int i = 0; i < 64; i++) {
				returnMessageString4B += Bit4B5B.get(returnMessageString5B.substring(5*i, i*5+5));
			}
			
			
			
			//convert 4B string to Bytes
			byte returnSignal[] = new byte[32];
			BigInteger IntBMessage = new BigInteger(returnMessageString4B, 2);
			returnSignal = IntBMessage.toByteArray();
			
			
			
			//display hex 4B message
			System.out.println("Received 32 Bytes: " + IntBMessage.toString(16));


			
			//send decoded message
			os.write(returnSignal);
			
			
			
			//receive verification signal
			if(is.read() == 1) {
				System.out.println("Good response.");
			}
			else{
				System.out.println("Bad response.");
			}
        }
		System.out.println("Disconnected from server.");
    }
}