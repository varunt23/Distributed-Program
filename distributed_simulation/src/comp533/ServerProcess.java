package comp533;

import java.rmi.RemoteException;
import inputport.rpc.GIPCLocateRegistry;
import inputport.rpc.GIPCRegistry;
import port.ATracingConnectionListener;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;


import assignments.util.mainArgs.ServerArgsProcessor;
import inputport.rpc.GIPCLocateRegistry;
import port.ATracingConnectionListener;
import util.trace.bean.BeanTraceUtility;
import util.trace.factories.FactoryTraceUtility;
import util.trace.misc.ThreadDelayed;
import util.trace.port.PortTraceUtility;
import util.trace.port.consensus.ConsensusTraceUtility;
import util.trace.port.nio.NIOTraceUtility;
import util.trace.port.rpc.gipc.GIPCRPCTraceUtility;
import util.trace.port.rpc.rmi.RMITraceUtility;
import util.annotations.Tags;
import util.tags.DistributedTags;
@Tags({DistributedTags.SERVER, DistributedTags.RMI, DistributedTags.NIO, DistributedTags.GIPC})

public class ServerProcess {

	public static void main(String[] args) {
		NIOTraceUtility.setTracing();
		FactoryTraceUtility.setTracing();
		BeanTraceUtility.setTracing();
		RMITraceUtility.setTracing();
		ConsensusTraceUtility.setTracing();
		ThreadDelayed.enablePrint();
		GIPCRPCTraceUtility.setTracing();
		
		java.rmi.registry.Registry registry;
		try {
			int port = ServerArgsProcessor.getRegistryPort(args);
			String host = ServerArgsProcessor.getRegistryHost(args);
			registry = LocateRegistry.getRegistry(host, port);
			util.trace.port.rpc.rmi.RMIRegistryLocated.newCase(registry, host, port, registry);
			ServerInterface server = new Server(ServerArgsProcessor.getNIOServerPort(args));
			String name = ServerInterface.class.getName();

			UnicastRemoteObject.exportObject(server, 0);
			registry.rebind(name, server);
			util.trace.port.rpc.rmi.RMIObjectRegistered.newCase(server, name, server, registry);
			
			
			GIPCRegistry gipcRegistry = GIPCLocateRegistry.createRegistry(ServerArgsProcessor.getGIPCServerPort(args));
			gipcRegistry.rebind(name, server);	
			gipcRegistry.getInputPort().addConnectionListener(new ATracingConnectionListener(gipcRegistry.getInputPort()));
			server.init();
			server.start();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

}
