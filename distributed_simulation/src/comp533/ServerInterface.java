package comp533;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import consensus.ProposalFeedbackKind;
import util.interactiveMethodInvocation.ConsensusAlgorithm;
import util.interactiveMethodInvocation.IPCMechanism;

public interface ServerInterface extends Remote, Serializable {

	public void start() throws RemoteException;

	public void setClients(ClientInterface client, String str) throws RemoteException;

	public void broadcast(String cli, String str) throws RemoteException;

	void broadcastGIPC(String clientInt, IPCMechanism ipcMechanism) throws RemoteException;

	void setClientsGIPC(GClientInterface clientInt, String str) throws RemoteException;

	public void broadcastMS(String name, boolean newValue) throws RemoteException;

	void init() throws RemoteException;

	public void setClientsNIO(ClientInterface clientNIO, String cliname) throws RemoteException;

	void broadcastCA(String clientInt, ConsensusAlgorithm conAlg) throws RemoteException;

//	void broadcastAtomic(String clientInt, boolean isAtomic);
	void broadcastSync(String clientInt, String command) throws RemoteException;

	void broadcastAtomic(String clientInt, boolean isAtomic) throws RemoteException;
	public void setBool(boolean val) throws RemoteException;
//	void broadcastSync(String clientInt, String command);
	void returnAccept(ProposalFeedbackKind success) throws RemoteException;
//	void returnAccept(ProposalFeedbackKind success);
}
