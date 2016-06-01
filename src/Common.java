import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.auth.login.CredentialException;

public class Common {

	private static ConcurrentHashMap<Integer, Node> nodeMap = null;
	private static PrintWriter logWriter = null;

	private static int totalNodes = 0;
	private static int noOfFailureEvents = 0;
	private static int maxNumber = 0;
	private static int maxPerActive = 0;
	private static int minSendDelay = 0;
	private static List<String> failureList = null;
	private static ConcurrentHashMap<Integer, Boolean> serverStartMap = new ConcurrentHashMap<Integer, Boolean>();
	private static int rootNode;

	public static ConcurrentHashMap<Integer, Node>  initialize(String fileName) {

		nodeMap = new ConcurrentHashMap<Integer, Node>();
		try {
			FileReader fileReader = new FileReader(fileName);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			List<String> lines = new ArrayList<String>();

			try {
				String line;
				while((line = bufferedReader.readLine())!= null){
					lines.add(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			String [] firstLine = lines.get(0).split(" ");
			totalNodes = Integer.parseInt(firstLine[0].trim());
			noOfFailureEvents = Integer.parseInt(firstLine[1].trim());
			maxNumber = Integer.parseInt(firstLine[2].trim());
			maxPerActive = Integer.parseInt(firstLine[3].trim());
			minSendDelay = Integer.parseInt(firstLine[4].trim());

			for(int i=0 ; i<totalNodes ; i++){

				//node details
				String nodeDetails = lines.get(i+1);
				String [] nodeAttr = nodeDetails.split(" ");
				Node node = new Node(Integer.parseInt(nodeAttr[0].trim()));
				node.setHostName(nodeAttr[1].trim());
				node.setPortNumber(Integer.parseInt(nodeAttr[2].trim()));
				serverStartMap.put(node.getNodeId(), false);

				//neighbor details
				String neighborDetails = lines.get(totalNodes+1+i);
				List<Integer> neighbors = new ArrayList<Integer>();
				ConcurrentHashMap<Integer, Vector<String>> sentMessagesMap = new ConcurrentHashMap<Integer, Vector<String>>();
				ConcurrentHashMap<Integer, Vector<String>> receivedMessagesMap = new ConcurrentHashMap<Integer, Vector<String>>();
				for(String s : neighborDetails.split(" ")){

					neighbors.add(Integer.parseInt(s.trim()));

					Vector<String> msgSentVector = new Vector<String>();
					sentMessagesMap.put(Integer.parseInt(s.trim()), msgSentVector);

					Vector<String> msgReceivedVector = new Vector<String>();
					receivedMessagesMap.put(Integer.parseInt(s.trim()), msgReceivedVector);					
				}
				node.setNeighbours(neighbors);
				node.setSentMessagesMap(sentMessagesMap);
				node.setReceivedMessagesMap(receivedMessagesMap);
				node.setDiscardedpList(new ConcurrentHashMap<Integer, Vector<Object>>());
				nodeMap.put(node.getNodeId(),node);
			}
			setNodeMap(nodeMap);
			failureList = new ArrayList<String>();
			for(int i = 2*totalNodes +1 ; i< lines.size()- 1 ; i++){

				failureList.add(lines.get(i));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		rootNode = nodeMap.keys().nextElement();
		createSpanningTree(rootNode);
		for(int i : nodeMap.keySet()){
			Vector<String> itsOwnSendVector = new Vector<String>();
			nodeMap.get(i).getSentMessagesMap().put(i, itsOwnSendVector);
			Vector<String> itsOwnRcvVector = new Vector<String>();
			nodeMap.get(i).getReceivedMessagesMap().put(i, itsOwnRcvVector);
		}
		return nodeMap;


	}

	private static void initializeLog(int id){
		String logFileName = "./"+id + "_log.txt";

		try {
			logWriter = new PrintWriter(new FileWriter(new File(logFileName)));
			logWriter.println("Writing in file "+logFileName);
		} catch (IOException e) {
			System.out.println("Error: While opening log file : " + logFileName);
			System.out.println("Exiting application now !!");
		}
	}

	@SuppressWarnings("unchecked")
	public synchronized static boolean rollBack(Node node){

		if(node.getCpList().size() > 2){
			try{
				System.out.println("inside update rollback meth  "+node.getTotalMessagesSent()+" cp list size = "+node.getCpList().size());
				//System.out.println(" Discaded cp list ====== "+node.getDiscardedpList());
				//System.out.println("putting in dicrded list cp id = "+(node.getCpList().size()-1));
				//System.out.println(" the vector object is ===> "+node.getCpList().get(node.getCpList().size() - 1));
				//node.getDiscardedpList().put(node.getCpList().size()-1, node.getCpList().get(node.getCpList().size() - 1));
				Vector<Object> discardCP = node.getCpList().get(node.getCpList().size());
				node.getDiscardedpList().put(node.getCpList().size(), discardCP);
				System.out.println("***************************** DISCARDED CP IS **************************************");
				System.out.println(node.getNodeId()+" size =>  "+Common.getNodeMap().get(node.getNodeId()).getCpList().size()
						+" Total Sent=> "+(Integer)discardCP.get(1)+" Total Received=> "+(Integer)discardCP.get(2)
						+ " sent message map => "+(ConcurrentHashMap<Integer, Vector<Object>>)discardCP.get(3)
						+ " received message map => "+(ConcurrentHashMap<Integer, Vector<Object>>)discardCP.get(4));
				System.out.println("=======================================================================================\n\n");
				node.getCpList().remove(node.getCpList().size());
				System.out.println(" node cp list size after removing "+node.getCpList().size());
				Vector<Object> cp = node.getCpList().get(node.getCpList().size());
				System.out.println("***************************** ROLLBACK CHECKPOINT DETAILS **************************************");
				System.out.println(node.getNodeId()+" size =>  "+Common.getNodeMap().get(node.getNodeId()).getCpList().size()
						+" Total Sent=> "+(Integer)cp.get(1)+" Total Received=> "+(Integer)cp.get(2)
						+ " sent message map => "+(ConcurrentHashMap<Integer, Vector<Object>>)cp.get(3)
						+ " received message map => "+(ConcurrentHashMap<Integer, Vector<Object>>)cp.get(4));
				System.out.println("=======================================================================================");
				node.setIsActive(((Boolean) cp.get(0)).booleanValue());
				node.setTotalMessagesSent((Integer)cp.get(1));
				node.setTotalMessagesReceived((Integer)cp.get(2));
				node.setSentMessagesMap((ConcurrentHashMap<Integer, Vector<String>>) cp.get(3));
				node.setReceivedMessagesMap((ConcurrentHashMap<Integer, Vector<String>>) cp.get(4));
				node.setReceivedAfterLastCP((List<String>) cp.get(5));
				node.setSentAfterLastCP((List<String>) cp.get(6));
				Common.updateNode(node.getNodeId(), node);
				System.out.println(" after Roll back, CP list size in node is "+node.getCpList().size());
			}catch(Exception e){
				System.out.println("Problem in rolling back "+e.getMessage());
				e.printStackTrace();
			}
			return true;
		}
		return false;
	}

	public synchronized static void updateServerStartMap(int nodeId){
		System.out.println(nodeId+" updating to true");
		Common.serverStartMap.put(nodeId, true);
	}

	public synchronized static Boolean allServerStarted(){
		for(int id : Common.serverStartMap.keySet()){
			if(!serverStartMap.get(id)){
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				allServerStarted();
			}
			else
				return true;
		}
		return true;
	}
	public static void setNodeMap(ConcurrentHashMap<Integer, Node> nodeMap){
		Common.nodeMap = nodeMap;
	}
	public synchronized static ConcurrentHashMap<Integer, Node> getNodeMap(){
		return Common.nodeMap;
	}
	public static int getNoOfNodes(){
		return totalNodes;
	}
	public static int getNoOfFailureEvents(){
		return noOfFailureEvents;
	}
	public static int getMaxNumber(){
		return maxNumber;
	}
	public static int getMaxPerActive(){
		return maxPerActive;
	}
	public static int getMinSendDelay(){
		return minSendDelay;
	}
	public static List<String> getFailureList(){
		return failureList;
	}

	public synchronized static Node getNode(int id){
		for(int n : nodeMap.keySet()){
			if(n == id)
				return nodeMap.get(n);
		}
		return null;
	}
	public static void updateNode(int nodeId, Node node){
		Common.nodeMap.put(nodeId, node);
	}
	public static Node getNode(String host){
		for(int n : nodeMap.keySet()){
			if(Common.getNode(n).getHostName() == host)
				return nodeMap.get(n);
		}
		return null;
	}

	public synchronized static Set<Integer> getRandomNeighbors(int nodeId){
		Random randomGenerator = new Random();
		int randomMax = Common.getMaxPerActive() > Common.getNodeMap().get(nodeId).getNeighbours().size() 
		? Common.getNodeMap().get(nodeId).getNeighbours().size() : Common.getMaxPerActive();
		int random = randomGenerator.nextInt(randomMax);
		if(random == 0)
			random = 1;
		Set<Integer> randomNeighbors = new HashSet<Integer>();

		while(random > 0){
			int neighbor = Common.getNodeMap().get(nodeId).getNeighbours().get(new Random().nextInt(Common.getNodeMap().get(nodeId).getNeighbours().size()));
			if(!randomNeighbors.contains(neighbor)){
				randomNeighbors.add(neighbor);
				random --;
			}
			else
				continue;
		}
		//System.out.println("For node "+node.getNodeId()+" ===> random number selected is "+temp+" and neighbors are "+randomNeighbors);
		return randomNeighbors;
	}

	public static synchronized void writeNodeStatusStr(Node node, String newMessage){

		String file_name = "./"+node.getNodeId()+"_log.txt";


		Writer writer;	
		//File fout=new File("./",file_name);
		//System.out.println("Creating file:"+fout.getPath());
		//try{if (!fout.exists())	if(fout.createNewFile()) System.out.println("file Created");else System.out.println("problem creating file");}catch(IOException ioe){ioe.printStackTrace();System.out.println("Problem creating file");}

		try{
			FileOutputStream FoutStream=new FileOutputStream(file_name, true);

			java.nio.channels.FileLock lock = FoutStream.getChannel().lock();
			try{
				synchronized(lock)
				{
					Thread.sleep(20);
					writer = new BufferedWriter(new OutputStreamWriter(FoutStream, "UTF-8"));
					writer.append(newMessage);
					writer.append("\n");
					writer.append("\n");
					writer.close();	
				}
			}catch(IOException ioe){ioe.printStackTrace();}finally {lock.release();FoutStream.close();}
		}catch(Exception e){e.printStackTrace();}
	}

	public static synchronized void logCriticalSection(String message)
	{
		String file_name = "Critical_Section.txt";    
		File fout=new File("./",file_name);
		try{if (!fout.exists())        if(fout.createNewFile()) System.out.println("file Created");else System.out.println("problem creating file");}catch(IOException ioe){ioe.printStackTrace();System.out.println("Problem creating file");}
		Writer writer;
		// Calendar cal=Calendar.getInstance();
		//Write ENTER
		try{
			FileOutputStream FoutStream=new FileOutputStream(file_name, true);
			try{
				writer = new BufferedWriter(new OutputStreamWriter(FoutStream, "UTF-8"));
				writer.append(message);
				writer.append("\n");
				writer.close();        
			}catch(IOException ioe){
				ioe.printStackTrace();
			}
			finally {
				FoutStream.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		//Release file
		try { 
			Thread.sleep(10);
		} catch (InterruptedException ie) {ie.printStackTrace();}
		//Release File
	}

	// Creating Spanning Tree
	public static void createSpanningTree(int node)
	{	
		for(int neighbor : nodeMap.get(node).getNeighbours()){
			if(getNode(neighbor).getParent() == -1 && neighbor != rootNode){
				Node n = nodeMap.get(neighbor);
				n.setParent(node);
				nodeMap.get(node).getChildren().add(n.getNodeId());
			}
		}
		for(int n1 : nodeMap.get(node).getChildren()){
			createSpanningTree(n1);
		}
	}
}
