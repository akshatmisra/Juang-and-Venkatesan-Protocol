import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class ProcessRecovery extends Thread{

	private int nodeId;
	private List<Integer> lock = null;
	private String message = null;
	private ConcurrentHashMap<Integer,String> receivedRollBackInfoList = new ConcurrentHashMap<Integer,String>();
	private ConcurrentHashMap<Integer,String> receivedRecoveryList = new ConcurrentHashMap<Integer,String>();
	private boolean b = false;

	public ProcessRecovery(int nodeId, List<Integer> lock){
		this.nodeId = nodeId;
		this.lock = lock;
		this.setName("PROCESSOR "+nodeId);
	}

	public void run(){

		while(true){

			try {
				message = Common.getNodeMap().get(nodeId).getJuangVenkyQueue().take();
				processMessage(message);
			}catch(Exception e){
			}
		}
	}

	private synchronized void processMessage(String message) {

		String [] msg = message.split(",");
		String msgType = msg[0];
		int srcNode = Integer.parseInt(msg[1].trim());
		int destNode = Integer.parseInt(msg[2].trim());
		int failedNode = Integer.parseInt(msg[3].trim());

		if(msgType.equalsIgnoreCase("resume")){

			System.out.println(nodeId+ " *********************in PROCESS RECOVERY, RECEIVED message "+message);

			Common.getNodeMap().get(nodeId).setInRecoveryMode(false);
			Common.getNodeMap().get(nodeId).setIsFailed(false);
			Common.getNodeMap().get(nodeId).setTempCPCount(0);

			for(int child : Common.getNodeMap().get(nodeId).getChildren()){
				String resume = "resume"+","+nodeId+","+child+","+"-1";
				new StartClient(resume);
			}

			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			synchronized (lock) {
				lock.clear();
				lock.notify();
			}

			for(Vector<Object> vec : Common.getNodeMap().get(nodeId).getDiscardedpList().values()){

				System.out.println(nodeId+" Discarded checkpoints after resume");
				@SuppressWarnings("unchecked")
				ConcurrentHashMap<Integer, String> toSendMap = (ConcurrentHashMap<Integer, String>) vec.get(3);
				for(String resendMsg : toSendMap.values()){
					new StartClient(resendMsg);
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			Common.getNodeMap().get(nodeId).getDiscardedpList().clear();

			System.out.println(nodeId+" Finished sending discarded check points");
			new REBProtocol(nodeId, lock);
		}
		
		if(msgType.equalsIgnoreCase("stop")){

			System.out.println(nodeId+ " *********************in PROCESS RECOVERY, RECEIVED message "+message);

			for(int childId : Common.getNodeMap().get(nodeId).getChildren()){
				String stopMessage = "stop"+","+nodeId+","+childId+","+failedNode;
				new StartClient(stopMessage);
			}
			Common.getNodeMap().get(nodeId).setInRecoveryMode(true);

			if(failedNode == nodeId && Common.rollBack(Common.getNodeMap().get(nodeId))){
				System.out.println(nodeId+ " I AM THE FAILING NODE, so b = true");
				System.out.println(" BEFORE ROLLBACK ======> "+Common.getNodeMap().get(nodeId).getTotalMessagesSent());
				System.out.println(" AFTER ROLLBACK ======> "+Common.getNodeMap().get(nodeId).getTotalMessagesSent());
			}
			else{
				System.out.println(nodeId+ " I AM NOT THE FAILING NODE, so b = false");
				b = false;
			}

			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if(Common.getNodeMap().get(nodeId).getChildren().size() == 0){
				System.out.println(nodeId+" I am the LEAF node, so sending rollback "+b);
				String rollBackMessage = "rollback"+","+nodeId+","+(Common.getNodeMap().get(nodeId).getParent())+","+failedNode+","+b;
				new StartClient(rollBackMessage);
				b = false;
			}

		}

		else if(msgType.equalsIgnoreCase("rollback")){

			System.out.println(nodeId+ " *********************in PROCESS RECOVERY, RECEIVED message "+message);
			synchronized (new Object()) {
				receivedRollBackInfoList.put(srcNode, message);

				if(receivedRollBackInfoList.size() ==  Common.getNodeMap().get(nodeId).getChildren().size()){
					for(String m : receivedRollBackInfoList.values()){
						String [] marr = m.split(",");
						boolean isRollingBack = Boolean.parseBoolean(marr[4]);
						//System.out.println(nodeId + "receiving "+m.getIsRollingBack()+" from "+m.getSrcNodeId());
						b = b || isRollingBack;
						//System.out.println(nodeId +" AFTER <<<<<<<<<<<< receiving roll back, B value is "+b);
					}


					if(Common.getNodeMap().get(nodeId).getParent() == -1){
						System.out.println(" inside rolling back, at root, calculated b value is "+b);

						if(b){
							b = false;
							//System.out.println(nodeId+ " I am the parent node receiving roll back, and B is true, so sending START RECOVERY");
							for(int child : Common.getNodeMap().get(nodeId).getChildren()){
								String startRecovery = "startrecovery"+","+nodeId+","+child+","+failedNode;
								new StartClient(startRecovery);
							}
							try {
								Thread.sleep(200);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							for(int neighbor : Common.getNodeMap().get(nodeId).getNeighbours()){
								if(neighbor != nodeId){
									String recovery = "recovery"+","+nodeId+","+neighbor+","+failedNode+","
									+Common.getNodeMap().get(nodeId).getSentMessagesMap().get(neighbor).size();
									new StartClient(recovery);
								}
							}
						}
						else{
							System.out.println(nodeId+ " I am the parent node receiving roll back, and B is false, so sending RESUME");

							for(int child : Common.getNodeMap().get(nodeId).getChildren()){
								System.out.println(" resume sent to "+child);
								String resume = "resume"+","+nodeId+","+child+","+"-1";
								new StartClient(resume);
								Common.getFailureList().remove(0);
							}
							
							Common.getNodeMap().get(nodeId).setInRecoveryMode(false);
							Common.getNodeMap().get(nodeId).setIsFailed(false);
							Common.getNodeMap().get(nodeId).setTempCPCount(0);
							
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							synchronized (lock) {
								lock.clear();
								lock.notify();
							}
							
							for(Vector<Object> vec : Common.getNodeMap().get(nodeId).getDiscardedpList().values()){

								System.out.println(nodeId+" Discarded checkpoints after resume");
								@SuppressWarnings("unchecked")
								ConcurrentHashMap<Integer, String> toSendMap = (ConcurrentHashMap<Integer, String>) vec.get(3);
								for(String resendMsg : toSendMap.values()){
									new StartClient(resendMsg);
									try {
										Thread.sleep(10);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
								}
							}
							Common.getNodeMap().get(nodeId).getDiscardedpList().clear();

							System.out.println(nodeId+" Finished sending discarded check points");
							new REBProtocol(nodeId, lock);
						}
					}
					else{
						String rollBackMessage = "rollback"+","+nodeId+","+(Common.getNodeMap().get(nodeId).getParent())+","+failedNode+","+b;
						System.out.println(nodeId+" received from children, so rolling back "+b);
						new StartClient(rollBackMessage);
						b = false;
					}
					receivedRollBackInfoList.clear();
				}
			}

		}

		else if(msgType.equalsIgnoreCase("startrecovery")){

			System.out.println(nodeId+ " *********************in PROCESS RECOVERY, RECEIVED message "+message);
			for(int child : Common.getNodeMap().get(nodeId).getChildren()){
				String startRecovery = "startrecovery"+","+nodeId+","+child+","+failedNode;
				new StartClient(startRecovery);
			}

			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			for(int neighbor : Common.getNodeMap().get(nodeId).getNeighbours()){
				if(neighbor != nodeId){
					String recoveryMsg = "recovery"+","+nodeId+","+neighbor+","+failedNode+","
					+Common.getNodeMap().get(nodeId).getSentMessagesMap().get(neighbor).size();
					new StartClient(recoveryMsg);
				}
			}
		}

		else if(msgType.equalsIgnoreCase("recovery")){
			System.out.println(nodeId+ " *********************in PROCESS RECOVERY, RECEIVED message "+message +" message count => "+Integer.parseInt(msg[4].trim()));

			receivedRecoveryList.put(srcNode, message);

			if(receivedRecoveryList.size() == Common.getNodeMap().get(nodeId).getNeighbours().size()){
				//Boolean rollingBack = false;
				for(String m : receivedRecoveryList.values()){
					String [] m1 = m.split(",");
					int totalSentFromThisNode = Integer.parseInt(m1[4].trim());
					int totalReceivedToThisNeighbor = Common.getNodeMap().get(nodeId).getReceivedMessagesMap().get(Integer.parseInt(m1[1].trim())).size();
					System.out.println(nodeId+" .... "+m1[1]+"  %%%%%%%%%%% COMPARISION %%%%%%%%%% "+totalSentFromThisNode+" ==== "+totalReceivedToThisNeighbor);

					if(totalReceivedToThisNeighbor > totalSentFromThisNode && Common.rollBack(Common.getNodeMap().get(nodeId))){
						System.out.println(nodeId+" back to previous checkpoint "+Common.getNodeMap().get(nodeId).getTotalMessagesSent());
						System.out.println(nodeId+" this is previous CP   :)  "+Common.getNodeMap().get(nodeId).getTotalMessagesSent());
						b = true;
						break;
					}
					else
						b = false;
				}
				System.out.println(nodeId+" Rollback computed is ==== "+b);
				if(Common.getNodeMap().get(nodeId).getChildren().size() == 0){
					String rollBackMessage = "rollback"+","+nodeId+","+(Common.getNodeMap().get(nodeId).getParent())+","+failedNode+","+b;
					//System.out.println(nodeId+" i am rolling back and rollback value is "+b);
					new StartClient(rollBackMessage);
					b = false;
				}
				receivedRecoveryList.clear();
			}

		}
	}
}
