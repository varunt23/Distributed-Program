package comp533;

import java.rmi.RemoteException;
import java.util.concurrent.ArrayBlockingQueue;

import assignments.util.MiscAssignmentUtils;
import assignments.util.inputParameters.AnAbstractSimulationParametersBean;
import assignments.util.mainArgs.ClientArgsProcessor;
import consensus.ProposalFeedbackKind;
import coupledsims.Simulation;
import coupledsims.Simulation1;
import inputport.nio.manager.NIOManager;
import inputport.nio.manager.NIOManagerFactory;
import inputport.nio.manager.factories.classes.AConnectCommandFactory;
import inputport.nio.manager.factories.selectors.ConnectCommandFactorySelector;
import inputport.nio.manager.listeners.SocketChannelConnectListener;
import inputport.nio.manager.listeners.SocketChannelReadListener;
import inputport.nio.manager.listeners.SocketChannelWriteListener;
import main.BeauAndersonFinalProject;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import stringProcessors.HalloweenCommandProcessor;
import util.interactiveMethodInvocation.ConsensusAlgorithm;
import util.interactiveMethodInvocation.IPCMechanism;
import util.interactiveMethodInvocation.SimulationParametersControllerFactory;
import util.misc.ThreadSupport;
import util.trace.Tracer;
import util.trace.factories.FactoryTraceUtility;
import util.trace.misc.ThreadDelayed;
import util.trace.port.PerformanceExperimentEnded;
import util.trace.port.PerformanceExperimentStarted;
import util.trace.port.PortTraceUtility;
import util.trace.port.consensus.ConsensusTraceUtility;
import util.trace.port.nio.NIOTraceUtility;
import util.trace.port.rpc.rmi.RMITraceUtility;

