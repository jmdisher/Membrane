package com.jeffdisher.membrane.rest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

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
		Thread.sleep(1000L);
		wrapper.stop();
	}

	@Test
	public void testBasicLaminar() throws Throwable {
		Assume.assumeNotNull(WRAPPER_SERVER_JAR);
		// Start a Laminar server and give it time to start up.
		ProcessWrapper wrapper = ProcessWrapper.startJavaProcess(WRAPPER_SERVER_JAR
				, "--clientIp", "127.0.0.1"
				, "--clientPort", "8000"
				, "--clusterIp", "127.0.0.1"
				, "--clusterPort", "9000"
				, "--data", _folder.newFolder().getAbsolutePath()
		);
		Thread.sleep(1000L);
		
		MembraneWrapper membrane = MembraneWrapper.cluster("127.0.0.1", 8000);
		Thread.sleep(1000L);
		membrane.stop();
		
		wrapper.stop();
	}


	private static class MembraneWrapper {
		public static MembraneWrapper localWrapper() {
			Thread runner = new Thread(() -> {
				MembraneRest.main(new String[] {"--local_only"});
			});
			runner.start();
			return new MembraneWrapper(runner);
		}
		
		public static MembraneWrapper cluster(String hostname, int port) {
			Thread runner = new Thread(() -> {
				MembraneRest.main(new String[] {"--hostname", hostname, "--port", Integer.toString(port)});
			});
			runner.start();
			return new MembraneWrapper(runner);
		}
		
		private final Thread _runner;
		private MembraneWrapper(Thread runner) {
			_runner = runner;
		}
		public void stop() throws MalformedURLException, IOException, InterruptedException {
			HttpURLConnection connection = (HttpURLConnection)new URL("http://localhost:8080/exit").openConnection();
			connection.setRequestMethod("DELETE");
			Assert.assertEquals("Shutting down\n".length(), connection.getContentLength());
			connection.disconnect();
			_runner.join();
		}
	}
}
