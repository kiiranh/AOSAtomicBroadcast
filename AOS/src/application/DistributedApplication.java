/**
 * CS 6378.002 Advanced Operating Systems
 * Fall 2013
 * Project 1 - Atomic Broadcast
 *
 * @author Kiran Gavali
 */

package application;

import java.util.Queue;

import model.Message;

public final class DistributedApplication extends Thread {
    private StringBuilder localString;

    /* Holds the messages to be broadcast */
    private static Queue<Message> sendMessageQueue;

    /* Holds the messages which are delivered to the application */
    private static Queue<Message> deliveredMessageQueue;

    public DistributedApplication(String copy, Queue<String> sendMessageQueue,
	    Queue<Message> deliveredMessageQueue) {
	this.localString = new StringBuilder(copy);
    }

    public void proposeAction(String msg) {
	// TODO Broadcast to all nodes.
    }

    public void processAction(String msg) {
	// TODO Perform the action on local queue
    }

    @Override
    public void run() {
	// TODO
	// Sleep random amount of time to perform my actions
	// Create send and receive threads ?
	// TODO Log the actions performed
    }
}