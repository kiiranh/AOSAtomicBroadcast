/**
 * CS 6378.002 Advanced Operating Systems
 * Fall 2013
 * Project 1 - Atomic Broadcast
 *
 * @author Kiran Gavali
 */

package model;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.LinkedList;

import com.sun.nio.sctp.MessageInfo;
import com.sun.nio.sctp.SctpChannel;
import com.sun.nio.sctp.SctpServerChannel;

@SuppressWarnings("restriction")
public class Connection {
    public enum Stream {
	CONTROL(0), DATA(1);

	private final int value;

	private Stream(int val) {
	    value = val;
	}
    }

    public enum Action {
	START, STOP;
    }

    private ArrayList<SctpChannel> channelList;
    private final ByteBuffer buf = ByteBuffer.allocateDirect(70);
    private final CharBuffer cbuf = CharBuffer.allocate(70);;
    private final Charset charset = Charset.forName("ISO-8859-1");
    private CharsetEncoder encoder;
    private CharsetDecoder decoder;

    public Connection() {
	channelList = new ArrayList<SctpChannel>();
	encoder = charset.newEncoder();
	decoder = charset.newDecoder();
    }

    public void setUp(ArrayList<NodeInfo> nodes, int myId) throws IOException {
	// Selectively create connections to the given nodes.
	int i = 0;
	System.out.println("MyID: " + myId);

	// Connect to nodes with lower IDs
	// FIXME Assume configuration file has nodes in ascending order of IDs
	while (nodes.get(i).getNodeId() != myId) {
	    // Connect to this node
	    System.out.println("Trying to connect to Node Id: " + i);
	    InetSocketAddress serverAddr = new InetSocketAddress(nodes.get(i)
		    .getHostname(), nodes.get(i).getPort());
	    SctpChannel sc = SctpChannel.open(serverAddr, 0, 0);
	    channelList.add(sc);
	    System.out.println("Connected to Node Id: " + i);
	    System.out.println("\tLocal Channel: "
		    + sc.getAllLocalAddresses().iterator().next()
		    + " Remote Channel: "
		    + sc.getRemoteAddresses().iterator().next());
	    ++i;
	}

	// i points to myId
	int myPort = nodes.get(i).getPort();
	System.out.println("My Port = " + myPort);
	++i;

	SctpServerChannel ssc = SctpServerChannel.open();
	InetSocketAddress serverAddr = new InetSocketAddress(myPort);
	ssc.bind(serverAddr);
	// We have already gone through nodes with lower IDs. Only nodes with
	// higher IDs remain.
	// Accept connections from nodes with higher IDs.
	while (nodes.size() != i) {
	    System.out.println("Awaiting connection from Node Id: " + i);
	    SctpChannel sc = ssc.accept();
	    channelList.add(sc);
	    System.out.println("Accepted connection from Node Id: " + i);
	    System.out.println("\tLocal Channel: "
		    + sc.getAllLocalAddresses().iterator().next()
		    + " Remote Channel: "
		    + sc.getRemoteAddresses().iterator().next());
	    ++i;
	}
    }

    /**
     * Called by leader to start off the program/computation
     * 
     * @throws IOException
     */
    public void leaderNotifyStart(int leaderId) throws IOException {
	MessageInfo messageInfo = null;

	for (SctpChannel channel : channelList) {
	    cbuf.clear();
	    buf.clear();
	    cbuf.put(leaderId + "," + Action.START).flip();
	    encoder.encode(cbuf, buf, true);
	    buf.flip();

	    /* send the message on the US stream */
	    messageInfo = MessageInfo
		    .createOutgoing(null, Stream.CONTROL.value);
	    channel.send(buf, messageInfo);
	    System.out.println("Signalled START to Channel: "
		    + channel.getRemoteAddresses().iterator().next());
	}

	cbuf.clear();
	buf.clear();
    }

    /**
     * Called by non-leaders to wait for START signal from Leader
     * 
     * @throws IOException
     */
    public void awaitStartFromLeader() throws IOException {
	MessageInfo messageInfo = null;
	buf.clear();
	System.out.println("Awaiting START signal from Leader");

	while (true) {
	    // TODO Assuming the first node in the config file to be the leader
	    messageInfo = channelList.get(0).receive(buf, System.out, null);
	    buf.flip();
	    if (buf.remaining() > 0
		    && messageInfo.streamNumber() == Stream.CONTROL.value) {
		String msg = decoder.decode(buf).toString();
		if (msg.contains("" + Action.START)) {
		    System.out.println("Received Signal START from Leader: "
			    + msg);
		    break;
		}
	    }
	    buf.clear();
	    try {
		Thread.sleep(5);
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}
    }

    synchronized public void broadcast(Message msg) throws IOException {
	// 1. Convert Message to String
	// 2. Broadcast over All Channels.
	for (SctpChannel channel : channelList) {
	    cbuf.clear();
	    buf.clear();
	    cbuf.put(msg.toString()).flip();
	    encoder.encode(cbuf, buf, true);
	    buf.flip();

	    /* send the message on the US stream */
	    MessageInfo messageInfo = MessageInfo.createOutgoing(null,
		    Stream.DATA.value);
	    channel.send(buf, messageInfo);
	}

	System.out.println("Broadcast Message: " + msg.toString());
	cbuf.clear();
	buf.clear();

    }

    synchronized public LinkedList<Message> receive() throws IOException {
	LinkedList<Message> receivedMessages = new LinkedList<Message>();

	// TODO
	// 1. Read messages from channel
	MessageInfo messageInfo = null;
	buf.clear();
	System.out.println("Checking message on Channel...");

	for (SctpChannel channel : channelList) {
	    // Get all available messages from each channel
	    messageInfo = channel.receive(buf, System.out, null);
	    buf.flip();
	    if (buf.remaining() > 0
		    && messageInfo.streamNumber() == Stream.DATA.value) {
		String msgStr = decoder.decode(buf).toString();

		// 2. Parse message and add to queue
		Message msg = Message.parseMessage(msgStr);
		receivedMessages.add(msg);
	    }
	    buf.clear();
	    try {
		Thread.sleep(5);
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}

	return receivedMessages;
    }

    public void tearDown() throws Exception {
	for (SctpChannel channel : channelList) {
	    channel.close();
	}
    }
}