public class Client extends AnAbstractSimulationParametersBean implements ClientInterface, GClientInterface,
		SocketChannelConnectListener, SocketChannelWriteListener, SocketChannelReadListener {

	ServerInterface server;
	HalloweenCommandProcessor commandP1;
	String name;
	String serverName;
	protected int NUM_EXPERIMENT_COMMANDS = 500;
	public static final String EXPERIMENT_COMMAND_1 = "move 1 -1";
	public static final String EXPERIMENT_COMMAND_2 = "undo";
	protected PropertyChangeListener listener;
	protected IPCMechanism check;
	protected NIOManager nioManager = NIOManagerFactory.getSingleton();
	protected SocketChannel socketChannel;
	private ArrayBlockingQueue<Message> readQueue;
	protected final String READ_THREAD_NAME = "Read Thread";

	public HalloweenCommandProcessor createSimulation(String aPrefix) {
		return BeauAndersonFinalProject.createSimulation(aPrefix, Simulation1.SIMULATION1_X_OFFSET,
				Simulation.SIMULATION_Y_OFFSET, Simulation.SIMULATION_WIDTH, Simulation.SIMULATION_HEIGHT,
				Simulation1.SIMULATION1_X_OFFSET, Simulation.SIMULATION_Y_OFFSET);
	}

	protected Client(int aServerPort, ServerInterface server, IPCMechanism check) {
		setFactories();
		this.server = server;
		this.check = check;
		this.setIPCMechanism(IPCMechanism.RMI);
		initialize(aServerPort);
		readQueue = new ArrayBlockingQueue<Message>(1024);
		Thread cliRead = new Thread(new ClientReader(this.readQueue));
		cliRead.setName(READ_THREAD_NAME);
		cliRead.start();
	}

	@Override
	public void start(String[] args) {
		SimulationParametersControllerFactory.getSingleton().addSimulationParameterListener(this);
		SimulationParametersControllerFactory.getSingleton().processCommands();

	}

	@Override
	public void broadcast(String s) {
		util.misc.ThreadSupport.sleep(this.getDelay());
		util.trace.port.consensus.ProposalLearnedNotificationReceived.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, s);
		util.trace.port.consensus.ProposedStateSet.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, s);
		if (getConsensusAlgorithm() == ConsensusAlgorithm.CENTRALIZED_SYNCHRONOUS && s == null)
			s = "";
		// checking if atomic
		if(isAtomicBroadcast()) {
			commandP1.processCommand(s);
		}
	}

	public void quit() {
		System.exit(0);
	}

	void processArgs(String[] args) {
		System.out.println("Registry host:" + ClientArgsProcessor.getRegistryHost(args));
		System.out.println("Registry port:" + ClientArgsProcessor.getRegistryPort(args));
		System.out.println("Server host:" + ClientArgsProcessor.getServerHost(args));
		System.out.println("Headless:" + ClientArgsProcessor.getHeadless(args));
		System.out.println("Client name:" + ClientArgsProcessor.getClientName(args));
		name = ClientArgsProcessor.getClientName(args);
		serverName = ClientArgsProcessor.getServerHost(args);
		System.setProperty("java.awt.headless", ClientArgsProcessor.getHeadless(args));
	}

	protected void setTracing() {
		PortTraceUtility.setTracing();
		RMITraceUtility.setTracing();
		NIOTraceUtility.setTracing();
		FactoryTraceUtility.setTracing();
		ConsensusTraceUtility.setTracing();
		ThreadDelayed.enablePrint();
		trace(true);
	}

	public void init(String[] args) {
		setTracing();
		processArgs(args);
		listener = new ACoupler(ClientArgsProcessor.getClientName(args), server, this);
		commandP1 = createSimulation(Simulation1.SIMULATION1_PREFIX);
		commandP1.addPropertyChangeListener(listener);
	}

	public HalloweenCommandProcessor getCmd() {
		return commandP1;

	}

	public void setCmd(HalloweenCommandProcessor commandP1, String[] args) {
		setTracing();
		processArgs(args);
		listener = new ACoupler(ClientArgsProcessor.getClientName(args), server, this);
		this.commandP1 = commandP1;
		this.commandP1.addPropertyChangeListener(listener);
		SimulationParametersControllerFactory.getSingleton().addSimulationParameterListener(this);

	}

	@Override
	public void quit(int aCode) {
		System.exit(aCode);
	}

	@Override
	public void broadcastMetaState(boolean newValue) {
		setBroadcastMetaState(newValue);
		try {
			util.trace.port.consensus.ProposalMade.newCase(this,
					util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, newValue);

			server.broadcastMS(name, newValue);

			util.trace.port.consensus.RemoteProposeRequestSent.newCase(this,
					util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, newValue);

		} catch (RemoteException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void ipcMechanism(IPCMechanism newValue) {
		if (getConsensusAlgorithm() == ConsensusAlgorithm.CENTRALIZED_SYNCHRONOUS) {
			super.ipcMechanism(newValue);
			if (isBroadcastMetaState()) {
				try {
					util.trace.port.consensus.ProposalMade.newCase(this,
							util.trace.port.consensus.communication.CommunicationStateNames.IPC_MECHANISM, -1,
							newValue);
					server.broadcastGIPC(name, newValue);
					util.trace.port.consensus.RemoteProposeRequestSent.newCase(this,
							util.trace.port.consensus.communication.CommunicationStateNames.IPC_MECHANISM, -1,
							newValue);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		} else {
			super.ipcMechanism(newValue);
			if (isBroadcastMetaState()) {
				try {
					util.trace.port.consensus.ProposalMade.newCase(this,
							util.trace.port.consensus.communication.CommunicationStateNames.IPC_MECHANISM, -1,
							newValue);
					server.broadcastGIPC(name, newValue);
					util.trace.port.consensus.RemoteProposeRequestSent.newCase(this,
							util.trace.port.consensus.communication.CommunicationStateNames.IPC_MECHANISM, -1,
							newValue);

				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void simulationCommand(String aCommand) {
		if (this.getIPCMechanism() != check) {
			return;
		}
		long aDelay = getDelay();
		if (aDelay > 0) {
			ThreadSupport.sleep(aDelay);
		}

		commandP1.setInputString(aCommand);
	}

	@Override
	public void trace(boolean newValue) {
		super.trace(newValue);
		Tracer.showInfo(isTrace());
	}

	@Override
	public void localProcessingOnly(boolean newValue) {
		super.localProcessingOnly(newValue);
		if (isLocalProcessingOnly()) {
			commandP1.removePropertyChangeListener(listener);
		} else {
			commandP1.addPropertyChangeListener(listener);
		}
	}

	@Override
	public void atomicBroadcast(boolean newValue) {
		super.atomicBroadcast(newValue);
		commandP1.setConnectedToSimulation(!isAtomicBroadcast());
		if (isBroadcastMetaState()) {
			try {
				util.trace.port.consensus.ProposalMade.newCase(this,
						util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, newValue);
				server.broadcastAtomic(name, newValue);
				util.trace.port.consensus.RemoteProposeRequestSent.newCase(this,
						util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, newValue);

			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void consensusAlgorithm(ConsensusAlgorithm conAlg) {
		setConsensusAlgorithm(conAlg);
		if (!isAtomicBroadcast())
			atomicBroadcast(true);
		if (isBroadcastMetaState()) {
			try {
				util.trace.port.consensus.ProposalMade.newCase(this,
						util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, conAlg);
				server.broadcastCA(name, conAlg);
				util.trace.port.consensus.RemoteProposeRequestSent.newCase(this,
						util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, conAlg);

			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void experimentInput() {
		long aStartTime = System.currentTimeMillis();
		PerformanceExperimentStarted.newCase(this, aStartTime, NUM_EXPERIMENT_COMMANDS);
		boolean anOldValue = isTrace();
		this.trace(false);
		for (int i = 0; i < NUM_EXPERIMENT_COMMANDS; i++) {
			commandP1.setInputString(EXPERIMENT_COMMAND_1);
			commandP1.setInputString(EXPERIMENT_COMMAND_2);
		}
		trace(anOldValue);
		long anEndTime = System.currentTimeMillis();
		PerformanceExperimentEnded.newCase(this, aStartTime, anEndTime, anEndTime - aStartTime,
				NUM_EXPERIMENT_COMMANDS);

	}

	@Override
	public void delaySends(int aMillisecondDelay) {
		super.delaySends(aMillisecondDelay);
	}

	@Override
	public void broadcastGIPC(IPCMechanism str) {
		util.trace.port.consensus.ProposalLearnedNotificationReceived.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.IPC_MECHANISM, -1, str);
		super.ipcMechanism(str);
		util.trace.port.consensus.ProposedStateSet.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.IPC_MECHANISM, -1, str);
	}

	@Override
	public void broadcastMS(boolean newValue) {
		util.trace.port.consensus.ProposalLearnedNotificationReceived.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, newValue);
		setBroadcastMetaState(newValue);
		util.trace.port.consensus.ProposedStateSet.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, newValue);

	}

	protected void setFactories() {
		ConnectCommandFactorySelector.setFactory(new AConnectCommandFactory(SelectionKey.OP_READ));
	}

	public void processInput(String input) {
		ByteBuffer aWriteMessage = ByteBuffer.wrap(input.getBytes());
		nioManager.write(socketChannel, aWriteMessage, this);
	}

	protected void initialize(int aServerPort) {
		try {
			socketChannel = SocketChannel.open();
			InetAddress aServerAddress = InetAddress.getByName(serverName);
			nioManager.connect(socketChannel, aServerAddress, aServerPort, this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void connected(SocketChannel aSocketChannel) {
		nioManager.addReadListener(aSocketChannel, this);
	}

	@Override
	public void notConnected(SocketChannel theSocketChannel, Exception e) {

	}

	@Override
	public void written(SocketChannel socketChannel, ByteBuffer theWriteBuffer, int sendId) {

	}

	@Override
	public void socketChannelRead(SocketChannel aSocketChannel, ByteBuffer aMessage, int aLength) {
		readQueue.add(new Message(aSocketChannel, MiscAssignmentUtils.deepDuplicate(aMessage)));

	}

	@Override
	public void broadcastAtomic(boolean isAtomic) {
		util.trace.port.consensus.ProposalLearnedNotificationReceived.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, isAtomic);

		setAtomicBroadcast(isAtomic);
		commandP1.setConnectedToSimulation(!isAtomicBroadcast());

		util.trace.port.consensus.ProposedStateSet.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, isAtomic);

	}

	@Override
	public void broadcastCA(ConsensusAlgorithm conAlg) {
		util.trace.port.consensus.ProposalLearnedNotificationReceived.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, conAlg);

		this.setConsensusAlgorithm(conAlg);

		util.trace.port.consensus.ProposedStateSet.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.BROADCAST_MODE, -1, conAlg);

	}

	@Override
	public synchronized void accept(String clientInt, String command) {
		util.trace.port.consensus.ProposalAcceptRequestReceived.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, null);

		util.trace.port.consensus.ProposalAcceptedNotificationSent.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1, null,
				ProposalFeedbackKind.SUCCESS);
		try {
			server.setBool(true);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
