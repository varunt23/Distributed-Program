package comp533;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.rmi.RemoteException;

import util.interactiveMethodInvocation.ConsensusAlgorithm;
import util.interactiveMethodInvocation.IPCMechanism;
import util.trace.trickOrTreat.LocalCommandObserved;

public class ACoupler implements PropertyChangeListener {

	private String name;
	private ServerInterface server;
	private Client client;

	public ACoupler(String name, ServerInterface server, Client client) {
		this.server = server;
		this.name = name;
		this.client = client;
	}

	public void propertyChange(PropertyChangeEvent evt) {
		if (!evt.getPropertyName().equals("InputString"))
			return;
		LocalCommandObserved.newCase(this, (String) evt.getNewValue());
		util.trace.port.consensus.ProposalMade.newCase(this,
				util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1,
				(String) evt.getNewValue());
		if (client.getConsensusAlgorithm() == ConsensusAlgorithm.CENTRALIZED_ASYNCHRONOUS) {
			try {
				util.trace.port.consensus.RemoteProposeRequestSent.newCase(this,
						util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1,
						(String) evt.getNewValue());
				if (client.getIPCMechanism() == IPCMechanism.NIO && client.check == IPCMechanism.NIO) {
					client.processInput((String) evt.getNewValue());
				} else {
					server.broadcast(name, (String) evt.getNewValue());
				}

			} catch (RemoteException e) {
				e.printStackTrace();
			}

		} else if (client.getConsensusAlgorithm() == ConsensusAlgorithm.CENTRALIZED_SYNCHRONOUS) {
			try {
				util.trace.port.consensus.RemoteProposeRequestSent.newCase(this,
						util.trace.port.consensus.communication.CommunicationStateNames.COMMAND, -1,
						(String) evt.getNewValue());
				if (client.getIPCMechanism() == IPCMechanism.NIO && client.check == IPCMechanism.NIO) {
					client.processInput((String) evt.getNewValue());
				} else {
					server.broadcastSync(name, (String) evt.getNewValue());
				}

			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
}