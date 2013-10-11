/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sample_sctp_client;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author tientran
 */
public class Sample_SCTP_Sever {

    /**
     * @param args the command line arguments
     */
    private static  void sendMessage(SctpChannel clientSock, String Message) throws CharacterCodingException
    {
        // prepare byte buffer to send massage
        ByteBuffer sendBuffer = ByteBuffer.allocate(512);
        sendBuffer.clear();
        //Reset a pointer to point to the start of buffer 
        sendBuffer.put(Message.getBytes());
        sendBuffer.flip();

        
        try {
            //Send a message in the channel 
            MessageInfo messageInfo = MessageInfo.createOutgoing(null,0);
            clientSock.send(sendBuffer, messageInfo);
        } catch (IOException ex) {
            Logger.getLogger(Sample_SCTP_Sever.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    public static void main(String[] args) throws IOException {
        // Create SCTP Server Object
        SctpServerChannel serverSock;
        
        
        // Binding Object into Server port

        serverSock = SctpServerChannel.open();
        InetSocketAddress serverAddr = new InetSocketAddress(55522);
        serverSock.bind(serverAddr);
        
       
        System.out.println("Bound port: 55522");
        System.out.println("Waiting for connection ...");
        
        // Waiting for Connection from client
        boolean Listening = true;
        // Create buffer to send or receive message

        while(Listening)
        {
            // Receive a connection from client and accept it
            SctpChannel clientSock = serverSock.accept();
            System.out.println("Received Connection");
            // Now server will communication to this Client via clientSock
            
            String Message = "WELCOME MESSAGE FROM SERVER";
            sendMessage(clientSock,Message);

        }
    }
}
