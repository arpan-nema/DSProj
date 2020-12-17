package pb.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.xml.stream.events.EndDocument;

import jdk.jshell.execution.Util;
import pb.managers.PeerManager;
import pb.Server;
import pb.WhiteboardServer;
import pb.managers.*;
import pb.managers.endpoint.*;

import java.net.ServerSocket;
import java.net.UnknownHostException;
import pb.managers.endpoint.Endpoint;
import pb.utils.Utils;


/**
 * Initial code obtained from:
 * https://www.ssaurel.com/blog/learn-how-to-make-a-swing-painting-and-drawing-application/
 */
public class WhiteboardApp {
	private static Logger log = Logger.getLogger(WhiteboardApp.class.getName());
	
	/**
	 * Emitted to another peer to subscribe to updates for the given board. Argument
	 * must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String listenBoard = "BOARD_LISTEN";

	/**
	 * Emitted to another peer to unsubscribe to updates for the given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String unlistenBoard = "BOARD_UNLISTEN";

	/**
	 * Emitted to another peer to get the entire board data for a given board.
	 * Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String getBoardData = "GET_BOARD_DATA";

	/**
	 * Emitted to another peer to give the entire board data for a given board.
	 * Argument must have format "host:port:boardid%version%PATHS".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardData = "BOARD_DATA";

	/**
	 * Emitted to another peer to add a path to a board managed by that peer.
	 * Argument must have format "host:port:boardid%version%PATH". The numeric value
	 * of version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathUpdate = "BOARD_PATH_UPDATE";

	/**
	 * Emitted to another peer to indicate a new path has been accepted. Argument
	 * must have format "host:port:boardid%version%PATH". The numeric value of
	 * version must be equal to the version of the board without the PATH added,
	 * i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardPathAccepted = "BOARD_PATH_ACCEPTED";

	/**
	 * Emitted to another peer to remove the last path on a board managed by that
	 * peer. Argument must have format "host:port:boardid%version%". The numeric
	 * value of version must be equal to the version of the board without the undo
	 * applied, i.e. the current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoUpdate = "BOARD_UNDO_UPDATE";

	/**
	 * Emitted to another peer to indicate an undo has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the undo applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardUndoAccepted = "BOARD_UNDO_ACCEPTED";

	/**
	 * Emitted to another peer to clear a board managed by that peer. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearUpdate = "BOARD_CLEAR_UPDATE";

	/**
	 * Emitted to another peer to indicate an clear has been accepted. Argument must
	 * have format "host:port:boardid%version%". The numeric value of version must
	 * be equal to the version of the board without the clear applied, i.e. the
	 * current version of the board.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardClearAccepted = "BOARD_CLEAR_ACCEPTED";

	/**
	 * Emitted to another peer to indicate a board no longer exists and should be
	 * deleted. Argument must have format "host:port:boardid".
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardDeleted = "BOARD_DELETED";

	/**
	 * Emitted to another peer to indicate an error has occurred.
	 * <ul>
	 * <li>{@code args[0] instanceof String}</li>
	 * </ul>
	 */
	public static final String boardError = "BOARD_ERROR";
	
	/**
	 * White board map from board name to board object 
	 */
	Map<String,Whiteboard> whiteboards;
	
	/**
	 * The currently selected white board
	 */
	Whiteboard selectedBoard = null;
	
	/**
	 * The peer:port string of the peer. This is synonomous with IP:port, host:port,
	 * etc. where it may appear in comments.
	 */
	String peerport="standalone"; // a default value for the non-distributed version
	
	/*
	 * GUI objects, you probably don't need to modify these things... you don't
	 * need to modify these things... don't modify these things [LOTR reference?].
	 */
	
	JButton clearBtn, blackBtn, redBtn, createBoardBtn, deleteBoardBtn, undoBtn;
	JCheckBox sharedCheckbox ;
	DrawArea drawArea;
	JComboBox<String> boardComboBox;
	boolean modifyingComboBox=false;
	boolean modifyingCheckBox=false;
	PeerManager peerManager;
	String serverHost;
	int serverPort;
	Endpoint serverEndpoint;
	ClientManager serverManger;
	public static final Set<Endpoint> allConnectedPeers= new HashSet<>();

