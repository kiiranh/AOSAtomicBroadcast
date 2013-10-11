/**
 * CS 6378.002 Advanced Operating Systems
 * Project 1
 * Fall 2013
 *
 * @author Kiran Gavali
 */
package service;

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
    private static int highestKnownTimestamp = 0;
    private static PriorityQueue<Message> pendingQueue = new PriorityQueue<>(0); // Message
										 // is
										 // Comparable
    /* Holds the messages to be broadcast */
    private static Queue<Message> sendMessageQueue;

    /* Holds the messages which are delivered to the application */
    private static Queue<Message> deliveredMessageQueue;

    private class sendThread implements Runnable {
	@Override
	public void run() {
	    // TODO
	    //
	}
    }

    private class receiveThread implements Runnable {
	@Override
	public void run() {
	    // TODO Auto-generated method stub

	}
    }

    public Skeens(Connection connection, int myId,
	    Queue<Message> sendMessageQueue,
	    Queue<Message> deliveredMessageQueue) {
	Skeens.connection = connection;
	Skeens.myId = myId;
	Skeens.sendMessageQueue = sendMessageQueue; 
    }

    @Override
    public void run() {
	// TODO Auto-generated method stub
	// Start Send and Receive Thread
	// Wait for them to end?
    }
}
