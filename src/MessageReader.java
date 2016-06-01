import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.List;


public class MessageReader extends Thread {
	private Socket clientSocket = null;
	private int serverNodeId;
	private List<Integer> lock = null;

	MessageReader(Socket clientSocket, int serverNodeId, List<Integer> lock1) throws Exception {
		this.clientSocket = clientSocket;
		this.serverNodeId = serverNodeId;
		lock = lock1;
		this.setName("READER "+serverNodeId);
	}

	public synchronized void run() {

		synchronized (new Object()) {


			try {  

				while(true){

					BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					//The method readLine is blocked until a message is received 
					String message = (String)reader.readLine();
					if(message != null){
						String [] msg = message.split(",");
						String msgType = msg[0];
						if(msgType.equalsIgnoreCase("resume"))
							System.out.println(" Message received is "+message);
						int srcNodeId = Integer.parseInt(msg[1].trim());
						int destNodeId = Integer.parseInt(msg[2].trim());
						if(msgType.equalsIgnoreCase("application") && srcNodeId != destNodeId
								&& !Common.getNodeMap().get(serverNodeId).getIsFailed()){
							Common.getNodeMap().get(serverNodeId).getApplicationQueue().add(message);
							Common.getNodeMap().get(serverNodeId).setTotalMessagesReceived(Common.getNodeMap().get(serverNodeId).getTotalMessagesReceived()+1);
							Common.getNodeMap().get(serverNodeId).getReceivedMessagesMap().get(Integer.parseInt(msg[1].trim())).add(message);
							Common.getNodeMap().get(serverNodeId).getReceivedAfterLastCP().add(message);
						}
						else if(msgType.equalsIgnoreCase("failure")){
							Common.getNodeMap().get(serverNodeId).getApplicationQueue().add(message);
						}
						else if((msgType.equalsIgnoreCase("stop") || msgType.equalsIgnoreCase("startrecovery")
								|| msgType.equalsIgnoreCase("recovery") || msgType.equalsIgnoreCase("rollback")
								|| msgType.equalsIgnoreCase("resume")) && srcNodeId != destNodeId)
							Common.getNodeMap().get(serverNodeId).getJuangVenkyQueue().add(message);
						else{
							System.out.println(" discard the message == > "+message);
							Common.getNodeMap().get(serverNodeId).getDiscardQueue().add(message);
						}
					}

				}

			} catch (IOException ex) {
				ex.printStackTrace();
				System.out.println(serverNodeId+" ......Server is starting......");
			}
		}
	}
}