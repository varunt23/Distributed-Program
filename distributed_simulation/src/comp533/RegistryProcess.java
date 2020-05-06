package comp533;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.Scanner;

import assignments.util.mainArgs.RegistryArgsProcessor;

import util.annotations.Tags;
import util.tags.DistributedTags;
@Tags({DistributedTags.REGISTRY, DistributedTags.RMI})

public class RegistryProcess {

	public static void main(String[] args) {
		java.rmi.registry.Registry registry;
		try {
			int port = RegistryArgsProcessor.getRegistryPort(args);
			registry = LocateRegistry.createRegistry(port);
			util.trace.port.rpc.rmi.RMIRegistryCreated.newCase(registry, port);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		Scanner sc = new Scanner(System.in);
		sc.nextLine();
	}
}
