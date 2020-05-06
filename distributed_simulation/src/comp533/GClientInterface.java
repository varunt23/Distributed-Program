package comp533;

import consensus.ProposalFeedbackKind;
import stringProcessors.HalloweenCommandProcessor;
import util.interactiveMethodInvocation.ConsensusAlgorithm;
import util.interactiveMethodInvocation.IPCMechanism;

public interface GClientInterface {
	void start(String[] args);

	void init(String[] args);

	void broadcast(String s);

	public HalloweenCommandProcessor createSimulation(String prefix);

	void broadcastGIPC(IPCMechanism ipcMechanism);

	void broadcastMS(boolean newValue);

	void setCmd(HalloweenCommandProcessor commandP1, String[] args);

	HalloweenCommandProcessor getCmd();

//	public boolean receiveBroadcast(String message);
//	public boolean receiveG(IPCMechanism message);
//	public boolean receiveMS(boolean message);
	void broadcastCA(ConsensusAlgorithm conAlg);

	void broadcastAtomic(boolean isAtomic);

	public ConsensusAlgorithm getConsensusAlgorithm();

	void accept(String clientInt, String command);

}
