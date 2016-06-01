import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;



public class CheckPointingProtocol {

	private int nodeId;
	public CheckPointingProtocol(int nodeId){

		this.nodeId = nodeId;
	}

	@SuppressWarnings("unchecked")
	public synchronized void takeCheckPoint() {

		Node n = Common.getNodeMap().get(nodeId);
		int lastCPId = n.getCpList().size();
		Vector<Object> lastCP = n.getCpList().get(lastCPId);
		if((Integer)lastCP.get(1) != n.getTotalMessagesSent()){

			Vector<Object> cpData = new Vector<Object>();

			ConcurrentHashMap<Integer, Vector<String>> cpSentMap = new ConcurrentHashMap<Integer, Vector<String>>();
			for(Integer i : n.getSentMessagesMap().keySet()){
				Vector<String> sendMsgVec = new Vector<String>();
				for(String s : n.getSentMessagesMap().get(i)){
					sendMsgVec.add(s);
				}
				cpSentMap.put(i, sendMsgVec);
			}

			ConcurrentHashMap<Integer, Vector<String>> cpRcvMap = new ConcurrentHashMap<Integer, Vector<String>>();
			for(Integer i : n.getReceivedMessagesMap().keySet()){
				Vector<String> rcvdMsgVec = new Vector<String>();
				for(String s : n.getReceivedMessagesMap().get(i)){
					rcvdMsgVec.add(s);
				}
				cpRcvMap.put(i, rcvdMsgVec);
			}
			cpData.add(n.getIsActive());
			cpData.add(n.getTotalMessagesSent());
			cpData.add(n.getTotalMessagesReceived());
			cpData.add(cpSentMap);
			cpData.add(cpRcvMap);
			cpData.add(n.getReceivedAfterLastCP());
			cpData.add(n.getSentAfterLastCP());
			n.getCpList().put(lastCPId+1, cpData);	
			Common.getNodeMap().get(nodeId).setTempCPCount(n.getCpList().size());

			Vector<Object> recentCP = n.getCpList().get(n.getCpList().size());
			System.out.println("***************************** CHECKPOINT DETAILS **************************************");
			System.out.println(nodeId+" size =>  "+Common.getNodeMap().get(nodeId).getCpList().size()
					+" Total Sent=> "+(Integer)recentCP.get(1)+" Total Received=> "+(Integer)recentCP.get(2)
					+ " sent message map => "+(ConcurrentHashMap<Integer, Vector<Object>>)recentCP.get(3)
					+ " received message map => "+(ConcurrentHashMap<Integer, Vector<Object>>)recentCP.get(4));
			System.out.println("=======================================================================================");


			String [] nextFailingNode = Common.getFailureList().get(0).split(" ");
			if(Integer.parseInt(nextFailingNode[0].trim()) == nodeId 
					&& Integer.parseInt(nextFailingNode[1].trim()) == Common.getNodeMap().get(nodeId).getTempCPCount()){
				System.out.println(nodeId+" failing after taking "+Common.getNodeMap().get(nodeId).getCpList().size()+" checkpoints");
				Common.getNodeMap().get(nodeId).setIsFailed(true);
				processFailure();
			}
		}
	}

	public void processFailure(){

		//System.out.println("inside process failure method");
		int lastFailedMsgId = Common.getNodeMap().get(nodeId).getFailedMsgList().size();

		for(int neighbors : Common.getNodeMap().get(nodeId).getNeighbours()){
			String failureMessage = "failure"+","+nodeId+","+neighbors+","+(lastFailedMsgId+1)+","+nodeId;
			new StartClient(failureMessage);
		}
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Common.getNodeMap().get(nodeId).getFailedMsgList().add(lastFailedMsgId+1);
	}

}
