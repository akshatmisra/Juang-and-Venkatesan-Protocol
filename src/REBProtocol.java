import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class REBProtocol{

	private static final int TIME_UNIT = 50;
	private static final double UNIT_DIFF = 0.25;
	private int nodeId;

	public REBProtocol(int nodeId, List<Integer> lock){
		this.nodeId = nodeId;
		initiate();
	}

	public void initiate() {

		if(ifAllNeihborsStarted()){
			System.out.println(nodeId+" Starting REB");
			for(int neighbor : Common.getRandomNeighbors(nodeId)){
				String message = "application"+","+nodeId+","+neighbor;
				new StartClient(message);
				try{
					Thread.sleep(10);
				}catch(InterruptedException e){
					e.printStackTrace();
				}

			}
			Common.getNodeMap().get(nodeId).setIsActive(false);

		}
	}


	private synchronized boolean ifAllNeihborsStarted(){
		boolean ifAllQuorumsStarted = false;
		for(int neighbor : Common.getNodeMap().get(nodeId).getNeighbours()){
			Node neighborNode = Common.getNode(neighbor);
			final Socket sock = new Socket();
			final int timeOut = (int)TimeUnit.SECONDS.toMillis(30);
			try {
				sock.connect(new InetSocketAddress(neighborNode.getHostName(), neighborNode.getPortNumber()), timeOut);
				ifAllQuorumsStarted = true;
			} catch (IOException e) {
				e.printStackTrace();
				ifAllQuorumsStarted = false;
				try {
					Thread.sleep(50);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				ifAllNeihborsStarted();
			}
		}
		return ifAllQuorumsStarted;
	}
}