	public static final Map<String,Set<Endpoint>> boardListeningPeers=new HashMap<>();
	public volatile boolean waitToFinish=true;
	/**
	 * Initialize the white board app.
	 */
	private void addListeningPeers(String boardName,Endpoint ep)
	{
		if(!boardListeningPeers.containsKey(boardName))
		{
			boardListeningPeers.put(boardName,new HashSet<Endpoint>());
		}
		Set<Endpoint> endPoints=boardListeningPeers.get(boardName);
		if(!endPoints.contains(ep) && ep!=null)
		{
			endPoints.add(ep);
		}
	}
	private void removingListeningPeers(String boardName,Endpoint ep)
	{
		if(boardListeningPeers.containsKey(boardName))
		{
			Set<Endpoint> endPoints=boardListeningPeers.get(boardName);
			if(endPoints.contains(ep))
			{
				endPoints.remove(ep);
			}
		}
		
	}
	private void peerEmit(String host,int port,String data1,String eventName)
	{
		try
		{
			for(Endpoint e:allConnectedPeers)
			{
				if(e.getOtherEndpointId().substring(1).equals(host+":"+port))
				{
					e.emit(eventName,data1);
					return;
				}
			}
		}catch(Exception e)
		{}
		

	}

	public boolean alterBoardData(String data, String eventName)
	{
		boolean result=false;
		System.out.println("oooooooooooooooo Arpan recieved updates ooooooooooo");
		System.out.println(data);
		
		//172.16.4.215:51005:board1604618807489
		//
		long incomingVersion=getBoardVersion(data);
		String incomingBoardName=getBoardName(data);
		--incomingVersion;
		if(this.whiteboards.containsKey(incomingBoardName))
		{
			System.out.println("ppppppppppppppppp");
			Whiteboard board=this.whiteboards.get(incomingBoardName);
			System.out.println(incomingVersion+":"+board.getVersion());
			System.out.println("Version of present->"+board.getVersion());
			if(eventName.equals(boardPathUpdate))
			{
				WhiteboardPath newPath=new WhiteboardPath(getBoardPaths(data));
				boolean status =board.addPath(newPath,incomingVersion);
				drawSelectedWhiteboard();
				System.out.println(board.getNameAndVersion()+":"+status);
				result=status;
				if(!status && board.isRemote())
				{
					peerEmit(getIP(data), getPort(data), board.getNameAndVersion(), getBoardData);
					//ep.emit(getBoardData, board.getNameAndVersion());
				}
			}else if(eventName.equals(boardUndoUpdate))
			{
				boolean status=board.undo(incomingVersion);
				drawSelectedWhiteboard();
				result=status;
				if(!status && board.isRemote())
				{
					peerEmit(getIP(data), getPort(data), board.getNameAndVersion(), getBoardData);
					//ep.emit(getBoardData, board.getNameAndVersion());
				}
			}else if(eventName.equals(boardClearUpdate))
			{
				boolean status=board.clear(incomingVersion);
				drawSelectedWhiteboard();
				result=status;
				if(!status && board.isRemote())
				{
					peerEmit(getIP(data), getPort(data), board.getNameAndVersion(), getBoardData);
				}
			}

		}
		return result;
					
	}

