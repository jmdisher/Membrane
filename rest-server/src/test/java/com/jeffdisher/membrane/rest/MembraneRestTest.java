package com.jeffdisher.membrane.rest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


/**
 * Note that this REQUIRES the "WRAPPER_SERVER_JAR" to be set in environment variables.
 */
public class MembraneRestTest {
	private static final String WRAPPER_SERVER_JAR = System.getenv("WRAPPER_SERVER_JAR");

	@Rule
	public TemporaryFolder _folder = new TemporaryFolder();

	@Test
	public void testBasicLocal() throws Throwable {
		MembraneWrapper wrapper = MembraneWrapper.localWrapper();
		wrapper.stop();
	}

	@Test
	public void testBasicLaminar() throws Throwable {
		Assume.assumeNotNull(WRAPPER_SERVER_JAR);
		// Start a Laminar server and give it time to start up.
		ProcessWrapper wrapper = ProcessWrapper.startJavaProcess(WRAPPER_SERVER_JAR
				, "Laminar ready for leader connection or config upload..."
				, "--clientIp", "127.0.0.1"
				, "--clientPort", "8000"
				, "--clusterIp", "127.0.0.1"
				, "--clusterPort", "9000"
				, "--data", _folder.newFolder().getAbsolutePath()
		);
		
		MembraneWrapper membrane = MembraneWrapper.cluster("127.0.0.1", 8000);
		membrane.stop();
		
		wrapper.stop();
	}


	private static class MembraneWrapper {
		public static MembraneWrapper localWrapper() throws InterruptedException {
			CountDownLatch latch = new CountDownLatch(1);
			Thread runner = new Thread(() -> {
				MembraneRest.mainInTest(latch, new String[] {"--local_only"});
			});
			runner.start();
			latch.await();
			return new MembraneWrapper(runner);
		}
		
		public static MembraneWrapper cluster(String hostname, int port) throws InterruptedException {
			CountDownLatch latch = new CountDownLatch(1);
			Thread runner = new Thread(() -> {
				MembraneRest.mainInTest(latch, new String[] {"--hostname", hostname, "--port", Integer.toString(port)});
			});
			runner.start();
			latch.await();
			return new MembraneWrapper(runner);
		}
		
		private final Thread _runner;
		private MembraneWrapper(Thread runner) {
			_runner = runner;
		}
		public void stop() throws MalformedURLException, IOException, InterruptedException {
			HttpURLConnection connection = (HttpURLConnection)new URL("http://localhost:8080/exit").openConnection();
			connection.setRequestMethod("DELETE");
			// Note that sometimes the shutdown happens so quickly that we don't even get a response so just force the send.
			connection.getContentLength();
			connection.disconnect();
			_runner.join();
		}
	}
}
