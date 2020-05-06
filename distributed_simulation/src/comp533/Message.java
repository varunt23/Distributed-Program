package comp533;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Message {
	
	private ByteBuffer bb;
	private SocketChannel soc;
	public Message(SocketChannel soc, ByteBuffer bb) {
		this.bb = bb;
		this.soc = soc;
	}
	public ByteBuffer getBb() {
		return bb;
	}
	public void setBb(ByteBuffer bb) {
		this.bb = bb;
	}
	public SocketChannel getSoc() {
		return soc;
	}
	public void setSoc(SocketChannel soc) {
		this.soc = soc;
	}
}
