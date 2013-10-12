/**
 * CS 6378.002 Advanced Operating Systems
 * Project 1
 * Fall 2013
 *
 * @author Kiran Gavali
 */
package service;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;

import model.Connection;
import model.Message;

public class Skeens extends Thread {
    /**
     * Class variables. One instance should exist
     */
    private static Connection connection;
    private static int myId;
    private static int highestKnownTimestamp = 0; // TODO Handle concurrency
    private static PriorityQueue<Message> pendingQueue = new PriorityQueue<>(0); // Message
										 // is
										 // Comparable
    /* Holds the Actual Message Payload to be broadcast */
    private static Queue<String> sendMessageQueue;

    /* Holds the messages which are delivered to the application */
    private static Queue<Message> deliveredMessageQueue;

    /* To denote when a thread should stop */
    private static boolean keepWorking = true;

    public Skeens(Connection connection, int myId,
	    Queue<String> sendMessageQueue, Queue<Message> deliveredMessageQueue) {
	Skeens.connection = connection;
	Skeens.myId = myId;
	Skeens.sendMessageQueue = sendMessageQueue;
	Skeens.deliveredMessageQueue = deliveredMessageQueue;
    }

    class SendThread implements Runnable {
	@Override
	public void run() {
	    // TODO
	    System.out.println("Starting Send Service...");
	    while (keepWorking) {
		// Read sendQueue and send messages if any
		while (sendMessageQueue.peek() != null) {
		    String payload = sendMessageQueue.poll();
		    try {
			// FIXME Handle timestamp & concurrency
			// CREATE MESSAGE for payload
			Message msg = new Message(++highestKnownTimestamp,
				myId, payload);
			connection.broadcast(msg);
			System.out.println("");
		    } catch (IOException e) {
			e.printStackTrace();
		    }
		}
	    }
	    System.out.println("Stopping Send Service...");
	}
    }

    class ReceiveThread implements Runnable {
	@Override
	public void run() {
	    // TODO
	    System.out.println("Starting Receiving Messages...");
	    while (keepWorking) {
		try {
		    LinkedList<Message> receivedMessages = connection.receive();
		    // TODO Process the messages and Perform SEND/ACK/DELIVER
		    // ACCORDINGLY
		    for (Iterator<Message> iterator = receivedMessages
			    .iterator(); iterator.hasNext();) {
			Message message = iterator.next();
		    }

		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	    System.out.println("Stopping Receiving Messages...");
	}
    }

    @Override
    public void run() {
	// TODO Auto-generated method stub
	// Start Send and Receive Thread
	Thread sendThread = new Thread(new SendThread());
	Thread receiveThread = new Thread(new ReceiveThread());

	System.out.println("SERVICE: Starting Send Thread...");
	sendThread.start();
	System.out.println("SERVICE: Starting Receive Thread...");
	receiveThread.start();

	// Wait for them to end
	try {
	    sendThread.join();
	    receiveThread.join();
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}

	System.out.println("Stopping Skeen's service");
    }
}
