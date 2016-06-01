import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;


public class StartClient {

	String message = null;
	Socket socket = null;
	Node hostNode = null;
	Node srcNode = null;

	public StartClient(String message){
		this.message = message;
		startCommunication();
	}

	private void startCommunication() {

		String [] msg = message.split(",");
		hostNode = Common.getNodeMap().get(Integer.parseInt(msg[2].trim()));
		srcNode = Common.getNodeMap().get(Integer.parseInt(msg[1].trim()));
		int nodeId = srcNode.getNodeId();

			//Create a client socket and connect to server at 127.0.0.1 port 5000
			Socket clientSocket = null;
			try {
				clientSocket = new Socket(hostNode.getHostName(), hostNode.getPortNumber());
			} catch (UnknownHostException e2) {
				e2.printStackTrace();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
			try{
				PrintWriter writer = new PrintWriter(clientSocket.getOutputStream());
				writer.println(message);
				writer.flush();

				System.out.println("SENDING =======> "+message.toString());
				if(msg[0].equalsIgnoreCase("application")){
					srcNode.setTotalMessagesSent(srcNode.getTotalMessagesSent()+1);
					srcNode.getSentMessagesMap().get(hostNode.getNodeId()).add(message);
					srcNode.getSentAfterLastCP().add(message);
				}

			} catch (UnknownHostException e) {
				System.out.println("UNKNOWN HOST EXCEPTION, Node "+nodeId+ " is trying to connect Quorum "+hostNode.getNodeId());
				try {
					Thread.sleep(50);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				startCommunication();
			} catch (IOException e) {
				System.out.println("IO EXCEPTION, Node "+nodeId+ " is trying to connect Quorum "+hostNode.getNodeId());
				try {
					Thread.sleep(50);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				startCommunication();
		}
	}

}
