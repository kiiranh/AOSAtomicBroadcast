/**
 * CS 6378.002 Advanced Operating Systems
 * Project 1
 * Fall 2013
 *
 * @author Kiran Gavali
 */
package service;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import model.Connection;
import model.Message;

public class Skeens extends Thread {
    /**
     * Class variables. One instance should exist
     */
    private static Connection connection;
    private static int myId;
    private static int nodeCount;
    private static int logicalClock = 0;
    private static PriorityQueue<Message> pendingQueue = new PriorityQueue<Message>(); // Message
										       // is
										       // Comparable
    private static String logFile;

    private static enum Operation {
	ENQUEUE, UPDATE
    }

    private final Lock resourcesLock = new ReentrantLock(true);
    private final Lock applicationQueueLock = new ReentrantLock(true);
    // private final Condition notEmpty = applicationQueueLock.newCondition();

    /* Holds the Actual Message Payload to be broadcast */
    private static Queue<String> sendMessageQueue;

    /* Holds the messages which are delivered to the application */
    private static Queue<String> deliveredMessageQueue;

    /* To denote when a thread should stop */
    private static AtomicBoolean keepWorking = new AtomicBoolean(true);

    public Skeens(Connection connection, int myId, int nodeCount,
	    String deliveryLog) {
	Skeens.connection = connection;
	Skeens.myId = myId;
	Skeens.nodeCount = nodeCount;
	Skeens.logFile = deliveryLog;
	Skeens.sendMessageQueue = new LinkedList<String>();
	Skeens.deliveredMessageQueue = new LinkedList<String>();
    }

    public void writeLog(String s) {
	PrintWriter pw = null;
	try {
	    pw = new PrintWriter(new FileWriter(Skeens.logFile, true));
	    pw.append(s + "\n");
	} catch (Exception e) {
	    throw new RuntimeException(e);
	} finally {
	    if (pw != null) {
		try {
		    pw.close();
		} catch (Exception e) {
		    // Ignore issues during closing
		}
	    }
	}

    }

    // private void printPendingQueue() {
    // Iterator<Message> it = pendingQueue.iterator();
    // // writeLog("[CHECK] Pending Queue");
    // System.out.println("[CHECK] Pending Queue");
    // while (it.hasNext()) {
    // // writeLog(it.next().toString());
    // Message msg = it.next();
    // System.out.print(msg.toString());
    // System.out.println("\tReply Count: " + msg.getReplyCount());
    // }
    // }

    private Message processPendingQueue(Operation op, Message newMsg) {

	// Lamport's logical Clock. Set clock value to MAX (My Clock,
	// NewMsg.Clock) + 1
	logicalClock = Math.max(logicalClock, newMsg.getTimeStamp()) + 1;

	// System.out.println("[CHECK] processPendingQueue START");
	// System.out.println("[CHECK] CLOCK= " + logicalClock);
	// System.out.println("[CHECK] New Message=" + newMsg.toString() +
	// " Reply COunt= " + newMsg.getReplyCount());
	// printPendingQueue();

	switch (op) {
	case ENQUEUE:
	    // This is a new broadcast message.

	    // Update the timeStamp to MAX(my LogicalClock, newMsg.TS)
	    newMsg.setTimeStamp(Math.max(logicalClock, newMsg.getTimeStamp()));

	    // Update the message status to Pending. Add the
	    // message to the Priority Queue
	    newMsg.setStatus(Message.State.PENDING);
	    pendingQueue.add(newMsg);
	    break;

	case UPDATE:
	    Message msg = null;
	    Iterator<Message> it = null;
	    // Update a particular message with the newMsg
	    it = pendingQueue.iterator();
	    boolean found = false;

	    while (it.hasNext()) {
		msg = it.next();
		if (msg.equals(newMsg)) {
		    // Remove this msg from the queue.
		    found = true;
		    break;
		}
	    }
	    it = null;

	    // Check if we found the msg
	    if (found) {
		// Remove the old msg from the queue
		pendingQueue.remove(msg);
		// System.out.println("[CHECK] Removed Old Msg: " +
		// msg.toString() + " Reply COunt= " + msg.getReplyCount());

		// Update the msg with newMsg

		// Check if the new MSG is ACK or FIN
		if (newMsg.getType().equals(Message.Type.ACK)) {
		    // If the new ACK has greater TS Then update the Msg TS to
		    // that
		    newMsg.setTimeStamp(Math.max(msg.getTimeStamp(),
			    newMsg.getTimeStamp()));

		    // ACK Msg.: Increment reply count
		    newMsg.setReplyCount(msg.getReplyCount() + 1);

		    if (newMsg.getReplyCount() == (nodeCount - 1)) {
			// Received all ACKs. Change state to READY and type to
			// FINAL
			// System.out.println("[READY] GOT ALL ACKS: ReplyCount= "
			// + newMsg.getReplyCount() + " nodeCount= "
			// + nodeCount);
			newMsg.setType(Message.Type.FINAL);
			newMsg.setStatus(Message.State.READY);
		    }

		} else if (newMsg.getType().equals(Message.Type.FINAL)) {
		    // FIN msg: State already changed to ready
		    // 1. Set the status to READY for delivery
		    // Type=FIN and TS=final TS already in newMsg
		    newMsg.setStatus(Message.State.READY);
		}

		// IMP: Add the Updated Message to the priority queue
		pendingQueue.add(newMsg);
	    }

	    // Check the HEAD of the queue. While the head node state is READY,
	    // deliver it
	    Message msgToDeliver = null;
	    while ((msgToDeliver = pendingQueue.peek()) != null
		    && msgToDeliver.getStatus().equals(Message.State.READY)) {
		// Head of queue ready. Deliver it
		msgToDeliver = pendingQueue.poll();
		deliveredMessageQueue.add(msgToDeliver.getPayload());
		// String disp = "[SERVICE] DELIVERED MESSAGE: " +
		// msgToDeliver.toString();
		// System.out.println(disp);
		writeLog(msgToDeliver.toString());
	    }
	    break;

	default:
	    break;
	}

	// printPendingQueue();
	// System.out.println("[CHECK] CLOCK= " + logicalClock);
	// System.out.println("[CHECK] New Message=" + newMsg.toString()
	// + " Reply COunt= " + newMsg.getReplyCount());
	// System.out.println("[CHECK] processPendingQueue END");
	// Return the updated message
	return newMsg;
    }

