/**
 * CS 6378.002 Advanced Operating Systems
 * Project 1
 * Fall 2013
 *
 * @author Kiran Gavali
 */
package startup;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import model.Connection;
import model.NodeInfo;
import service.Skeens;
import application.DistributedApplication;

public class Node {
    private int myId;
    private int leaderId;
    private int nodeCount;
    private String configFilePath;
    private Skeens skeenImpl;
    private Connection connection;
    private DistributedApplication distributedApp;
    private String deliveryLogDir;
    private String deliveryLog;

    /* Holds Info about other nodes */
    private ArrayList<NodeInfo> nodeList;

    /* Holds the messages to be broadcast - Threadsafe */
    private Queue<String> sendMessageQueue = new ConcurrentLinkedQueue<String>();

    /* Holds the messages which are delivered to the application - Threadsafe */
    private Queue<String> deliveredMessageQueue = new ConcurrentLinkedQueue<String>();

    public Node(int nodeId, String configFilePath, String logDir) {
	this.myId = nodeId;
	this.nodeCount = 0;
	this.configFilePath = configFilePath;
	this.connection = new Connection();
	this.nodeList = new ArrayList<NodeInfo>();
	this.deliveryLogDir = logDir;
	this.deliveryLog = logDir + "node" + myId + ".log";
    }

    private void cleanUpLogs(String logDir) {
	Path path = null;
	try {

	    for (NodeInfo node : nodeList) {
		path = FileSystems.getDefault().getPath(logDir,
			"/node" + node.getNodeId() + ".log");
		Files.deleteIfExists(path);
	    }
	} catch (IOException e) {
	}
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
		if (0 == nodeCount) {
		    nodeCount = Integer.parseInt(line);
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
	    // System.out.println("Exception: " + e.getMessage());
	} finally {
	    br.close();
	}

	// Set Leader
	leaderId = nodeList.get(0).getNodeId();

	System.out.println("Node count= " + nodeCount);
	System.out.println("--- Nodes ---");
	for (NodeInfo node : nodeList) {
	    System.out.println(node.toString());
	}
    }

    private String getMD5Checksum(String absoluteFilePath) throws Exception {
	MessageDigest md = MessageDigest.getInstance("MD5");
	FileInputStream fis = new FileInputStream(absoluteFilePath);

	byte[] dataBytes = new byte[1024];

	int nread = 0;
	while ((nread = fis.read(dataBytes)) != -1) {
	    md.update(dataBytes, 0, nread);
	}

	fis.close();
	byte[] mdbytes = md.digest();

	// convert the byte to hex format method 1
	StringBuffer sb = new StringBuffer();
	for (int i = 0; i < mdbytes.length; i++) {
	    sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16)
		    .substring(1));
	}

	// System.out.println("Digest(in hex format):: " + sb.toString());
	return sb.toString();
    }

    private void verifyMessageOrder() throws Exception {
	// Performed by leader
	// Compare the log files to my log file
	String leaderChecksum = getMD5Checksum(deliveryLog);
	System.out.println("[VERIFICATION] Leader Delivery file Checksum: "
		+ leaderChecksum);

	String otherChecksum = "Different";
	boolean pass = true;

	for (NodeInfo node : nodeList) {
	    if (node.getNodeId() != myId) {
		// Compute others checksum and compare.
		otherChecksum = getMD5Checksum(deliveryLogDir + "node"
			+ node.getNodeId() + ".log");
		System.out.println("[VERIFICATION] Node " + node.getNodeId()
			+ " Delivery file Checksum: " + otherChecksum);
		if (!leaderChecksum.equals(otherChecksum)) {
		    System.out
			    .println("\n <<< [VERIFICATION] FAILED: Checksum for Node "
				    + node.getNodeId()
				    + " differs from that of Leader. >>>\n");
		    pass = false;
		}
	    }
	}

	if (pass) {
	    System.out
		    .println("\n <<< [VERIFICATION] MESSAGE DELIVERY LOG CONSISTENT: SUCCESS :) >>> \n");
	}
    }

    public void start() throws Exception {
	// Read Config File
	readConfigFile();

	// Clear previous log files
	cleanUpLogs(deliveryLogDir);
	System.out.println("Deleted previous log files");

	// Setup sctp connections
	connection.setUp(nodeList, myId);
	Thread.sleep(10);

	// Elect Leader (Node 0 or first node in the list)
	leaderId = nodeList.get(0).getNodeId();
	boolean amILeader = false;

	// Send leader info to all (Or assume Node 0)
	if (leaderId == myId) {
	    // I'm leader. I will start off the program on all nodes.
	    amILeader = true;
	    connection.leaderNotifyStart(leaderId);
	} else {
	    // I'm not leader. I will wait for "START" from leader.
	    connection.awaitStartFromLeader();
	}

	// Start processing
	// Start skeens implementation (Own Thread)
	skeenImpl = new Skeens(connection, myId, nodeCount, sendMessageQueue,
		deliveredMessageQueue, deliveryLog);
	skeenImpl.start();

	// Create Initial Object State and pass to applications
	String startString = "INTIALSTRING";

	// Start application (own thread)
	distributedApp = new DistributedApplication(startString, nodeCount,
		sendMessageQueue, deliveredMessageQueue, amILeader);
	distributedApp.start();

	// When processing done (Each node sends DONE with result to Node 0)
	// After DONE from ALL NODEs, leader Sends STOP to all processes.
	// On Receiving STOP, output result and Stop
	// CHANGED: App wil send result to leader App when done.
	// Leader App will verify and output result

	// Wait for end of application thread
	distributedApp.join();

	// Wait for end of skeens
	skeenImpl.join();

	// Halt
	connection.tearDown();

	// VERIFY THAT THE MESSAGES WERE TOTALLY ORDERED BY READING LOG FILES
	if (leaderId == myId) {
	    System.out
		    .println("\n[LEADER NODE] Verifying message ordering on all nodes...");
	    verifyMessageOrder();
	}

	System.out.println("\n******* HALTING ******");
    }

    public static void main(String[] args) throws Exception {
	// Get my Node ID and config file path from CLI
	if (args.length < 3) {
	    throw new Exception(
		    "Invalid command: Please specify node id, absolute configuration file path & absolute log directory path"
			    + "\n\t Command: java Node <nodeId> <absolute configFilePath> <absolute log dir path>\n\t "
			    + "Eg: java Node 0 \"/home/kiiranh/workspace/AOS/src/config.txt\" \"/home/kiiranh/workspace/AOS/bin/\"");
	}

	String logDir = args[2];
	if (!logDir.endsWith("/")) {
	    logDir = logDir + "/";
	}
	// System.out.println(logDir);
	// System.out.println(logFile);

	new Node(Integer.parseInt(args[0]), args[1], logDir).start();
    }
}
