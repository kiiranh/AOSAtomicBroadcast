/**
 * CS 6378.002 Advanced Operating Systems
 * Fall 2013
 * Project 1 - Atomic Broadcast
 *
 * @author Kiran Gavali
 */

package application;

import java.util.ArrayList;
import java.util.Queue;
import java.util.Random;

public final class DistributedApplication extends Thread {
    private String localString;
    private ArrayList<String> actions;
    private int nodeCount;
    private static boolean amILeader;
    private ArrayList<String> resultList = new ArrayList<String>();
    private int pendingResultCount;
    private static final int MY_COMPUTATION_COUNT = 7;

    /* Holds the messages to be broadcast */
    private static Queue<String> sendMessageQueue;

    /* Holds the messages which are delivered to the application */
    private static Queue<String> deliveredMessageQueue;

    public DistributedApplication(String original, int nodeCount,
	    Queue<String> sendMessageQueue,
	    Queue<String> deliveredMessageQueue, boolean amILeader) {
	this.localString = original;
	actions = new ArrayList<String>();
	actions.add("UC"); // Upper case
	actions.add("LC"); // Lower case
	actions.add("RL"); // Remove the last element
	actions.add("RI1"); // Replace I/i with 1
	actions.add("RA4"); // Replace A/a with 4
	actions.add("REV"); // Reverse the string
	actions.add("APPZ"); // Append Z

	this.nodeCount = nodeCount;
	this.pendingResultCount = nodeCount - 1;
	DistributedApplication.amILeader = amILeader;
	DistributedApplication.sendMessageQueue = sendMessageQueue;
	DistributedApplication.deliveredMessageQueue = deliveredMessageQueue;
    }

    private void processResults() {
	// At this point, App processing is done
	System.out.println("\nMY FINAL OBJECT: " + localString + "\n");
	if (amILeader) {
	    // Leader:
	    // Wait for result from all nodes
	    // Verify results and output
	    while (pendingResultCount > 0) {
		// Sleep a while
		try {
		    Thread.sleep(100);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}

		while (deliveredMessageQueue.peek() != null) {
		    String res = deliveredMessageQueue.poll();
		    if (res.startsWith("=")) {
			resultList.add(res.substring(1, res.length()));
			--pendingResultCount;
		    }
		}
	    }

	    // Got all Results. Verify
	    for (String result : resultList) {
		if (!result.equals(localString)) {
		    System.out
			    .println("\n <<< [VERIFICATION] DISTRIBUTED COMPUTATION INCONSISTENT => FAILED :) >>>\n");
		    return;
		}
	    }

	    System.out
		    .println("\n <<< [VERIFICATION] DISTRIBUTED COMPUTATION CONSISTENT => SUCCESS :) >>>\n");

	} else {
	    // Worker:
	    // Send result to Leader Node
	    sendMessageQueue.add("=" + localString);
	    System.out.println("[APPLICATION] SENT RESULT TO LEADER");
	}
    }

    @Override
    public void run() {
	int msgToProcess = MY_COMPUTATION_COUNT * nodeCount;
	int msgToPropose = MY_COMPUTATION_COUNT;
	Random r = new Random();

	System.out.println("\nINITIAL OBJECT: " + localString + "\n");

	while (msgToProcess > 0) {
	    if (msgToPropose > 0) {
		// Propose my action (Selected Randomly from the available set)
		sendMessageQueue
			.add(Integer.toString(r.nextInt(actions.size())));
		--msgToPropose;
	    }

	    // TODO Make this blocking
	    // Check if there are any messages to be delivered (a-deliver)
	    while (deliveredMessageQueue.peek() != null) {
		// Perform the action
		String msg = deliveredMessageQueue.poll();
		if (msg.startsWith("=")) {
		    // Result msg
		    resultList.add(msg.substring(1, msg.length()));
		    --pendingResultCount;
		} else {
		    // ACTION MESSAGE
		    switch (Integer.valueOf(msg)) {
		    case 0:
			localString = localString.toUpperCase();
			break;

		    case 1:
			localString = localString.toLowerCase();
			break;

		    case 2:
			if (localString.length() > 0) {
			    localString = localString.substring(0,
				    localString.length() - 1);
			}
			break;

		    case 3:
			localString = localString.replaceAll("[Ii]", "1");
			break;

		    case 4:
			localString = localString.replaceAll("[Aa]", "4");
			break;

		    case 5:
			localString = new StringBuilder(localString).reverse()
				.toString();

		    case 6:
			localString = localString + "Z";

		    default:
			break;
		    }

		    --msgToProcess;
		}

	    }

	    // Sleep a while
	    try {
		Thread.sleep(100);
	    } catch (InterruptedException e) {
		e.printStackTrace();
	    }
	}

	processResults();
	// Signal end by sending a final DONE message
	sendMessageQueue.add("DONE");
    }
}