	public WhiteboardApp(int peerPort,String whiteboardServerHost, 
			int whiteboardServerPort) {
		serverHost=whiteboardServerHost;
		serverPort=whiteboardServerPort;
		whiteboards=new HashMap<>();
		try
		{
			ServerSocket s = new ServerSocket(0);
			System.out.println("listening on port: " + s.getLocalPort());
			peerPort=s.getLocalPort();
			s.close();

			System.out.println("==========="+peerPort+"===========");
		}catch(Exception e)
		{

		}
		
		try
		{
			peerManager=new PeerManager(peerPort);
			peerManager.on(PeerManager.peerServerManager,(arg)->{
				ServerManager sm=(ServerManager)arg[0];
				sm.on(IOThread.ioThread,(arg1)->{
					System.out.println("=============="+arg1[0]+"==========");
					this.peerport=(String)arg1[0];
				});
			}).on(PeerManager.peerStarted,(arg)->{
				Endpoint ep=(Endpoint)arg[0];
				allConnectedPeers.add(ep);
				ep.on(getBoardData,(eventArgs2)->{
					String data=(String)eventArgs2[0];
					String fullName=getBoardName(data);
					System.out.println("========= getBoardData recieved"+fullName+"========");
					for(String k: whiteboards.keySet())
					{
						System.out.println(k);
					}
					if(whiteboards.containsKey(fullName))
					{
						Whiteboard board=whiteboards.get(fullName);
						String boardIdAnddata=board.toString();
						System.out.println("============Sending board data ============");
						System.out.println(boardIdAnddata);
						
						ep.emit(listenBoard, fullName);
						ep.emit(boardData, boardIdAnddata);
						System.out.println("xxxxxxxxxxxxPeer Port:"+this.peerport);
						
						for(String s:boardListeningPeers.keySet())
						{
							System.out.println("=================="+s+"===============");
							for(Endpoint e:boardListeningPeers.get(s))
							{
								System.out.println(e.getOtherEndpointId());
							}
							System.out.println("=================Ended===============");
						}
					}
				}).on(listenBoard,(eventArgs2)->{
					String boardData=(String)eventArgs2[0];
					//Someone wants to get board update
					addListeningPeers(getBoardName(boardData), ep);
					
									
				}).on(unlistenBoard,(eventArgs2)->{
					String boardData=(String)eventArgs2[0];
					//Someone wants to stop getting board updates
					removingListeningPeers(getBoardName(boardData), ep);
				}).on(boardPathUpdate,(eventArgs3)->{
					String data=(String)eventArgs3[0];
					System.out.println("oooooooooooooooo Arpan recieved updates ooooooooooo");
					boolean success=alterBoardData(data,boardPathUpdate);
					String boardName=getBoardName(data);
					Whiteboard board= whiteboards.get(boardName);
					if(!board.isRemote() && board.isShared() && success)
					{
						Set<Endpoint> listeningPeers=boardListeningPeers.get(boardName);
						for(Endpoint end:listeningPeers)
						{
							if(!ep.getOtherEndpointId().equals(end.getOtherEndpointId()))
							{
								end.emit(boardPathUpdate, data);
							}
						}
						ep.emit(boardPathAccepted,board.toString());
					}
					
					
				}).on(boardPathAccepted,(eventArgs3)->{
					String data=(String)eventArgs3[0];
					String boardName=getBoardName(data);
					if(this.whiteboards.containsKey(boardName))
					{
						Whiteboard board=this.whiteboards.get(boardName);
						board.whiteboardFromString(boardName,getBoardData(data));
						// Do something to change the version of current board.

					}
					
				}).on(boardUndoUpdate,(eventArgs3)->{
					String data=(String)eventArgs3[0];
					boolean success=alterBoardData(data,boardUndoUpdate);
					String boardName=getBoardName(data);
					Whiteboard board= whiteboards.get(boardName);
					if(!board.isRemote() && board.isShared() && success)
					{
						Set<Endpoint> listeningPeers=boardListeningPeers.get(boardName);
						for(Endpoint end:listeningPeers)
						{
							if(!ep.getOtherEndpointId().equals(end.getOtherEndpointId()))
							{
								end.emit(boardUndoUpdate, data);
							}
						}
						ep.emit(boardUndoAccepted,board.toString());
					}
					
					
				}).on(boardUndoAccepted,(eventArgs3)->{
					String data=(String)eventArgs3[0];
					
					String boardName=getBoardName(data);
					if(this.whiteboards.containsKey(boardName))
					{
						Whiteboard board=this.whiteboards.get(boardName);
						board.whiteboardFromString(boardName,getBoardData(data));
						// Do something to change the version of current board.
					}
					
				}).on(boardClearUpdate,(eventArgs3)->{
					String data=(String)eventArgs3[0];
					boolean success= alterBoardData(data,boardClearUpdate);
					String boardName=getBoardName(data);
					Whiteboard board= whiteboards.get(boardName);
					if(!board.isRemote() && board.isShared() && success)
					{
						Set<Endpoint> listeningPeers=boardListeningPeers.get(boardName);
						for(Endpoint end:listeningPeers)
						{
							if(!ep.getOtherEndpointId().equals(end.getOtherEndpointId()))
							{
								end.emit(boardClearUpdate, data);
							}
						}
						ep.emit(boardClearAccepted,board.toString());
					}
					
				}).on(boardClearAccepted,(eventArgs3)->{
					String data=(String)eventArgs3[0];
					String boardName=getBoardName(data);
					if(this.whiteboards.containsKey(boardName))
					{
						Whiteboard board=this.whiteboards.get(boardName);
						board.whiteboardFromString(boardName,getBoardData(data));
						// Do something to change the version of current board.
					}
				}).on(boardDeleted,(eventArgs2)->{
					String data=(String)eventArgs2[0];
					String boardName=getBoardName(data);
					if(this.whiteboards.containsKey(boardName))
					{
						Whiteboard board= this.whiteboards.get(boardName);
						if(board.isRemote())
						{
							deleteBoard(boardName);
						}
					}
				});
			}).on(PeerManager.peerError,(arg)->{
				System.out.println("=============="+arg[0]+"==========");
				Endpoint endpoint=(Endpoint)arg[0];
				if(allConnectedPeers.contains(endpoint))
				{
					allConnectedPeers.remove(endpoint);
				}
				for(String key:boardListeningPeers.keySet())
				{
					for(Endpoint e:boardListeningPeers.get(key))
					{
						if(e.getOtherEndpointId().equals(endpoint.getOtherEndpointId()))
						{
							removingListeningPeers(key, e);
							break;
						}
					}
				}
				
			}).on(PeerManager.peerStopped,(args)->{
				Endpoint endpoint = (Endpoint)args[0];
				if(allConnectedPeers.contains(endpoint))
				{
					allConnectedPeers.remove(endpoint);
				}
				for(String key:boardListeningPeers.keySet())
				{
					for(Endpoint e:boardListeningPeers.get(key))
					{
						if(e.getOtherEndpointId().equals(endpoint.getOtherEndpointId()))
						{
							removingListeningPeers(key, e);
							break;
						}
					}
				}
				System.out.println("Disconnected from peer: "+endpoint.getOtherEndpointId());
			});
			peerManager.start();
			
			serverManger=peerManager.connect(serverPort, serverHost);
			serverManger.on(ClientManager.sessionStarted,(args)->{
				serverEndpoint=(Endpoint)args[0];
				serverEndpoint.on(WhiteboardServer.sharingBoard,(eventArgs)->{
					try
					{
						String peerInfo=(String)eventArgs[0];
						String name=getBoardName(peerInfo);
						if(!whiteboards.containsKey(name) )
						{
							System.out.println("========= SharingBoard "+peerInfo+"=========");
							ClientManager peerClient=peerManager.connect(getPort(peerInfo), getIP(peerInfo));
							peerClient.on(PeerManager.peerStarted,(eventArgs1)->{
								System.out.println("=============="+"Peer Session Started"+"==========");
								Endpoint ep=(Endpoint)eventArgs1[0];
								allConnectedPeers.add(ep);
								ep.on(boardData,(eventArgs2)->{
									System.out.println("===============Arpan get board data===============");
									System.out.println(eventArgs2[0]);
									String boardInfo=(String)eventArgs2[0];
									Whiteboard board=new Whiteboard(getBoardName(boardInfo),true);
									if(whiteboards.containsKey(getBoardName(boardInfo)))
									{
										board=whiteboards.get(getBoardName(boardInfo));
										board.whiteboardFromString(getBoardName(boardInfo), getBoardData(boardInfo));
										drawSelectedWhiteboard();
									}else
									{
										board.whiteboardFromString(getBoardName(boardInfo), getBoardData(boardInfo));
										this.addBoard(board, true);
									}
									
									
									System.out.println("========"+board.getNameAndVersion());
									
								}).on(boardPathUpdate,(newArgs)->{
									System.out.println("ttttttttttt Data reached tttttttt");
									System.out.println(newArgs[0]);
									String boardString=(String)newArgs[0];
									alterBoardData(boardString,boardPathUpdate);

								}).on(boardUndoUpdate,(eventArgs3)->{
									String data=(String)eventArgs3[0];
									alterBoardData(data,boardUndoUpdate);
									
								}).on(boardClearUpdate,(eventArgs3)->{
									String data=(String)eventArgs3[0];
									alterBoardData(data,boardClearUpdate);
								}).on(boardUndoAccepted,(eventArgs3)->{
									String data=(String)eventArgs3[0];
									
									String boardName=getBoardName(data);
									if(this.whiteboards.containsKey(boardName))
									{
										Whiteboard board=this.whiteboards.get(boardName);
										board.whiteboardFromString(boardName,getBoardData(data));
										// Do something to change the version of current board.
									}
									
								}).on(boardClearAccepted,(eventArgs3)->{
									String data=(String)eventArgs3[0];
									
									String boardName=getBoardName(data);
									if(this.whiteboards.containsKey(boardName))
									{
										Whiteboard board=this.whiteboards.get(boardName);
										board.whiteboardFromString(boardName,getBoardData(data));
										// Do something to change the version of current board.
									}
									
								}).on(boardPathAccepted,(eventArgs3)->{
									String data=(String)eventArgs3[0];
									
									String boardName=getBoardName(data);
									if(this.whiteboards.containsKey(boardName))
									{
										Whiteboard board=this.whiteboards.get(boardName);
										board.whiteboardFromString(boardName,getBoardData(data));
										// Do something to change the version of current board.
									}
									
								}).on(boardDeleted,(eventArgs2)->{
									String data=(String)eventArgs2[0];
									String boardName=getBoardName(data);
									if(this.whiteboards.containsKey(boardName))
									{
										Whiteboard board= this.whiteboards.get(boardName);
										if(board.isRemote())
										{
											deleteBoard(boardName);
											
										}
									}
								});

								System.out.println("========Emitting getboardData Event"+ep.getOtherEndpointId()+"============");
								
								ep.emit(getBoardData,peerInfo);
								System.out.println("xxxxxxxxxxxPeer Port:"+this.peerport);
								
								for(String s:boardListeningPeers.keySet())
								{
									System.out.println("==================Arpan Peer List"+s+"===============");
									for(Endpoint e:boardListeningPeers.get(s))
									{
										System.out.println(e.getOtherEndpointId());
									}
									System.out.println("=================Ended===============");
								}
							});
							peerClient.start();
							
							
						}
						System.out.println("========NOT Emitting getboardData Event============");
						
						
						
					}catch(Exception e)
					{

					}
					

				}).on(WhiteboardServer.unsharingBoard,(newArgs)->{
					String boardData=(String)newArgs[0];
					System.out.println("===========Unshareing========"+boardData+"===========");
					if(whiteboards.containsKey(getBoardName(boardData)))
					{
						System.out.println("===========Board Present========"+boardData+"===========");
						Whiteboard board=whiteboards.get(getBoardName(boardData));
						if(board.isRemote())
						{
							deleteBoard(getBoardName(boardData));
						}
						
					}
				});
			});
			serverManger.start();
		}catch(Exception e)
		{
			
		}
		show(this.peerport);
		
		
	}
	
