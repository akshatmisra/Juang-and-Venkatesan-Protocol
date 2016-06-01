import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


public class Main {

	private static int nodeId;
	// lock object to stop or resume REB
	private static List<Integer> lock = new ArrayList<Integer>();
	
	public static void main(String[] args) {

		nodeId = Integer.parseInt(args[0].trim());
		Common.initialize(args[1]);
		new StartServer(nodeId, lock);
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Node n = Common.getNodeMap().get(nodeId);
		//initial CP
		Vector<Object> cpData = new Vector<Object>();
		cpData.add(n.getIsActive());
		cpData.add(n.getTotalMessagesSent());
		cpData.add(n.getTotalMessagesReceived());
		cpData.add(n.getSentMessagesMap());
		cpData.add(n.getReceivedMessagesMap());
		cpData.add(n.getReceivedAfterLastCP());
		cpData.add(n.getSentAfterLastCP());
		n.getCpList().put(1, cpData);	
		Common.getNodeMap().get(nodeId).setTempCPCount(n.getCpList().size());
		System.out.println(nodeId+" CHECKPOINT SIZE "+Common.getNodeMap().get(nodeId).getCpList().size()+" total messages sent "+n.getTotalMessagesSent());

		try {
			Thread.sleep(500);
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		

		//start juang and venkatesans protocol
		JuangVenky jv = new JuangVenky(nodeId, lock);
		jv.start();

		try {
			Thread.sleep(500);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		//start REB
		new REBProtocol(nodeId, lock);

	}

}
