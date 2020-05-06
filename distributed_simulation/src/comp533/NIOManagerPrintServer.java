package comp533;

import inputport.nio.manager.listeners.SocketChannelAcceptListener;
import inputport.nio.manager.listeners.SocketChannelReadListener;
import inputport.nio.manager.listeners.SocketChannelWriteListener;

public interface NIOManagerPrintServer extends SocketChannelAcceptListener, SocketChannelReadListener, SocketChannelWriteListener {

}
