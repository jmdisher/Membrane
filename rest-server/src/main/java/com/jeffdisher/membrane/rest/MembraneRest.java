package com.jeffdisher.membrane.rest;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;

import com.jeffdisher.breakwater.RestServer;
import com.jeffdisher.laminar.utils.Assert;
import com.jeffdisher.membrane.store.SynchronousStore;
import com.jeffdisher.membrane.store.connection.IConnectionFactory;
import com.jeffdisher.membrane.store.laminar.LaminarConnectionFactory;
import com.jeffdisher.membrane.store.local.LocalFactory;


public class MembraneRest {
	private static final String ARG_HOSTNAME = "hostname";
	private static final String ARG_PORT = "port";
	private static final String ARG_LOCAL_ONLY = "local_only";

	public static void main(String[] args) {
		// The normal entry-point doesn't care about the latch so just create anything.
		_main(new CountDownLatch(0), args);
	}

	public static void mainInTest(CountDownLatch bindLatch, String[] args) {
		// We are being called from a test so we want to pass in the latch the test gave us.
		_main(bindLatch, args);
	}

	private static void _main(CountDownLatch bindLatch, String[] args) {
		// Parse arguments.
		String hostname = _getArgument(args, ARG_HOSTNAME);
		String portString = _getArgument(args, ARG_PORT);
		boolean localOnly = _getFlag(args, ARG_LOCAL_ONLY);
		if (!localOnly && (null == hostname)) {
			_failStart("Missing hostname");
		}
		if (!localOnly && (null == portString)) {
			_failStart("Missing port");
		}
		
		// Create the appropriate factory.
		IConnectionFactory factory;
		if (localOnly) {
			factory = new LocalFactory();
		} else {
			int port;
			try {
				port = Integer.parseInt(portString);
			} catch (NumberFormatException e) {
				throw _failStart("Port not a number: \"" + portString + "\"");
			}
			InetSocketAddress server;
			try{
				server = _parseIpAndPort(hostname, port);
			} catch (UnknownHostException e) {
				throw _failStart("Unknown host: \"" + hostname + "\"");
			}
			factory = new LaminarConnectionFactory(server);
		}
		
		// Create the store and start the server.
		SynchronousStore store;
		try {
			store = new SynchronousStore(factory);
		} catch (IOException e) {
			throw _failStart("IO Exception while starting store: " + e.getLocalizedMessage());
		}
		
		// Create the server and start it.
		// We don't support any static content delivery.
		RestServer server = new RestServer(8080, null);
		CountDownLatch stopLatch = new CountDownLatch(1);
		new EntryPointManager(stopLatch, server, store);
		server.start();
		
		// Count-down the latch in case we are part of a testing environment.
		bindLatch.countDown();
		
		// Wait until we are told to shut down.
		try {
			stopLatch.await();
		} catch (InterruptedException e) {
			// We don't use interruption.
			throw Assert.unexpected(e);
		}
		server.stop();
		try {
			store.close();
		} catch (IOException e) {
			// If this happens on shutdown, just print it.
			e.printStackTrace();
		}
	}


	private static InetSocketAddress _parseIpAndPort(String ipString, int port) throws UnknownHostException {
		InetAddress ip = InetAddress.getByName(ipString);
		return new InetSocketAddress(ip, port);
	}

	private static String _getArgument(String[] args, String flag) {
		String check1 = "--" + flag;
		String check2 = "-" + flag.substring(0, 1);
		String match = null;
		for (int i = 0; (null == match) && (i < (args.length - 1)); ++i) {
			if (check1.equals(args[i]) || check2.equals(args[i])) {
				match = args[i+1];
			}
		}
		return match;
	}

	private static boolean _getFlag(String[] args, String flag) {
		String check1 = "--" + flag;
		String check2 = "-" + flag.substring(0, 1);
		boolean match = false;
		for (int i = 0; !match && (i < args.length); ++i) {
			if (check1.equals(args[i]) || check2.equals(args[i])) {
				match = true;
			}
		}
		return match;
	}

	private static RuntimeException _failStart(String problem) {
		System.err.println(problem);
		System.err.println("Usage: MembraneRest (--hostname <hostname> --port <port>)|--local_only");
		System.exit(1);
		// We never reach this point but it allows us to throw in the caller so flow control is explicit.
		throw new RuntimeException();
	}
}
