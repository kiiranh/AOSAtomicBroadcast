/**
 * CS 6378.002 Advanced Operating Systems
 * Fall 2013
 * Project 1 - Atomic Broadcast
 *
 * @author Kiran Gavali
 */

package model;

public class NodeInfo {
    String hostname;
    int port;
    int nodeId;

    public NodeInfo(int nodeId, String hostname, int port) {
	this.nodeId = nodeId;
	this.hostname = hostname;
	this.port = port;
    }

    public int getNodeId() {
	return this.nodeId;
    }
    

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
	return "Node ID: " + this.nodeId + "\tHostname: " + this.hostname
		+ "\tPort: " + this.port;
    }

}
