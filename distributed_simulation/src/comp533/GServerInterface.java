package comp533;

import java.rmi.RemoteException;

import stringProcessors.HalloweenCommandProcessor;
import util.interactiveMethodInvocation.IPCMechanism;

public interface GServerInterface {
	public void start();
	public void setClients(ClientInterface client, String str);
	public void broadcast(String cli, String str);
	void broadcastGIPC(String clientInt, IPCMechanism ipcMechanism);
	void setClientsGIPC(GClientInterface clientInt, String str);
	public void broadcastMS(String name, boolean newValue);
	void init();
}