    class SendThread implements Runnable {
	@Override
	public void run() {
	    // System.out.println("\n[SERVICE] Starting Send Service...");
	    while (keepWorking.get()) {

		try {
		    resourcesLock.lock();
		    applicationQueueLock.lock();

		    // Read sendQueue and send messages if any
		    while (sendMessageQueue.peek() != null) {
			String payload = sendMessageQueue.poll();
			try {
			    // Check if this is Result/Done/APP message
			    if (payload.startsWith("=")) {
				// This is result message. Send only to leader
				Message msg = new Message(++logicalClock, myId,
					payload);
				msg.setType(Message.Type.RESULT);
				connection.sendToLeader(msg);
				// connection.broadcast(msg);
			    } else if (payload.equals(Message.DONE)) {
				// This will be sent only by leader's
				// application. Stop service
				keepWorking.set(false);
			    } else {
				// CREATE MESSAGE for payload
				Message msg = new Message(++logicalClock, myId,
					payload);
				// Add to my queue
				processPendingQueue(Operation.ENQUEUE, msg);
				// System.out.println("[APPLICATION] Proposed: "
				// + msg.toString());
				connection.broadcast(msg);
			    }
			} catch (Exception e) {
			    e.printStackTrace();
			}
		    }

		} catch (Exception e) {

		} finally {
		    applicationQueueLock.unlock();
		    resourcesLock.unlock();
		}

		// Sleep for a moment
		try {
		    Thread.sleep(500);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
	    }
	    // System.out.println("[SERVICE] Stopping Send Service...");
	}
    }

    class ReceiveThread implements Runnable {
	@Override
	public void run() {
	    // System.out.println("[SERVICE] Starting Receiving Messages...");
	    while (keepWorking.get()) {
		try {
		    // Acquire Resources and Queue Locks
		    resourcesLock.lock();
		    applicationQueueLock.lock();

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
			    updatedMsg.setType(Message.Type.ACK);
			    connection.unicast(senderId, updatedMsg);
			    // System.out.println("[ACK] Sent ACK to Node "
			    // + senderId + " Msg: "
			    // + updatedMsg.toString());
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
		} finally {
		    // notEmpty.signal();
		    applicationQueueLock.unlock();
		    resourcesLock.unlock();
		}

		// Sleep for a moment
		try {
		    Thread.sleep(500);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
	    }
	    // System.out.println("[SERVICE] Stopping Receiving Messages...");
	}
    }

    public ArrayList<String> aDeliver() {
	// TODO Make this non-blocking
	ArrayList<String> messages = new ArrayList<String>();

	applicationQueueLock.lock();
	while (deliveredMessageQueue.peek() != null) {
	    messages.add(deliveredMessageQueue.poll());
	}
	applicationQueueLock.unlock();

	return messages;
    }

    public void aBroadCast(String message) {
	applicationQueueLock.lock();
	sendMessageQueue.add(message);
	applicationQueueLock.unlock();
    }

    @Override
    public void run() {
	System.out.println("\n <<< ATOMIC BROADCAST SERVICE STARTED >>>");

	// Start Send and Receive Thread
	Thread sendThread = new Thread(new SendThread());
	Thread receiveThread = new Thread(new ReceiveThread());

	// System.out.println("[SERVICE] Starting Send Thread...");
	sendThread.start();
	// System.out.println("[SERVICE] Starting Receive Thread...");
	receiveThread.start();

	// Wait for them to end
	try {
	    sendThread.join();
	    receiveThread.join();
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}

	System.out.println("\n <<< ATOMIC BROADCAST SERVICE STOPPED >>>");
    }

}
