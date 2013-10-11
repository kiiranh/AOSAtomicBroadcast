/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sample_sctp_client;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 *
 * @author tientran
 */
public class Sample_SCTP_Client {

    /**
     * @param args the command line arguments
     */
    private static String byteToString(ByteBuffer byteBuffer)
    {
		byteBuffer.position(0);
		byteBuffer.limit(512);
		byte[] bufArr = new byte[byteBuffer.remaining()];
		byteBuffer.get(bufArr);
		return new String(bufArr);
    }
    public static void main(String[] args) throws IOException {
       ByteBuffer byteBuffer;
       byteBuffer = ByteBuffer.allocate(512);

       // Create ClientSock and connect to sever
       SctpChannel ClientSock;
       InetSocketAddress serverAddr = new InetSocketAddress("localhost",55522); // You should replace "localhost" when run on multi machine
       ClientSock = SctpChannel.open();
       ClientSock.connect(serverAddr, 0, 0);
       
       System.out.println("Create Connection Successfully");
       MessageInfo messageInfo  = ClientSock.receive(byteBuffer,null,null);
       String message = byteToString(byteBuffer);
       System.out.println("Receive Message from Server:");
       System.out.println(message);

    }
}
