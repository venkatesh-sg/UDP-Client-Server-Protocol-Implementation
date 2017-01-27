package project;

/**
 * Created by Venkatesh on 12/1/2016.
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class Server {
	public static void main(String[] args) throws Exception
	{
		final int server_port =9916;  //Port number for the server
		DatagramSocket server_socket = new DatagramSocket(server_port);  //server socket in specified port
		while(true){                                                    //server to run non-stop
			final int max_size = 8192;                                 //some OS will not support larger packet
			byte[] receivedMessage = new byte[max_size];               //Received message buffer
			
			//Packet for received message
			DatagramPacket receive_Packet = new DatagramPacket(receivedMessage,receivedMessage.length);
			InetAddress clientAddress;                                //IP address of the client
			System.out.println("........Server is ready to receive the request from client........");
			server_socket.receive(receive_Packet);                    //Server receives packet from the client
			
			//Getting IP address and port number from the received packet
			clientAddress = receive_Packet.getAddress();
			int clientPort = receive_Packet.getPort();
			
			String s=new String(receivedMessage).trim();              //Changing the Byte stream in to String object
			String response = "<response>";
			s.replaceAll("\\s","");                                   //All the white spaces will be removed
			String s1[] = s.split("</request>");
			String request_check_sum = s1[1];                         //Extracting Checksum from full message string
			String request=s.substring(0, s.length()-request_check_sum.length());   //Extracting only request excluding checksum
			
			System.out.println("\nRequest: "+s+"\n");
			System.out.println("Calculating the Checksum for request");
			long check_sum_cal =checksum(request);                    //Calculating the checksum
			
			//Getting ID information from the request
			String s2 =s.split("<id>")[1];
			String id =s2.split("</id>")[0];
			response= response+"<id>"+id+"</id><code>";
			
			String seq[]={"<request>","<id>","</id>","<measurement>","</measurement>","</request>"};  //String array in order to check the syntax order in the request
			String checksumcal=Long.toString(check_sum_cal);             //Converting Checksum to String object to compare
			
			System.out.println("Comparing the Checksum");
			if(!checksumcal.contains(request_check_sum)){                //Comparing checksum 
				response=response+"1</code></response>";
			}
			else{                              //Checksum's are equal
				//Checking the Syntax of the request
				boolean Syntax = true;
				boolean check;
				String ck=s;
				for(int i=0;i<seq.length;i++){
					check=ck.contains(seq[i]);
					if(check==false){
						Syntax=false;
						break;
					}
					else{
						ck=ck.split(seq[i])[1];
					}
					
				}
				if(Syntax==false){      
					response=response+"2</code></response>";   //code 2 if syntax fails
				}
				else{
					String ms=s.split("<measurement>")[1];
					String val=null;
					ms=ms.split("</measurement>")[0];   //Extracting the measurement id from request
					try (BufferedReader br = new BufferedReader(new FileReader("data.txt"))) {
					    String line;
					    System.out.println("Reading the data from the file");
					    while ((line = br.readLine()) != null) {   //Reading the data file line by line
					    	String[] linep=line.split("\t");
					       if(linep[0].equals(ms)){
					    	   val=linep[1];   //Extracting the value for measurement id from data file
					       }
					    }
					}
					if(val==null){
						response=response+"3</code></response>";   //Code 3 if there is no requested measurement id
					}
					else{
						//Assembling the response with value
						response=response+"0</code><measurement>"+ms+"</measurement><value>"+val+"</value></response>";
					}
				}
			}
			
			System.out.println("Calculating checksum for response");
			long Checksum=checksum(response);  //Calculating the checksum
			response=response+Long.toString(Checksum);
			System.out.println("\nResponse: "+response+"\n");
			
			byte[] sentMessage = response.getBytes();   //Changing String object to Bytes to send
			//Setting the Sending packet
			DatagramPacket sent_Packet = new DatagramPacket(sentMessage, sentMessage.length);
			sent_Packet.setAddress(clientAddress); 
			sent_Packet.setPort(clientPort);
			
			System.out.println("Sending the response to client.....\n");
			server_socket.send(sent_Packet);  //Sending the response to client
		}
		
	}
	public static long checksum(String sequence)
	{
		char[] seqarray = sequence.toCharArray();   //String to char Sequence
		int a=(seqarray.length%2==0 ? seqarray.length/2:seqarray.length/2+1);
		int[] words = new int[a];
		int C=7919;
		int D=65536;
		long s=0;
		for(int x=0;x<seqarray.length/2;x++){
			//Each word with one character as MSB and other as LSB
			int msb =(int)seqarray[2*x];
			int lsb = (int)seqarray[(2*x)+1];
			words[x] =(msb << 8)+lsb;      //Shifting to add LSB to MSB 
		}
		if(seqarray.length%2!=0){
			words[a-1]= (int)seqarray[seqarray.length-1] << 8 + 0; //For odd number of characters in sequence
		}
		for(int y=0;y<words.length;y++){
			long index = s^words[y];           //XOR function
			s= (C*index)%D;                     //MOD function to get Checksum
		}
		return s;                            //Checksum return
	}

}
