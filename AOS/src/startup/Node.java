/**
 * CS 6378.002 Advanced Operating Systems
 * Project 1
 * Fall 2013
 *
 * @author Kiran Gavali
 */
package startup;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import model.Connection;
import model.Message;
import model.NodeInfo;
import service.Skeens;
import application.DistributedApplication;

public class Node {
    private int myId;
    private int leaderId;
    private int numberOfNodes;
    private String configFilePath;
    private Skeens skeenImpl;
    private Connection connection;
    private DistributedApplication distributedApp;

    /* Holds Info about other nodes */
    private ArrayList<NodeInfo> nodeList;

    /* Holds the messages to be broadcast - Threadsafe */
    private Queue<String> sendMessageQueue = new ConcurrentLinkedQueue<String>();

    /* Holds the messages which are delivered to the application - Threadsafe */
    private Queue<Message> deliveredMessageQueue = new ConcurrentLinkedQueue<Message>();

    public Node(int nodeId, String configFilePath) {
	this.myId = nodeId;
	this.numberOfNodes = 0;
	this.configFilePath = configFilePath;
	this.connection = new Connection();
	this.nodeList = new ArrayList<NodeInfo>();
    }

    private void readConfigFile() throws Exception {
	BufferedReader br = new BufferedReader(new FileReader(
		this.configFilePath));
	try {
	    String line;
	    while ((line = br.readLine().trim()) != null) {
		// Check if this is a comment
		if (line.startsWith("#")) {
		    continue;
		}
		// Not a comment. Ignore the comment part
		line = line.substring(0, line.indexOf("#")).trim();
		if (0 == numberOfNodes) {
		    numberOfNodes = Integer.parseInt(line);
		} else {
		    String[] parts = line.split(" ");
		    if (parts.length != 3) {
			throw new Exception(
				"Invalid Configuration file: Format for specifying node.");
		    }

		    NodeInfo node = new NodeInfo(Integer.parseInt(parts[0]),
			    parts[1], Integer.parseInt(parts[2]));
		    nodeList.add(node);
		}
	    }
	} catch (Exception e) {
	    System.out.println("Exception: " + e.getMessage());
	} finally {
	    br.close();
	}

	// Set Leader
	leaderId = nodeList.get(0).getNodeId();

	System.out.println("Node count= " + numberOfNodes);
	System.out.println("--- Nodes ---");
	for (NodeInfo node : nodeList) {
	    System.out.println(node.toString());
	}
    }

    public void start() throws Exception {
	// TODO
	// Read Config File
	readConfigFile();

	// Setup sctp connections
	connection.setUp(nodeList, myId);
	Thread.sleep(10);

	// Elect Leader (Node 0 or first node in the list)
	leaderId = nodeList.get(0).getNodeId();

	// Send leader info to all (Or assume Node 0)
	if (leaderId == myId) {
	    // I'm leader. I will start off the program on all nodes.
	    connection.leaderNotifyStart(leaderId);
	} else {
	    // I'm not leader. I will wait for "START" from leader.
	    connection.awaitStartFromLeader();
	}

	// Start processing
	// Start skeens implementation (Own Thread)
	skeenImpl = new Skeens(connection, myId, numberOfNodes,
		sendMessageQueue, deliveredMessageQueue);
	skeenImpl.start();

	// TODO Create Initial Object State and pass to applications
	String startString = new String("IntialString");

	// Start application (own thread)
	distributedApp = new DistributedApplication(startString,
		sendMessageQueue, deliveredMessageQueue);
	distributedApp.start();

	// When processing done (Each node sends DONE with result to Node 0)
	// After DONE from ALL NODEs, leader Sends STOP to all processes.
	// On Receiving STOP, output result and Stop

	// Wait for end of application thread
	// Wait for end of skeens

	connection.tearDown();
    }

    public static void main(String[] args) throws Exception {
	// Get my Node ID and config file path from CLI
	if (args.length < 2) {
	    throw new Exception(
		    "Invalid command: Please specify node id and configuration file path. "
			    + "\n\t Command: java Node <nodeId> <configFilePath>\n\t "
			    + "Eg: java Node 0 \"/home/kiiranh/workspace/AOS/src/config.txt\"");
	}

	new Node(Integer.parseInt(args[0]), args[1]).start();
    }
}
