import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class StartServer extends Thread{
	private int nodeId;
	private List<Integer> lock;

	public StartServer(int nodeId, List<Integer> lock1) {
		this.nodeId = nodeId;
		lock = lock1;
		start();
	}
	public void run()
	{
		createServerSocket();
	}

	private void createServerSocket(){	

		ServerSocket serverSocket = null;
		Socket connectedSocket = null;
		try {
			serverSocket = new ServerSocket(Common.getNodeMap().get(nodeId).getPortNumber());
			System.out.println("=============== SERVER "+Common.getNodeMap().get(nodeId).getNodeId()+" started =============");
			//Common.writeNodeStatusStr(node, "Server started");
			Common.updateServerStartMap(nodeId);
			ProcessREBMessage p = new ProcessREBMessage(nodeId, lock);
			p.start();
			ProcessRecovery r = new ProcessRecovery(nodeId, lock);
			r.start();
			while (true) {
				
				connectedSocket = serverSocket.accept();
				String hostName = connectedSocket.getInetAddress().getHostName();
				//if(Common.getNodeMap().get(nodeId).getConnectedClientSocketMap().get(hostName) != null)
					//connectedSocket = Common.getNodeMap().get(nodeId).getConnectedClientSocketMap().get(hostName);
					
				MessageReader obClient = new MessageReader(connectedSocket, nodeId, lock);
				obClient.start();
				
			}
		} catch (Exception e) {
			System.out.println(this.nodeId+" trying to start again");
			try {
				Thread.sleep(50);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			createServerSocket();
			
		}
	}

}