	/******
	 * 
	 * Utility methods to extract fields from argument strings.
	 * 
	 ******/
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer:port:boardid
	 */
	public static String getBoardName(String data) {
		String[] parts=data.split("%",2);
		return parts[0];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return boardid%version%PATHS
	 */
	public static String getBoardIdAndData(String data) {
		String[] parts=data.split(":");
		return parts[2];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version%PATHS
	 */
	public static String getBoardData(String data) {
		String[] parts=data.split("%",2);
		return parts[1];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return version
	 */
	public static long getBoardVersion(String data) {
		String[] parts=data.split("%",3);
		return Long.parseLong(parts[1]);
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return PATHS
	 */
	public static String getBoardPaths(String data) {
		String[] parts=data.split("%",3);
		return parts[2];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return peer
	 */
	public static String getIP(String data) {
		String[] parts=data.split(":");
		return parts[0];
	}
	
	/**
	 * 
	 * @param data = peer:port:boardid%version%PATHS
	 * @return port
	 */
	public static int getPort(String data) {
		String[] parts=data.split(":");
		return Integer.parseInt(parts[1]);
	}
	
	/******
	 * 
	 * Methods called from events.
	 * 
	 ******/
	
	// From whiteboard server
	
	
	// From whiteboard peer
	
	
	
	/******
	 * 
	 * Methods to manipulate data locally. Distributed systems related code has been
	 * cut from these methods.
	 * 
	 ******/
	
	/**
	 * Wait for the peer manager to finish all threads.
	 */
	public void waitToFinish() {
		while(waitToFinish)
		{
			try
			{
				Utils.getInstance().wait(2000);
			}catch(Exception e)
			{

			}
			
		}
		
	}
	
	/**
	 * Add a board to the list that the user can select from. If select is
	 * true then also select this board.
	 * @param whiteboard
	 * @param select
	 */
	public void addBoard(Whiteboard whiteboard,boolean select) {
		synchronized(whiteboards) {
			whiteboards.put(whiteboard.getName(), whiteboard);
		}
		updateComboBox(select?whiteboard.getName():null);
	}
	
	/**
	 * Delete a board from the list.
	 * @param boardname must have the form peer:port:boardid
	 */
	public void deleteBoard(String boardname) {
		synchronized(whiteboards) {
			Whiteboard whiteboard = whiteboards.get(boardname);
			if(whiteboard!=null) {
				if(whiteboard.isRemote())
				{
					String host=getIP(boardname);
					int port=getPort(boardname);
					peerEmit(host, port, boardname, unlistenBoard);
					
				}else if(whiteboard.isShared())
				{
					Set<Endpoint> allEndpoints=boardListeningPeers.get(boardname);
					for(Endpoint ep:allEndpoints)
					{
						ep.emit(boardDeleted, whiteboard.getNameAndVersion());

					}
					serverEndpoint.emit(WhiteboardServer.unshareBoard, boardname);
					boardListeningPeers.remove(boardname);
					
				}
				//deleteBoard(boardname);
				whiteboards.remove(boardname);
			}
		}
		updateComboBox(null);
	}
	
	/**
	 * Create a new local board with name peer:port:boardid.
	 * The boardid includes the time stamp that the board was created at.
	 */
	public void createBoard() {
		String name = peerport+":board"+Instant.now().toEpochMilli();
		Whiteboard whiteboard = new Whiteboard(name,false);
		addBoard(whiteboard,true);
	}
	
	/**
	 * Add a path to the selected board. The path has already
	 * been drawn on the draw area; so if it can't be accepted then
	 * the board needs to be redrawn without it.
	 * @param currentPath
	 */

	public void pathCreatedLocally(WhiteboardPath currentPath) {
		if(selectedBoard!=null) {
			if(!selectedBoard.addPath(currentPath,selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard(); // just redraw the screen without the path
			} else {
				// was accepted locally, so do remote stuff if needed
				
				drawSelectedWhiteboard();	
				if(selectedBoard.isRemote())
				{
					String boardname=selectedBoard.getName();
					String host=getIP(boardname);
					int port=getPort(boardname);
					peerEmit(host, port, selectedBoard.getNameAndVersion()+"%"+currentPath.toString(), boardPathUpdate);
					
				}else if(selectedBoard.isShared())
				{
					if(boardListeningPeers.containsKey(selectedBoard.getName()))
					{
						Set<Endpoint> allEndpoints=boardListeningPeers.get(selectedBoard.getName());
						for (Endpoint ep:allEndpoints)
						{
							ep.emit(boardPathUpdate, selectedBoard.getNameAndVersion()+"%"+currentPath.toString());
							//ep.emit(boardPathUpdate, selectedBoard.getNameAndVersion()+"%"+currentPath.toString());
						}
					}
					
				}
				
				
				
			}
		} else {
			log.severe("path created without a selected board: "+currentPath);
		}
	}
	
	/**
	 * Clear the selected whiteboard.
	 */
	public void clearedLocally() {
		if(selectedBoard!=null) {
			if(!selectedBoard.clear(selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
			} else {
				// was accepted locally, so do remote stuff if needed
				if(selectedBoard.isRemote())
				{
					String boardname=selectedBoard.getName();
					String host=getIP(boardname);
					int port=getPort(boardname);
					peerEmit(host, port, selectedBoard.getNameAndVersion(), boardClearUpdate);
					
					
				}else if(selectedBoard.isShared())
				{
					if(boardListeningPeers.containsKey(selectedBoard.getName()))
					{
						Set<Endpoint> allendPoints=boardListeningPeers.get(selectedBoard.getName());
						for(Endpoint ep:allendPoints)
						{
							ep.emit(boardClearUpdate, selectedBoard.getNameAndVersion());
						}
					}
				}
				drawSelectedWhiteboard();
			}
		} else {
			log.severe("cleared without a selected board");
		}
	}
	
	/**
	 * Undo the last path of the selected whiteboard.
	 */
	public void undoLocally() {
		if(selectedBoard!=null) {
			if(!selectedBoard.undo(selectedBoard.getVersion())) {
				// some other peer modified the board in between
				drawSelectedWhiteboard();
			} else {
				if(selectedBoard.isRemote())
				{
					String boardname=selectedBoard.getName();
					String host=getIP(boardname);
					int port=getPort(boardname);
					peerEmit(host, port, selectedBoard.getNameAndVersion(), boardUndoUpdate);
					
				}else if(selectedBoard.isShared())
				{
					if(boardListeningPeers.containsKey(selectedBoard.getName()))
					{
						Set<Endpoint> allendPoints=boardListeningPeers.get(selectedBoard.getName());
						for(Endpoint ep:allendPoints)
						{
							ep.emit(boardUndoUpdate, selectedBoard.getNameAndVersion());
						}
					}
				}
				drawSelectedWhiteboard();
			}
		} else {
			log.severe("undo without a selected board");
		}
	}
	
	/**
	 * The variable selectedBoard has been set.
	 */
	public void selectedABoard() {
		drawSelectedWhiteboard();
		log.info("selected board: "+selectedBoard.getName());
	}
	
	/**
	 * Set the share status on the selected board.
	 */
	public void setShare(boolean share) {
		if(selectedBoard!=null) {
			selectedBoard.setShared(share);
			if(share)
			{
				serverEndpoint.emit(WhiteboardServer.shareBoard,selectedBoard.getName());
				addListeningPeers(selectedBoard.getName(),null);
				
			}else
			{
			
				serverEndpoint.emit(WhiteboardServer.unshareBoard, selectedBoard.getName());
				boardListeningPeers.remove(selectedBoard.getName());
			}
        } else {
        	log.severe("there is no selected board");
        }
	}
	
	/**
	 * Called by the gui when the user closes the app.
	 */
	public void guiShutdown() {
		// do some final cleanup
		HashSet<Whiteboard> existingBoards= new HashSet<>(whiteboards.values());
		existingBoards.forEach((board)->{
			deleteBoard(board.getName());
		});
    	whiteboards.values().forEach((whiteboard)->{
			
			
		});
		try
		{
			//serverManger.shutdown();
			peerManager.shutdown();
			waitToFinish=false;

		}catch(Exception ex)
		{

		}
		
	}
	
	

	/******
	 * 
	 * GUI methods and callbacks from GUI for user actions.
	 * You probably do not need to modify anything below here.
	 * 
	 ******/
	
	/**
	 * Redraw the screen with the selected board
	 */
	public void drawSelectedWhiteboard() {
		drawArea.clear();
		if(selectedBoard!=null) {
			selectedBoard.draw(drawArea);
		}
	}
	
	/**
	 * Setup the Swing components and start the Swing thread, given the
	 * peer's specific information, i.e. peer:port string.
	 */
	public void show(String peerport) {
		// create main frame
		JFrame frame = new JFrame("Whiteboard Peer: "+peerport);
		Container content = frame.getContentPane();
		// set layout on content pane
		content.setLayout(new BorderLayout());
		// create draw area
		drawArea = new DrawArea(this);

		// add to content pane
		content.add(drawArea, BorderLayout.CENTER);

		// create controls to apply colors and call clear feature
		JPanel controls = new JPanel();
		controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

		/**
		 * Action listener is called by the GUI thread.
		 */
		ActionListener actionListener = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == clearBtn) {
					clearedLocally();
				} else if (e.getSource() == blackBtn) {
					drawArea.setColor(Color.black);
				} else if (e.getSource() == redBtn) {
					drawArea.setColor(Color.red);
				} else if (e.getSource() == boardComboBox) {
					if(modifyingComboBox) return;
					if(boardComboBox.getSelectedIndex()==-1) return;
					String selectedBoardName=(String) boardComboBox.getSelectedItem();
					if(whiteboards.get(selectedBoardName)==null) {
						log.severe("selected a board that does not exist: "+selectedBoardName);
						return;
					}
					selectedBoard = whiteboards.get(selectedBoardName);
					// remote boards can't have their shared status modified
					if(selectedBoard.isRemote()) {
						sharedCheckbox.setEnabled(false);
						sharedCheckbox.setVisible(false);
					} else {
						modifyingCheckBox=true;
						sharedCheckbox.setSelected(selectedBoard.isShared());
						modifyingCheckBox=false;
						sharedCheckbox.setEnabled(true);
						sharedCheckbox.setVisible(true);
					}
					selectedABoard();
				} else if (e.getSource() == createBoardBtn) {
					createBoard();
				} else if (e.getSource() == undoBtn) {
					if(selectedBoard==null) {
						log.severe("there is no selected board to undo");
						return;
					}
					undoLocally();
				} else if (e.getSource() == deleteBoardBtn) {
					if(selectedBoard==null) {
						log.severe("there is no selected board to delete");
						return;
					}
					deleteBoard(selectedBoard.getName());
				}
			}
		};
		
		clearBtn = new JButton("Clear Board");
		clearBtn.addActionListener(actionListener);
		clearBtn.setToolTipText("Clear the current board - clears remote copies as well");
		clearBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		blackBtn = new JButton("Black");
		blackBtn.addActionListener(actionListener);
		blackBtn.setToolTipText("Draw with black pen");
		blackBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		redBtn = new JButton("Red");
		redBtn.addActionListener(actionListener);
		redBtn.setToolTipText("Draw with red pen");
		redBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		deleteBoardBtn = new JButton("Delete Board");
		deleteBoardBtn.addActionListener(actionListener);
		deleteBoardBtn.setToolTipText("Delete the current board - only deletes the board locally");
		deleteBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		createBoardBtn = new JButton("New Board");
		createBoardBtn.addActionListener(actionListener);
		createBoardBtn.setToolTipText("Create a new board - creates it locally and not shared by default");
		createBoardBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		undoBtn = new JButton("Undo");
		undoBtn.addActionListener(actionListener);
		undoBtn.setToolTipText("Remove the last path drawn on the board - triggers an undo on remote copies as well");
		undoBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
		sharedCheckbox = new JCheckBox("Shared");
		sharedCheckbox.addItemListener(new ItemListener() {    
	         public void itemStateChanged(ItemEvent e) { 
	            if(!modifyingCheckBox) setShare(e.getStateChange()==1);
	         }    
	      }); 
		sharedCheckbox.setToolTipText("Toggle whether the board is shared or not - tells the whiteboard server");
		sharedCheckbox.setAlignmentX(Component.CENTER_ALIGNMENT);
		

		// create a drop list for boards to select from
		JPanel controlsNorth = new JPanel();
		boardComboBox = new JComboBox<String>();
		boardComboBox.addActionListener(actionListener);
		
		
		// add to panel
		controlsNorth.add(boardComboBox);
		controls.add(sharedCheckbox);
		controls.add(createBoardBtn);
		controls.add(deleteBoardBtn);
		controls.add(blackBtn);
		controls.add(redBtn);
		controls.add(undoBtn);
		controls.add(clearBtn);

		// add to content pane
		content.add(controls, BorderLayout.WEST);
		content.add(controlsNorth,BorderLayout.NORTH);

		frame.setSize(600, 600);
		
		// create an initial board
		createBoard();
		
		// closing the application
		frame.addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosing(WindowEvent windowEvent) {
		        if (JOptionPane.showConfirmDialog(frame, 
		            "Are you sure you want to close this window?", "Close Window?", 
		            JOptionPane.YES_NO_OPTION,
		            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION)
		        {
		        	guiShutdown();
		            frame.dispose();
		        }
		    }
		});
		
		// show the swing paint result
		frame.setVisible(true);
		
	}
	
	/**
	 * Update the GUI's list of boards. Note that this method needs to update data
	 * that the GUI is using, which should only be done on the GUI's thread, which
	 * is why invoke later is used.
	 * 
	 * @param select, board to select when list is modified or null for default
	 *                selection
	 */
	private void updateComboBox(String select) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				modifyingComboBox=true;
				boardComboBox.removeAllItems();
				int anIndex=-1;
				synchronized(whiteboards) {
					ArrayList<String> boards = new ArrayList<String>(whiteboards.keySet());
					Collections.sort(boards);
					for(int i=0;i<boards.size();i++) {
						String boardname=boards.get(i);
						boardComboBox.addItem(boardname);
						if(select!=null && select.equals(boardname)) {
							anIndex=i;
						} else if(anIndex==-1 && selectedBoard!=null && 
								selectedBoard.getName().equals(boardname)) {
							anIndex=i;
						} 
					}
				}
				modifyingComboBox=false;
				if(anIndex!=-1) {
					boardComboBox.setSelectedIndex(anIndex);
				} else {
					if(whiteboards.size()>0) {
						boardComboBox.setSelectedIndex(0);
					} else {
						drawArea.clear();
						createBoard();
					}
				}
				
			}
		});
	}
	
}
