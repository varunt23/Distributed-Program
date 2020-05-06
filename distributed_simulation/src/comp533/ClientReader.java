package comp533;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class ClientReader implements Runnable {
	protected ArrayBlockingQueue<Message> readQueue;
	
	public ClientReader(ArrayBlockingQueue<Message> readQueue) {
		this.readQueue = readQueue;
		
	}

	@Override
	public void run() {
		Message message = null;

		while (true) {
			try {
				message = readQueue.take();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ByteBuffer bb = message.getBb();
			String command = new String(bb.array(), bb.position(), bb.array().length);
			util.trace.port.consensus.ProposalLearnedNotificationReceived.newCase(this, util.trace.port.consensus.communication.CommunicationStateNames.COMMAND,-1, command);
			util.trace.port.consensus.ProposedStateSet.newCase(this, util.trace.port.consensus.communication.CommunicationStateNames.COMMAND,-1, command);
		}
	}
}
