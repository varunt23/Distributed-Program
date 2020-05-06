package comp533;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.rmi.RemoteException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import assignments.util.MiscAssignmentUtils;
import assignments.util.inputParameters.AnAbstractSimulationParametersBean;
import consensus.ProposalFeedbackKind;
import inputport.nio.manager.NIOManager;
import inputport.nio.manager.NIOManagerFactory;
import inputport.nio.manager.factories.classes.AnAcceptCommandFactory;
import inputport.nio.manager.factories.selectors.AcceptCommandFactorySelector;
import util.interactiveMethodInvocation.ConsensusAlgorithm;
import util.interactiveMethodInvocation.IPCMechanism;
import util.interactiveMethodInvocation.SimulationParametersControllerFactory;
import util.trace.Tracer;
import util.trace.factories.FactoryTraceUtility;
import util.trace.port.nio.NIOTraceUtility;
import util.trace.port.nio.SocketChannelBound;

public class Server extends AnAbstractSimulationParametersBean implements ServerInterface, NIOManagerPrintServer {

	private static final long serialVersionUID = 1L;
	private Map<String, ClientInterface> clientList = new HashMap<>();
	private Map<String, GClientInterface> clientListGIPC = new HashMap<>();
	private Map<String, ClientInterface> clientListNIO = new HashMap<>();

	protected NIOManager nioManager = NIOManagerFactory.getSingleton();
	public Set<SocketChannel> clients = new HashSet<>();
	private ArrayBlockingQueue<Message> readQueue;
	protected final String READ_THREAD_NAME = "Read Thread";

	public String commandSync;
	public IPCMechanism ipcSync;
	public boolean rpc;

	public Server() {

	}

	public Server(int aServerPort) {
		setTracing();
		setFactories();
		initialize(aServerPort);
		readQueue = new ArrayBlockingQueue<Message>(1024);
		Thread serverRead = new Thread(new ServerReader(this.readQueue, clients));
		serverRead.setName(READ_THREAD_NAME);
		serverRead.start();

	}

	@Override
	public void start() {
		SimulationParametersControllerFactory.getSingleton().processCommands();
	}

	@Override
	public void init() {
		SimulationParametersControllerFactory.getSingleton().addSimulationParameterListener(this);
	}

	@Override
	public void setClients(ClientInterface clientInt, String str) {
		clientList.put(str, clientInt);

	}

	public void setClientsGIPC(GClientInterface clientInt, String str) {
		clientListGIPC.put(str, clientInt);
	}

	public void setClientsNIO(ClientInterface clientInt, String str) {
		clientListNIO.put(str, clientInt);

	}

	@Override
	public void trace(boolean newValue) {
		super.trace(newValue);
		Tracer.showInfo(isTrace());
	}

	@Override
	public void broadcast(String clientInt, String str) {
		util.trace.port.consensus.RemoteProposeRequestReceived.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, str);

		for (String c : clientListGIPC.keySet()) {
			if (!(c.equals(clientInt))) {
				util.trace.port.consensus.ProposalLearnedNotificationSent.newCase(this,
						util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, str);
				clientListGIPC.get(c).broadcast(str);
			}
			if (c.equals(clientInt) && isAtomicBroadcast()) {
				util.trace.port.consensus.ProposalLearnedNotificationSent.newCase(this,
						util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, str);
				clientListGIPC.get(c).broadcast(str);
			}
		}

