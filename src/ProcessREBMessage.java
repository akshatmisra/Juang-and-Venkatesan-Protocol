import java.util.List;

public class ProcessREBMessage extends Thread{

	private int nodeId;
	private String message = null;
	private List<Integer> lock = null;

	public ProcessREBMessage(int nodeId, List<Integer> lock){
		this.nodeId = nodeId;
		this.lock = lock;
		this.setName("PROCESSOR "+nodeId);
	}

	public synchronized void run(){

		while(true){

			synchronized(lock){
				if(lock.size() > 0){
					try {
						System.out.println(nodeId+" node is waiting on lock");
						System.out.println("FINAL SENT MAP => "+Common.getNodeMap().get(nodeId).getSentMessagesMap());
						System.out.println("FINAL RECEIVE MAP => "+Common.getNodeMap().get(nodeId).getReceivedMessagesMap());
						lock.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else{
					try {
						message = Common.getNodeMap().get(nodeId).getApplicationQueue().take();
						processMessage(message);
					}catch(Exception e){
					}
				}
			}


		}
	}

	private synchronized void processMessage(String message) {

		String msg[] = message.split(",");
		String msgType = msg[0];
		int srcNode = Integer.parseInt(msg[1].trim());
		int destNode = Integer.parseInt(msg[2].trim());

		if(Common.getNodeMap().get(nodeId).getTotalMessagesSent() >= 100 && msgType.equalsIgnoreCase("application")){
			System.out.println(nodeId+" message max value reached");
			System.exit(0);
		}
		else if(msgType.equalsIgnoreCase("application") && Common.getNodeMap().get(nodeId).getTotalMessagesSent() < 100 
				&& !Common.getNodeMap().get(nodeId).getIsFailed() && Common.getNodeMap().get(nodeId).getDiscardedpList().size() <= 0){
			//System.out.println(node.getNodeId()+" was passive");
			Common.getNodeMap().get(nodeId).setIsActive(true);


			for(int selectedNeighbor : Common.getRandomNeighbors(nodeId)){
				String appMsg = "application"+","+nodeId+","+selectedNeighbor;
				new StartClient(appMsg);

				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			Common.getNodeMap().get(nodeId).setIsActive(false);
		}
		else if(msgType.equalsIgnoreCase("failure")){

			int failureId = Integer.parseInt(msg[3].trim());
			int failedNode = Integer.parseInt(msg[4].trim());
			//Common.getNodeMap().get(nodeId).setIsFailed(true);
			System.out.println(nodeId+" in process REB message RECEIVED "+message);
			if(srcNode != destNode && !Common.getNodeMap().get(nodeId).getFailedMsgList().contains(failureId)){
				Common.getNodeMap().get(nodeId).setInRecoveryMode(true);
				Common.getNodeMap().get(nodeId).getFailedMsgList().add(failureId);
				for(int neighbors : Common.getNodeMap().get(nodeId).getNeighbours()){
					if(neighbors != srcNode){

						String failureMessage = "failure"+","+nodeId+","+neighbors+","+failureId+","+failedNode;
						new StartClient(failureMessage);
					}
				}

				if(Common.getNodeMap().get(nodeId).getParent() == -1){

					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if(failedNode == nodeId){
						String stopMessage = "stop"+","+nodeId+","+nodeId+","+failedNode;
						new StartClient(stopMessage);
					}
					else{
						for(int childId : Common.getNodeMap().get(nodeId).getChildren()){
							String stopMessage = "stop"+","+nodeId+","+childId+","+failedNode;
							new StartClient(stopMessage);
						}
					}
				}
				lock.add(0);
				System.out.println(nodeId+" ******************stopped itself after receiving FAILURE");
			}
		}
	}
}
