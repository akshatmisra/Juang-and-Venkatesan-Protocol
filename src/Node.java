

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class Node {
	
	//node id
	private int nodeId = 0;
	//host name
	private String hostName = null;
	//port number
	private int portNumber = 0;
	//list of neighbors
	private List<Integer> neighbours = null;
	//if the node is active
	private Boolean isActive = true;
	//total message sent till now
	private int totalMessagesSent = 0;
	private int totalMessagesReceived = 0;
	private ConcurrentHashMap<Integer, Socket> connectedServerSocketMap = new ConcurrentHashMap<Integer, Socket>();
	private ConcurrentHashMap<String, Socket> connectedClientSocketMap = new ConcurrentHashMap<String, Socket>();
	private ConcurrentHashMap<Integer, ObjectOutputStream> connectedServerOutMap = new ConcurrentHashMap<Integer, ObjectOutputStream>();
	private ConcurrentHashMap<String, ObjectInputStream> connectedClientInMap = new ConcurrentHashMap<String, ObjectInputStream>();
	private ConcurrentHashMap<Integer, Vector<String>> sentMessagesMap = null;
	private ConcurrentHashMap<Integer, Vector<String>> receivedMessagesMap = null;
	private List<String> sentAfterLastCP = new ArrayList<String>();
	private List<String> receivedAfterLastCP = new ArrayList<String>();
	private BlockingQueue<String> applicationQueue = new LinkedBlockingQueue<String>();
	private BlockingQueue<String> juangVenkyQueue = new LinkedBlockingQueue<String>();
	private BlockingQueue<String> discardQueue = new LinkedBlockingQueue<String>();
	private int parent = -1;
	private List<Integer> children = new ArrayList<Integer>();
	private List<Integer> failedMsgList = new ArrayList<Integer>();
	private int tempCPCount = 0;
	private Boolean isFailed = false;
	private Boolean inRecoveryMode = false;
	private ConcurrentHashMap<Integer, Vector<Object>> cpList = new ConcurrentHashMap<Integer, Vector<Object>>();
	private ConcurrentHashMap<Integer, Vector<Object>> discardedpList = new ConcurrentHashMap<Integer, Vector<Object>>();
	
	
	public Node(int nodeId){
		this.nodeId = nodeId;
	}
	/**
	 * @return the nodeId
	 */
	public int getNodeId() {
		return nodeId;
	}
	/**
	 * @return the hostName
	 */
	public String getHostName() {
		return hostName;
	}
	/**
	 * @param hostName the hostName to set
	 */
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	/**
	 * @return the portNumber
	 */
	public int getPortNumber() {
		return portNumber;
	}
	/**
	 * @param portNumber the portNumber to set
	 */
	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}
	/**
	 * @return the neighbours
	 */
	public List<Integer> getNeighbours() {
		return neighbours;
	}
	/**
	 * @param neighbours the neighbours to set
	 */
	public void setNeighbours(List<Integer> neighbours) {
		this.neighbours = neighbours;
	}
	/**
	 * @return the isActive
	 */
	public Boolean getIsActive() {
		return isActive;
	}
	/**
	 * @param isActive the isActive to set
	 */
	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
		Common.updateNode(nodeId, this);
	}
	/**
	 * @return the totalMessagesSent
	 */
	public int getTotalMessagesSent() {
		return totalMessagesSent;
	}
	/**
	 * @param totalMessagesSent the totalMessagesSent to set
	 */
	public void setTotalMessagesSent(int totalMessagesSent) {
		this.totalMessagesSent = totalMessagesSent;
		Common.updateNode(nodeId, this);
	}
	/**
	 * @return the totalMessagesReceived
	 */
	public int getTotalMessagesReceived() {
		return totalMessagesReceived;
	}
	/**
	 * @param totalMessagesReceived the totalMessagesReceived to set
	 */
	public void setTotalMessagesReceived(int totalMessagesReceived) {
		this.totalMessagesReceived = totalMessagesReceived;
	}
	/**
	 * @return the connectedServerSocketMap
	 */
	public ConcurrentHashMap<Integer, Socket> getConnectedServerSocketMap() {
		return connectedServerSocketMap;
	}
	/**
	 * @param connectedServerSocketMap the connectedServerSocketMap to set
	 */
	public void setConnectedServerSocketMap(
			ConcurrentHashMap<Integer, Socket> connectedServerSocketMap) {
		this.connectedServerSocketMap = connectedServerSocketMap;
	}
	/**
	 * @return the connectedClientSocketMap
	 */
	public ConcurrentHashMap<String, Socket> getConnectedClientSocketMap() {
		return connectedClientSocketMap;
	}
	/**
	 * @param connectedClientSocketMap the connectedClientSocketMap to set
	 */
	public void setConnectedClientSocketMap(
			ConcurrentHashMap<String, Socket> connectedClientSocketMap) {
		this.connectedClientSocketMap = connectedClientSocketMap;
	}
	
	/**
	 * @return the connectedServerOutMap
	 */
	public ConcurrentHashMap<Integer, ObjectOutputStream> getConnectedServerOutMap() {
		return connectedServerOutMap;
	}
	/**
	 * @param connectedServerOutMap the connectedServerOutMap to set
	 */
	public void setConnectedServerOutMap(
			ConcurrentHashMap<Integer, ObjectOutputStream> connectedServerOutMap) {
		this.connectedServerOutMap = connectedServerOutMap;
	}
	/**
	 * @return the connectedClientInMap
	 */
	public ConcurrentHashMap<String, ObjectInputStream> getConnectedClientInMap() {
		return connectedClientInMap;
	}
	/**
	 * @param connectedClientInMap the connectedClientInMap to set
	 */
	public void setConnectedClientInMap(
			ConcurrentHashMap<String, ObjectInputStream> connectedClientInMap) {
		this.connectedClientInMap = connectedClientInMap;
	}
	/**
	 * @return the sentMessagesMap
	 */
	public ConcurrentHashMap<Integer, Vector<String>> getSentMessagesMap() {
		return sentMessagesMap;
	}
	/**
	 * @param sentMessagesMap the sentMessagesMap to set
	 */
	public void setSentMessagesMap(
			ConcurrentHashMap<Integer, Vector<String>> sentMessagesMap) {
		this.sentMessagesMap = sentMessagesMap;
		Common.updateNode(nodeId, this);
	}
	/**
	 * @return the receivedMessagesMap
	 */
	public ConcurrentHashMap<Integer, Vector<String>> getReceivedMessagesMap() {
		return receivedMessagesMap;
	}
	/**
	 * @param receivedMessagesMap the receivedMessagesMap to set
	 */
	public void setReceivedMessagesMap(
			ConcurrentHashMap<Integer, Vector<String>> receivedMessagesMap) {
		this.receivedMessagesMap = receivedMessagesMap;
		Common.updateNode(nodeId, this);
	}
	/**
	 * @return the sentAfterLastCP
	 */
	public List<String> getSentAfterLastCP() {
		return sentAfterLastCP;
	}
	/**
	 * @param sentAfterLastCP the sentAfterLastCP to set
	 */
	public void setSentAfterLastCP(List<String> sentAfterLastCP) {
		this.sentAfterLastCP = sentAfterLastCP;
		Common.updateNode(nodeId, this);
	}
	/**
	 * @return the receivedAfterLastCP
	 */
	public List<String> getReceivedAfterLastCP() {
		return receivedAfterLastCP;
	}
	/**
	 * @param receivedAfterLastCP the receivedAfterLastCP to set
	 */
	public void setReceivedAfterLastCP(List<String> receivedAfterLastCP) {
		this.receivedAfterLastCP = receivedAfterLastCP;
		Common.updateNode(nodeId, this);
	}
	/**
	 * @return the applicationQueue
	 */
	public BlockingQueue<String> getApplicationQueue() {
		return applicationQueue;
	}
	/**
	 * @param applicationQueue the applicationQueue to set
	 */
	public void setApplicationQueue(BlockingQueue<String> applicationQueue) {
		this.applicationQueue = applicationQueue;
		Common.updateNode(nodeId, this);
	}
	/**
	 * @return the discardQueue
	 */
	public BlockingQueue<String> getDiscardQueue() {
		return discardQueue;
	}
	/**
	 * @param discardQueue the discardQueue to set
	 */
	public void setDiscardQueue(BlockingQueue<String> discardQueue) {
		this.discardQueue = discardQueue;
		Common.updateNode(nodeId, this);
	}
	/**
	 * @return the juangVenkyQueue
	 */
	public BlockingQueue<String> getJuangVenkyQueue() {
		return juangVenkyQueue;
	}
	/**
	 * @param juangVenkyQueue the juangVenkyQueue to set
	 */
	public void setJuangVenkyQueue(BlockingQueue<String> juangVenkyQueue) {
		this.juangVenkyQueue = juangVenkyQueue;
		Common.updateNode(nodeId, this);
	}
	/**
	 * @return the parent
	 */
	public int getParent() {
		return parent;
	}
	/**
	 * @param parent the parent to set
	 */
	public void setParent(int parent) {
		this.parent = parent;
		Common.updateNode(nodeId, this);
	}
	/**
	 * @return the children
	 */
	public List<Integer> getChildren() {
		return children;
	}
	/**
	 * @param children the children to set
	 */
	public void setChildren(List<Integer> children) {
		this.children = children;
		Common.updateNode(nodeId, this);
	}
	/**
	 * @return the failedMsgList
	 */
	public List<Integer> getFailedMsgList() {
		return failedMsgList;
	}
	/**
	 * @param failedMsgList the failedMsgList to set
	 */
	public void setFailedMsgList(List<Integer> failedMsgList) {
		this.failedMsgList = failedMsgList;
		Common.updateNode(nodeId, this);
	}
	/**
	 * @return the tempCPCount
	 */
	public int getTempCPCount() {
		return tempCPCount;
	}
	/**
	 * @param tempCPCount the tempCPCount to set
	 */
	public void setTempCPCount(int tempCPCount) {
		this.tempCPCount = tempCPCount;
		Common.updateNode(nodeId, this);
	}
	/**
	 * @return the isFailed
	 */
	public Boolean getIsFailed() {
		return isFailed;
	}
	/**
	 * @param isFailed the isFailed to set
	 */
	public void setIsFailed(Boolean isFailed) {
		this.isFailed = isFailed;
		Common.updateNode(nodeId, this);
	}
	/**
	 * @return the inRecoveryMode
	 */
	public Boolean getInRecoveryMode() {
		return inRecoveryMode;
	}
	/**
	 * @param inRecoveryMode the inRecoveryMode to set
	 */
	public void setInRecoveryMode(Boolean inRecoveryMode) {
		this.inRecoveryMode = inRecoveryMode;
		Common.updateNode(nodeId, this);
	}
	/**
	 * @return the cpList
	 */
	public ConcurrentHashMap<Integer, Vector<Object>> getCpList() {
		return cpList;
	}
	/**
	 * @param cpList the cpList to set
	 */
	public void setCpList(ConcurrentHashMap<Integer, Vector<Object>> cpList) {
		this.cpList = cpList;
		Common.updateNode(nodeId, this);
	}
	/**
	 * @return the discardedpList
	 */
	public ConcurrentHashMap<Integer, Vector<Object>> getDiscardedpList() {
		return discardedpList;
	}
	/**
	 * @param discardedpList the discardedpList to set
	 */
	public void setDiscardedpList(
			ConcurrentHashMap<Integer, Vector<Object>> discardedpList) {
		this.discardedpList = discardedpList;
		Common.updateNode(nodeId, this);
		
	}
}
