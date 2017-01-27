package project;

/**
 * Created by Venkatesh on 12/1/2016.
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;
import java.util.Scanner;
import java.io.InterruptedIOException;
public class Client {
	public static void main(String[] args) throws Exception{
		final int server_port = 9916;   //Server port
		while(true){
			Random rand =new Random();      //Generating random number for ID
			long random_id = rand.nextInt(65536);
			String request = "<id>"+random_id+"</id>";
			
			//Getting measurement id from user
			System.out.println("Enter the Measurement value: ");
			Scanner data=new Scanner(System.in);
			String M_value=data.nextLine();
			
			//Assembling the request
			request = request+"<measurement>"+M_value+"</measurement>";
			request = "<request>"+request+"</request>";
			
			System.out.println("Calculating the checksum for request...");
			long check_sum = checksum(request); //Calculating the checksum for the request
			request = request+Long.toString(check_sum);        //appending checksum to message
			System.out.println("\nRequest: "+request+"\n");
			byte[] sent_request = request.getBytes();          //converting String to bytes
			InetAddress server = InetAddress.getLocalHost();
			DatagramPacket sentPacket = new DatagramPacket(sent_request,sent_request.length,server, server_port);
			DatagramSocket clientSocket = new DatagramSocket();
			Decode(sentPacket,clientSocket,data);    
		}
		
	}
	public static void Decode(DatagramPacket sentPacket,DatagramSocket clientSocket,Scanner data) throws Exception{
		//Decoding the response 
		String response =communication(sentPacket,clientSocket);      //communication part
		String[] res_parts=response.split("<code>");
		String code=res_parts[1];
		if(code.charAt(0)=='0'){
			String[] value_parts=response.split("<value>");
			String value =value_parts[1].split("</value>")[0];              // extracting the measurement value from the response
			System.out.print("Value for requested measurement is: "+value+"\n\n");
		}
		else if(code.charAt(0)=='1'){
			System.out.print("Error: integrity check failure. The request has one or more bit errors.\n");
			System.out.print("\n Do you want to send the request again (y/n)?");        //response of user when integrity check fails
			String user_response= data.nextLine();
			if(user_response.equals("y")){
				Decode(sentPacket,clientSocket,data);                     
			}
		}
		else if(code.charAt(0)=='2'){
			System.out.print("Error: malformed request. The syntax of the request message is not correct.\n");   //Syntax
		}
		else{
			System.out.print("Error: non-existent measurement. The measurement with the requested measurement ID does not exist.\n\n");
		}
	}
	public static String communication(DatagramPacket sentPacket,DatagramSocket clientSocket) throws Exception
	{
		final int Timeout_val = 1000;  //Timeout value to 1 second
		final int max_size =8192;      //some OS will not support larger message 
		byte[] received_message = new byte[max_size];
		DatagramPacket receivedPacket = new DatagramPacket(received_message,received_message.length);
        boolean boool=true;
        int i=0;
        while(boool)
        {
        	System.out.println("Sending the request...");
        	clientSocket.send(sentPacket);    //Sending the Request to server
			System.out.println("waiting for the response...");
			clientSocket.setSoTimeout(Timeout_val*2^i);
			try
			{
				boool=false;
				clientSocket.receive(receivedPacket);          //Receiving the Response
				System.out.println("Receiving the response...");
			}
			catch(InterruptedIOException e)
			{
				boool=true;
				System.out.print("Timeout occured "+(i+1)+" Time\n");  
				i++;
				if(i==4)
				{
					System.out.print("\nError:Communication failure!!");   //if time out occurs 4 times
					System.exit(0);
				}
			}
        }
		String s = new String(received_message).trim();   //converting response to String
		s.replaceAll("\\s","");                           //Removing all white spaces
		
		String[] parts=s.split("</response>");
		String response_check_sum = parts[1];
		String response=s.substring(0, s.length()-response_check_sum.length());
		
		System.out.println("\nResponse: "+s+"\n");
		System.out.println("Calculating the checksum for response....");
		long check_sum_cal =checksum(response);              //Calculating the Checksum for response
		String checksumcal=Long.toString(check_sum_cal);
		
		if(!checksumcal.contains(response_check_sum))        //comparing the checksum
		{
			s = communication(sentPacket,clientSocket);       //Resends the packet if checksum fails
			
		}
		return s;                                        //return response
	}
	public static long checksum(String sequence){
		//Checksum Calculation
		char[] seqarray = sequence.toCharArray();          //converting String to char sequence
		int a=(seqarray.length%2==0 ? seqarray.length/2:seqarray.length/2+1);
		int[] words = new int[a];
		int C=7919;
		int D=65536;
		long s=0;
		for(int x=0;x<seqarray.length/2;x++){
			//Each word with one character as MSB and other as LSB
			int msb =(int)seqarray[2*x];
			int lsb = (int)seqarray[(2*x)+1];
			words[x] =(msb << 8)+lsb;          //Shifting to add LSB to MSB
		}
		if(seqarray.length%2!=0){
			words[a-1]= (int)seqarray[seqarray.length-1] << 8 + 0;         //For odd number of characters in sequence
		}
		for(int y=0;y<words.length;y++){
			long index = s^words[y];     //XOR function
			s= (C*index)%D;              //MOD function to get Checksum
		}
		return s;                        //Checksum
		
	}

}
