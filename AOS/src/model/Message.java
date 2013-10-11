/**
 * CS 6378.002 Advanced Operating Systems
 * Fall 2013
 * Project 1 - Atomic Broadcast
 *
 * @author Kiran Gavali
 */

package model;

public class Message implements Comparable<Message> {

    public static final char MESSAGE_FIELDS_SEPARATOR = ':';
    public static final char MESSAGE_ID_SEPARATOR = '-';

    public static enum State {
	PENDING, READY;
    }

    public static enum Type {
	NEW, ACK, FINAL;
    }

    private class MessageId implements Comparable<MessageId> {
	final private int originatorTimeStamp; // Timestamp when the message was
					       // originally sent
	final private int originatorId;

	public MessageId(int ts, int id) {
	    originatorTimeStamp = ts;
	    originatorId = id;
	}

	@Override
	public int hashCode() {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + getOuterType().hashCode();
	    result = prime * result + originatorId;
	    result = prime * result + originatorTimeStamp;
	    return result;
	}

	@Override
	public boolean equals(Object obj) {
	    if (this == obj)
		return true;
	    if (obj == null)
		return false;
	    if (getClass() != obj.getClass())
		return false;
	    MessageId other = (MessageId) obj;
	    if (!getOuterType().equals(other.getOuterType()))
		return false;
	    if (originatorId != other.originatorId)
		return false;
	    if (originatorTimeStamp != other.originatorTimeStamp)
		return false;
	    return true;
	}

	private Message getOuterType() {
	    return Message.this;
	}

	@Override
	public int compareTo(MessageId other) {
	    if (originatorTimeStamp == other.originatorTimeStamp) {
		// TS equal- break ties with senderId
		return Integer.valueOf(originatorId).compareTo(
			Integer.valueOf(other.originatorId));
	    } else {
		// TS not equal
		return Integer.valueOf(originatorTimeStamp).compareTo(
			Integer.valueOf(other.originatorTimeStamp));
	    }
	}
    }

    private MessageId msgId; // Will be represented as
			     // "originatorTimeStamp-originatorId"
    private State status;
    private Type type;

    /*
     * This will be updated depending on the message type: NEW => original TS
     * ACK => proposed TS FIN => final TS
     */
    private int timeStamp;
    private String payload;

    public Message(int origTS, int origID, String applicationMessage) {
	this.msgId = new MessageId(origTS, origID);
	this.status = State.PENDING;
	this.type = Type.NEW;
	this.timeStamp = msgId.originatorTimeStamp;
	this.payload = applicationMessage;
    }

    public int getTimeStamp() {
	return this.timeStamp;
    }

    public void acknowledge(int newTimestamp) {
	type = Type.ACK;
	this.timeStamp = newTimestamp;
    }

    public void finalize(int finalTimeStamp) {
	this.type = Type.FINAL;
	this.timeStamp = finalTimeStamp;
	this.status = State.READY;
    }

    @Override
    public String toString() {
	// ID(TS-SenderID):State:Type:TimeStamp:payload
	return "" + msgId.originatorTimeStamp + MESSAGE_ID_SEPARATOR
		+ msgId.originatorId + MESSAGE_FIELDS_SEPARATOR + status
		+ MESSAGE_FIELDS_SEPARATOR + type + MESSAGE_FIELDS_SEPARATOR
		+ timeStamp + MESSAGE_FIELDS_SEPARATOR + payload;
    }

    public static Message parseMessage(String msgStr) {
	String[] msgParts = msgStr.split("" + MESSAGE_FIELDS_SEPARATOR);
	// ID(TS-SenderID):State:Type:TimeStamp:payload
	int oriTS = Integer.parseInt(msgParts[0].split(""
		+ MESSAGE_ID_SEPARATOR)[0]);
	int oriID = Integer.parseInt(msgParts[0].split(""
		+ MESSAGE_ID_SEPARATOR)[1]);

	Message msg = new Message(oriTS, oriID, msgParts[4]);
	msg.status = State.valueOf(msgParts[1]);
	msg.type = Type.valueOf(msgParts[2]);
	msg.timeStamp = Integer.parseInt(msgParts[3]);

	return msg;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((msgId == null) ? 0 : msgId.hashCode());
	// result = prime * result + ((payload == null) ? 0 :
	// payload.hashCode());
	// result = prime * result + ((status == null) ? 0 : status.hashCode());
	// result = prime * result + timeStamp;
	// result = prime * result + ((type == null) ? 0 : type.hashCode());
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj)
	    return true;
	if (obj == null)
	    return false;
	if (getClass() != obj.getClass())
	    return false;
	Message other = (Message) obj;
	// Assuming MessageID is unique, compare only msgId
	if (msgId == null) {
	    if (other.msgId != null)
		return false;
	} else if (!msgId.equals(other.msgId))
	    return false;
	// if (payload == null) {
	// if (other.payload != null)
	// return false;
	// } else if (!payload.equals(other.payload))
	// return false;
	// if (status != other.status)
	// return false;
	// if (timeStamp != other.timeStamp)
	// return false;
	// if (type != other.type)
	// return false;
	return true;
    }

    @Override
    public int compareTo(Message other) {
	if (timeStamp == other.timeStamp) {
	    // Current timestamp equal - break tie using message id
	    return msgId.compareTo(other.msgId);
	} else {
	    // timestamp differ.
	    return Integer.valueOf(timeStamp).compareTo(
		    Integer.valueOf(other.timeStamp));
	}
    }
}
