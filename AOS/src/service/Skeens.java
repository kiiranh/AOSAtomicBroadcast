/**
 * CS 6378.002 Advanced Operating Systems
 * Project 1
 * Fall 2013
 *
 * @author Kiran Gavali
 */
package service;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import model.Connection;
import model.Message;

public class Skeens extends Thread {
    /**
     * Class variables. One instance should exist
     */
    private static Connection connection;
    private static int myId;
    private static int nodeCount;
    private static AtomicInteger highestKnownTimestamp = new AtomicInteger(0); // TODO
									       // Handle
									       // concurrency
    private static PriorityQueue<Message> pendingQueue = new PriorityQueue<Message>(); // Message
										       // is
										       // Comparable

    private static enum Operation {
	ENQUEUE, UPDATE
    }

    /* Holds the Actual Message Payload to be broadcast */
    private static Queue<String> sendMessageQueue;

    /* Holds the messages which are delivered to the application */
    private static Queue<String> deliveredMessageQueue;

    /* To denote when a thread should stop */
    private static AtomicBoolean keepWorking = new AtomicBoolean(true);

    public Skeens(Connection connection, int myId, int nodeCount,
	    Queue<String> sendMessageQueue, Queue<String> deliveredMessageQueue) {
	Skeens.connection = connection;
	Skeens.myId = myId;
	Skeens.nodeCount = nodeCount;
	Skeens.sendMessageQueue = sendMessageQueue;
	Skeens.deliveredMessageQueue = deliveredMessageQueue;
    }

    private synchronized Message processPendingQueue(Operation op,
	    Message newMsg) {
	Message msg = null;
	Iterator<Message> it = null;

	switch (op) {
	case ENQUEUE:
	    // This is a new broadcast message.
	    // Increment the timestamp for this receive event (IF NOT
	    // FROM ME)
	    if (newMsg.getMsgId().getOriginatorId() != Skeens.myId) {
		newMsg.setTimeStamp(highestKnownTimestamp.incrementAndGet());
	    }

	    // 2. Update the message to set its type = ACK
	    newMsg.setType(Message.Type.ACK);

	    // 3. Update the message status to Pending. Add the
	    // message to the Priority Queue
	    newMsg.setStatus(Message.State.PENDING);
	    pendingQueue.add(newMsg);
	    break;

	case UPDATE:
	    // Update a particular message with the newMsg
	    it = pendingQueue.iterator();
	    while (it.hasNext()) {
		msg = it.next();
		if (msg.equals(newMsg)) {
		    // Remove this msg from the queue.
		    break;
		}
	    }
	    it = null;

	    // Check if we found the msg
	    if (msg != null) {
		pendingQueue.remove(msg);

		// Update the msg with newMsg
		if (newMsg.getTimeStamp() < msg.getTimeStamp()) {
		    newMsg.setTimeStamp(msg.getTimeStamp());
		}

		// Check if the new MSG is ACK or FIN
		if (newMsg.getType().equals(Message.Type.ACK)) {
		    // ACK Msg.: Increment reply count
		    newMsg.setReplyCount(msg.getReplyCount() + 1);

		    if (newMsg.getReplyCount() == (nodeCount - 1)) {
			// Received all ACKs. Change state to READY and type to
			// FINAL
			newMsg.setType(Message.Type.FINAL);
			newMsg.setStatus(Message.State.READY);
		    }

		} else if (newMsg.getType().equals(Message.Type.FINAL)) {
		    // FIN msg: State already changed to ready
		    // 1. Set the status to READY for delivery
		    // Type=FIN and TS=final TS already in newMsg
		    newMsg.setStatus(Message.State.READY);
		}

	    } else {
		System.out.println("ERROR: Message \"" + newMsg.getMsgId()
			+ "\" not found!");
	    }

	    // IMP: Add the Updated Message to the priority queue
	    pendingQueue.add(newMsg);

	    // Check the HEAD of the queue. While the head node state is READY,
	    // deliver it
	    Message msgToDeliver = null;
	    while ((msgToDeliver = pendingQueue.peek()) != null
		    && msgToDeliver.getStatus().equals(Message.State.READY)) {
		// Head of queue ready. Deliver it
		msgToDeliver = pendingQueue.poll();
		deliveredMessageQueue.add(msgToDeliver.getPayload());
		String disp = "Delivered: " + msgToDeliver.toString();
		System.out.println(disp);
	    }
	    break;

	default:
	    break;
	}

	// Return the updated message
	return newMsg;
    }

    class SendThread implements Runnable {
	@Override
	public void run() {
	    System.out.println("Starting Send Service...");
	    while (keepWorking.get()) {
		// Read sendQueue and send messages if any
		while (sendMessageQueue.peek() != null) {
		    String payload = sendMessageQueue.poll();
		    try {
			// Check if this is Result/Done/APP message
			if (payload.startsWith("=")) {
			    // This is result message. Send only to leader
			    Message msg = new Message(
				    highestKnownTimestamp.incrementAndGet(),
				    myId, payload);
			    msg.setType(Message.Type.RESULT);
			    connection.sendResultToLeader(msg);
			} else if (payload.equals("DONE")) {
			    // Processing done. Set variable to stop service
			    keepWorking.set(false);
			} else {
			    // CREATE MESSAGE for payload
			    Message msg = new Message(
				    highestKnownTimestamp.incrementAndGet(),
				    myId, payload);
			    connection.broadcast(msg);
			    // Add to my queue
			    processPendingQueue(Operation.ENQUEUE, msg);
			    System.out.println("Proposed: " + msg.toString());
			}
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		}

		// Sleep for a moment
		try {
		    Thread.sleep(100);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
	    }
	    System.out.println("Stopping Send Service...");
	}
    }

    class ReceiveThread implements Runnable {
	@Override
	public void run() {
	    System.out.println("Starting Receiving Messages...");
	    while (keepWorking.get()) {
		try {
		    LinkedList<Message> receivedMessages = connection.receive();
		    // Process the messages and Perform SEND/ACK/DELIVER
		    // ACCORDINGLY

		    for (Iterator<Message> iterator = receivedMessages
			    .iterator(); iterator.hasNext();) {
			Message message = iterator.next();

			// Check type of message
			switch (message.getType()) {
			case NEW:
			    Message updatedMsg = processPendingQueue(
				    Operation.ENQUEUE, message);

			    // Send the ACK/REPLY message to the sending node.
			    int senderId = message.getMsgId().getOriginatorId();
			    connection.unicast(senderId, updatedMsg);
			    break;

			case ACK:
			    // This is a ACK/Reply message.
			    // 1. Update the Stored message with the updated
			    // timing info: Update if current is greater
			    Message updatedMessage = processPendingQueue(
				    Operation.UPDATE, message);

			    // 2. Check if we have received ACK/REPLIES for this
			    // message from all the messages. If YEs msg would
			    // have been marked
			    // READY, FIN and timestamp updated to the latest
			    if (updatedMessage.getStatus().equals(
				    Message.State.READY)) {
				connection.broadcast(updatedMessage);
			    }

			    break;

			case FINAL:
			    // This is the final message.
			    processPendingQueue(Operation.UPDATE, message);
			    break;

			case RESULT:
			    // This is result from a node. Add to queue
			    deliveredMessageQueue.add(message.getPayload());

			default:
			    break;
			}

		    }

		} catch (Exception e) {
		    e.printStackTrace();
		}

		// Sleep for a moment
		try {
		    Thread.sleep(100);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
	    }
	    System.out.println("Stopping Receiving Messages...");
	}
    }

    @Override
    public void run() {
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
