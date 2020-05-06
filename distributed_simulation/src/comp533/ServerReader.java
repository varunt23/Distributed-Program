package comp533;

import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

import inputport.nio.manager.NIOManager;
import inputport.nio.manager.NIOManagerFactory;

public class ServerReader implements Runnable {
	protected ArrayBlockingQueue<Message> readQueue;
	protected NIOManager nioManager = NIOManagerFactory.getSingleton();
	protected Set<SocketChannel> soc;

	public ServerReader(ArrayBlockingQueue<Message> readQueue, Set<SocketChannel> soc) {
		this.readQueue = readQueue;
		this.soc = soc;
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
			for (SocketChannel sock : soc) {
				nioManager.write(sock, message.getBb());
			}
		}
	}
}