		for (String c : clientList.keySet()) {
			if (!(c.equals(clientInt))) {
				try {
					util.trace.port.consensus.ProposalLearnedNotificationSent.newCase(this,
							util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, str);
					clientList.get(c).broadcast(str);

				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			if ((c.equals(clientInt)) && isAtomicBroadcast()) {
				util.trace.port.consensus.ProposalLearnedNotificationSent.newCase(this,
						util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, str);
				try {
					clientList.get(c).broadcast(str);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
		for (String c : clientListNIO.keySet()) {
			if (!(c.equals(clientInt))) {
				util.trace.port.consensus.ProposalLearnedNotificationSent.newCase(this,
						util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, str);
				try {
					clientListNIO.get(c).broadcast(str);

				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			if ((c.equals(clientInt)) && isAtomicBroadcast()) {
				util.trace.port.consensus.ProposalLearnedNotificationSent.newCase(this,
						util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, str);
				try {
					clientListNIO.get(c).broadcast(str);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

	}

	@Override
	public void broadcastMetaState(boolean newValue) {
		setBroadcastMetaState(newValue);

		util.trace.port.consensus.ProposalMade.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, newValue);

		this.broadcastMS(null, newValue);

		util.trace.port.consensus.RemoteProposeRequestSent.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, newValue);

	}

	@Override
	public void ipcMechanism(IPCMechanism newValue) {
		super.ipcMechanism(newValue);
		if (isBroadcastMetaState()) {
			util.trace.port.consensus.ProposalMade.newCase(this,
					util.trace.port.consensus.communication.CommunicationStateNames.IPC_MECHANISM, -1, newValue);
			this.broadcastGIPC(null, newValue);
			util.trace.port.consensus.RemoteProposeRequestSent.newCase(this,
					util.trace.port.consensus.communication.CommunicationStateNames.IPC_MECHANISM, -1, newValue);
		}
	}

	@Override
	public void broadcastGIPC(String clientInt, IPCMechanism ipcMechanism) {
		util.trace.port.consensus.RemoteProposeRequestReceived.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.IPC_MECHANISM, -1, ipcMechanism);
		util.trace.port.consensus.ProposalLearnedNotificationSent.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.IPC_MECHANISM, -1, ipcMechanism);

		super.ipcMechanism(ipcMechanism);
		for (String c : clientList.keySet()) {
			if (!(c.equals(clientInt))) {
				try {
					clientList.get(c).broadcastGIPC(ipcMechanism);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

		for (String c : clientListGIPC.keySet()) {
			if (!(c.equals(clientInt))) {
				clientListGIPC.get(c).broadcastGIPC(ipcMechanism);
			}

		}
		for (String c : clientListNIO.keySet()) {
			if (!(c.equals(clientInt))) {
				try {
					clientListNIO.get(c).broadcastGIPC(ipcMechanism);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public void quit() {
		System.exit(0);
	}

	@Override
	public void broadcastMS(String name, boolean newValue) {
		util.trace.port.consensus.RemoteProposeRequestReceived.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, newValue);
		util.trace.port.consensus.ProposalLearnedNotificationSent.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, newValue);

		setBroadcastMetaState(newValue);
		for (String c : clientList.keySet()) {
			if (!(c.equals(name))) {
				try {
					clientList.get(c).broadcastMS(newValue);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

		for (String c : clientListGIPC.keySet()) {
			if (!(c.equals(name))) {
				clientListGIPC.get(c).broadcastMS(newValue);
			}
		}

		for (String c : clientListNIO.keySet()) {
			if (!(c.equals(name))) {
				try {
					clientListNIO.get(c).broadcastMS(newValue);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected void setFactories() {
		AcceptCommandFactorySelector.setFactory(new AnAcceptCommandFactory(SelectionKey.OP_READ));
	}

	protected void setTracing() {
		FactoryTraceUtility.setTracing();
		NIOTraceUtility.setTracing();
	}

	protected void initialize(int aServerPort) {
		try {
			ServerSocketChannel aServerFactoryChannel = ServerSocketChannel.open();
			InetSocketAddress anInternetSocketAddress = new InetSocketAddress(aServerPort);
			aServerFactoryChannel.socket().bind(anInternetSocketAddress);
			SocketChannelBound.newCase(this, aServerFactoryChannel, anInternetSocketAddress);
			nioManager.enableListenableAccepts(aServerFactoryChannel, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void socketChannelAccepted(ServerSocketChannel aServerSocketChannel, SocketChannel aSocketChannel) {
		nioManager.addReadListener(aSocketChannel, this); // a socket channel is the client channel
		clients.add(aSocketChannel);
	}

	@Override
	public void socketChannelRead(SocketChannel aSocketChannel, ByteBuffer aMessage, int aLength) {

		String aMessageString = new String(aMessage.array(), aMessage.position(), aLength);
		util.trace.port.consensus.RemoteProposeRequestReceived.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, aMessageString);
		util.trace.port.consensus.ProposalLearnedNotificationSent.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, aMessageString);

		readQueue.add(new Message(aSocketChannel, MiscAssignmentUtils.deepDuplicate(aMessage)));
	}

	@Override
	public void written(SocketChannel socketChannel, ByteBuffer theWriteBuffer, int sendId) {

	}

	@Override
	public void broadcastAtomic(String clientInt, boolean isAtomic) {
		util.trace.port.consensus.RemoteProposeRequestReceived.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, isAtomic);
		util.trace.port.consensus.ProposalLearnedNotificationSent.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, isAtomic);

		this.setAtomicBroadcast(isAtomic);
		for (String c : clientList.keySet()) {
			if (!(c.equals(clientInt))) {
				try {
					clientList.get(c).broadcastAtomic(isAtomic);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

		for (String c : clientListGIPC.keySet()) {
			if (!(c.equals(clientInt))) {
				clientListGIPC.get(c).broadcastAtomic(isAtomic);
			}
		}

		for (String c : clientListNIO.keySet()) {
			if (!(c.equals(clientInt))) {
				try {
					clientListNIO.get(c).broadcastAtomic(isAtomic);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void broadcastCA(String clientInt, ConsensusAlgorithm conAlg) {
		util.trace.port.consensus.RemoteProposeRequestReceived.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, conAlg);
		util.trace.port.consensus.ProposalLearnedNotificationSent.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, conAlg);

		this.setConsensusAlgorithm(conAlg);
		for (String c : clientList.keySet()) {
			if (!(c.equals(clientInt))) {
				try {
					clientList.get(c).broadcastCA(conAlg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}

		for (String c : clientListGIPC.keySet()) {
			if (!(c.equals(clientInt))) {
				clientListGIPC.get(c).broadcastCA(conAlg);
			}
		}

		for (String c : clientListNIO.keySet()) {
			if (!(c.equals(clientInt))) {
				try {
					clientListNIO.get(c).broadcastCA(conAlg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void broadcastSync(String clientInt, String command) {
		util.trace.port.consensus.RemoteProposeRequestReceived.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, command);
		commandSync = command;
		ProposalFeedbackKind consensusAccept = ProposalFeedbackKind.SUCCESS;
		boolean temp = true;
		for (String c : clientListGIPC.keySet()) {
			util.trace.port.consensus.ProposalAcceptRequestSent.newCase(this,
					util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, command);
			//temp = clientListGIPC.get(c).accept(clientInt, command);
			clientListGIPC.get(c).accept(clientInt, command);
			temp = this.rpc;
			if (!temp)
				consensusAccept = ProposalFeedbackKind.ACCESS_DENIAL;
			util.trace.port.consensus.ProposalAcceptedNotificationReceived.newCase(this,
					util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, null, consensusAccept);
		}

		for (String c : clientList.keySet()) {
			try {
				util.trace.port.consensus.ProposalAcceptRequestSent.newCase(this,
						util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, command);
				//temp = clientList.get(c).accept(clientInt, command);
				clientList.get(c).accept(clientInt, command);
				temp = this.rpc;

				if (!temp)
					consensusAccept = ProposalFeedbackKind.ACCESS_DENIAL;
				util.trace.port.consensus.ProposalAcceptedNotificationReceived.newCase(this,
						util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, null, consensusAccept);
			} catch (RemoteException e) {
				e.printStackTrace();
			}

		}
		for (String c : clientListNIO.keySet()) {
			try {
				util.trace.port.consensus.ProposalAcceptRequestSent.newCase(this,
						util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, command);
				//temp = clientListNIO.get(c).accept(clientInt, command);
				clientListNIO.get(c).accept(clientInt, command);
				temp = this.rpc;
				if (!temp)
					consensusAccept = ProposalFeedbackKind.ACCESS_DENIAL;
				util.trace.port.consensus.ProposalAcceptedNotificationReceived.newCase(this,
						util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, null, consensusAccept);

			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		returnAccept(consensusAccept);

	}
	
	
	public void setBool(boolean val) {
		this.rpc = val;
	}

	@Override
	public void returnAccept(ProposalFeedbackKind success) {
		if (success == ProposalFeedbackKind.ACCESS_DENIAL )
			commandSync = null;
		for (String c : clientListGIPC.keySet()) {
			util.trace.port.consensus.ProposalLearnedNotificationSent.newCase(this,
					util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, commandSync);
			clientListGIPC.get(c).broadcast(commandSync);
		}

		for (String c : clientList.keySet()) {
			try {
				util.trace.port.consensus.ProposalLearnedNotificationSent.newCase(this,
						util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, commandSync);
				clientList.get(c).broadcast(commandSync);

			} catch (RemoteException e) {
				e.printStackTrace();
			}

		}

		for (String c : clientListNIO.keySet()) {
			try {
				util.trace.port.consensus.ProposalLearnedNotificationSent.newCase(this,
						util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, commandSync);
				clientListNIO.get(c).broadcast(commandSync);

			} catch (RemoteException e) {
				e.printStackTrace();
			}

		}
	}

}
