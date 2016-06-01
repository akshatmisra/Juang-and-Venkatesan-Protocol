import java.util.List;


public class JuangVenky extends Thread{

	private int nodeId;
	private List<Integer> lock = null;
	private int count = 0;

	public JuangVenky(int nodeId, List<Integer> lock){
		System.out.println(nodeId+" inside juang and venky");
		this.nodeId = nodeId;
		this.lock = lock;
	}

	public synchronized void run(){


		while( Common.getNodeMap().get(nodeId).getTotalMessagesSent() <= 100 ){
			if(!Common.getNodeMap().get(nodeId).getInRecoveryMode() && !Common.getNodeMap().get(nodeId).getIsFailed()){

				CheckPointingProtocol cp = new CheckPointingProtocol(nodeId);
				cp.takeCheckPoint();
			}
			
		}
	}
}
