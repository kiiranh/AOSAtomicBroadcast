/**
 * CS 6378.002 Advanced Operating Systems
 * Fall 2013
 * Project 1 - Atomic Broadcast
 *
 * @author Kiran Gavali
 */

package application;

import java.util.ArrayList;
import java.util.Random;

import service.Skeens;

public final class DistributedApplication extends Thread {
    private String localString;
    private ArrayList<String> actions;
    private int nodeCount;
    private static boolean amILeader;
    private ArrayList<String> resultList = new ArrayList<String>();
    private int pendingResultCount;
    private static Skeens service;
    private static final int MY_COMPUTATION_COUNT = 50;

    public DistributedApplication(String original, int nodeCount,
	    boolean amILeader, Skeens service) {
	this.localString = original;
	actions = new ArrayList<String>();
	actions.add("UC"); // Upper case
	actions.add("LC"); // Lower case
	actions.add("RL"); // Remove the last element
	actions.add("APPZ"); // Append Z to tail
	actions.add("RMZ"); // Remove Z from tail
	actions.add("APP*"); // Append *
	actions.add("RI1"); // Replace I/i with 1
	actions.add("RA4"); // Replace A/a with 4
	actions.add("REV"); // Reverse the string
	

	this.nodeCount = nodeCount;
	this.pendingResultCount = nodeCount - 1;
	DistributedApplication.amILeader = amILeader;
	DistributedApplication.service = service;
    }

    private void processResults() {
	// At this point, App processing is done
	System.out.println("\n[APPLICATION] MY FINAL OBJECT: " + localString
		+ "\n");
	if (amILeader) {
	    // Leader:
	    // Wait for result from all nodes
	    // Verify results and output
	    while (pendingResultCount > 0) {
		// Sleep a while
		try {
		    Thread.sleep(10);
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}

		ArrayList<String> msgs = service.aDeliver();

		for (String msg : msgs) {
		    if (msg.startsWith("=")) {
			resultList.add(msg.substring(1, msg.length()));
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
	    // service.aBroadCast("DONE");

	} else {
	    // Worker:
	    // Send result to Leader Node
	    service.aBroadCast("=" + localString);
	    System.out.println("[APPLICATION] SENT RESULT TO LEADER");
	}
    }

    class SendThread implements Runnable {
	@Override
	public void run() {
	    int msgToPropose = MY_COMPUTATION_COUNT;
	    while (msgToPropose > 0) {
		// Sleep random time
		try {
		    Thread.sleep(200);
		} catch (Exception e) {
		    ;
		    ;
		}

		// Propose my action (Selected Randomly from the available set)
		service.aBroadCast(Integer.toString(new Random()
			.nextInt(actions.size())));
		--msgToPropose;
	    }
	}
    }

    class ReceiveThread implements Runnable {
	@Override
	public void run() {
	    int msgToProcess = MY_COMPUTATION_COUNT * nodeCount;

	    while (msgToProcess > 0) {
		try {
		    Thread.sleep(100);
		} catch (Exception e) {
		}

		ArrayList<String> msgs = service.aDeliver();

		for (String msg : msgs) {
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
			    localString = localString + "Z";
			    break;

			case 4:
			    if (localString.endsWith("Z")) {
				localString = localString.substring(0,
					localString.length() - 1);
			    }
			    break;

			case 5:
			    localString = localString.concat("*");
			    break;



			case 6:
			    localString = localString.replaceAll("[Ii]", "1");
			    break;

			case 7:
			    localString = localString.replaceAll("[Aa]", "4");
			    break;

			case 8:
			    localString = new StringBuilder(localString)
				    .reverse().toString();
			    break;


			default:
			    break;
			}

			--msgToProcess;
		    }

		}
	    }
	}
    }

    @Override
    public void run() {
	System.out.println("\n[APPLICATION] INITIAL OBJECT: " + localString
		+ "\n");

	Thread sendThread = new Thread(new SendThread());
	Thread receiveThread = new Thread(new ReceiveThread());

	System.out.println("[APPLICATION] Starting Send Thread...");
	sendThread.start();
	System.out.println("[APPLICATION] Starting Receive Thread...");
	receiveThread.start();

	// Wait for them to end
	try {
	    sendThread.join();
	    receiveThread.join();
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}

	processResults();
	service.aBroadCast("DONE");
    }
}
