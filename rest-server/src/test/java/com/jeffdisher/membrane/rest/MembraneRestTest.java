package com.jeffdisher.membrane.rest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.jeffdisher.membrane.avm.CounterProgram;


/**
 * Note that this REQUIRES the "WRAPPER_SERVER_JAR" to be set in environment variables.
 */
public class MembraneRestTest {
	private static final String WRAPPER_SERVER_JAR = System.getenv("WRAPPER_SERVER_JAR");
	private static final String MEMBRANE_URL = "http://localhost:8080/";

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

	/**
	 * Does what test.sh does, but in JUnit.
	 */
	@Test
	public void testDotShLocal() throws Throwable {
		MembraneWrapper membrane = MembraneWrapper.localWrapper();
		_testDotSh();
		membrane.stop();
	}

	/**
	 * Does what test.sh followed by test2.sh do, but in JUnit, and against a Laminar cluster.
	 */
	@Test
	public void test2DotShLaminar() throws Throwable {
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
		
		// Start the Membrane instance to run the first use-case.
		MembraneWrapper membrane = MembraneWrapper.cluster("127.0.0.1", 8000);
		_testDotSh();
		membrane.stop();
		
		// Start a new Membrane instance on the same cluster to show the overlap in data with the new use-case.
		membrane = MembraneWrapper.cluster("127.0.0.1", 8000);
		_test2DotSh();
		membrane.stop();
		
		wrapper.stop();
	}

	/**
	 * Does what test_avm.sh does, but in JUnit.
	 */
	@Test
	public void testAvmDotShLaminar() throws Throwable {
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
		_testAvmDotSh();
		membrane.stop();
		
		wrapper.stop();
	}


	private void _testDotSh() throws Throwable {
		// Create the topics.
		Assert.assertArrayEquals("topic1\n".getBytes(), RestHelpers.post(MEMBRANE_URL + "topic1", _createPostMap("String", new byte[0], new byte[0], false)));
		Assert.assertArrayEquals("topic2\n".getBytes(), RestHelpers.post(MEMBRANE_URL + "topic2", _createPostMap("String", new byte[0], new byte[0], false)));
		Assert.assertArrayEquals("employee_number\n".getBytes(), RestHelpers.post(MEMBRANE_URL + "employee_number", _createPostMap("integer", new byte[0], new byte[0], false)));
		
		// Populate the topics for key1 and key2.
		Assert.assertArrayEquals("Received 4 bytes\n".getBytes(), RestHelpers.put(MEMBRANE_URL + "employee_number/key1", new byte[] {0,0,0,1}));
		Assert.assertArrayEquals("Received 4 bytes\n".getBytes(), RestHelpers.put(MEMBRANE_URL + "employee_number/key2", new byte[] {0,0,0,2}));
		Assert.assertArrayEquals("Received 7 bytes\n".getBytes(), RestHelpers.put(MEMBRANE_URL + "topic1/key1", "TESTING".getBytes(StandardCharsets.UTF_8)));
		Assert.assertArrayEquals("Received 18 bytes\n".getBytes(), RestHelpers.put(MEMBRANE_URL + "topic1/key2", "TESTING2\nnext line".getBytes(StandardCharsets.UTF_8)));
		Assert.assertArrayEquals("Received 11 bytes\n".getBytes(), RestHelpers.put(MEMBRANE_URL + "topic2/key1", "topic1 key1".getBytes(StandardCharsets.UTF_8)));
		
		// Get the combined JSON documents.
		String key1String = new String(RestHelpers.get(MEMBRANE_URL + "json/key1"), StandardCharsets.UTF_8);
		JsonObject key1 = Json.parse(key1String).asObject();
		Assert.assertEquals("TESTING", key1.get("topic1").asString());
		Assert.assertEquals("topic1 key1", key1.get("topic2").asString());
		Assert.assertEquals(1, key1.get("employee_number").asInt());
		String key2String = new String(RestHelpers.get(MEMBRANE_URL + "json/key2"), StandardCharsets.UTF_8);
		JsonObject key2 = Json.parse(key2String).asObject();
		Assert.assertEquals("TESTING2\nnext line", key2.get("topic1").asString());
		Assert.assertEquals(2, key2.get("employee_number").asInt());
		
		// Shut down.
		Assert.assertArrayEquals("Shutting down\n".getBytes(), RestHelpers.delete(MEMBRANE_URL + "exit"));
	}

	/**
	 * Note that this assumes _testDotSh was already run against the same data store since it verifies overlapping data.
	 */
	private void _test2DotSh() throws Throwable {
		// Create topics which are allowed to already exist.
		Assert.assertArrayEquals("new_topic\n".getBytes(), RestHelpers.post(MEMBRANE_URL + "new_topic", _createPostMap("String", new byte[0], new byte[0], true)));
		Assert.assertArrayEquals("employee_number\n".getBytes(), RestHelpers.post(MEMBRANE_URL + "employee_number", _createPostMap("integer", new byte[0], new byte[0], true)));
		
		// Populate some data.
		Assert.assertArrayEquals("Received 4 bytes\n".getBytes(), RestHelpers.put(MEMBRANE_URL + "employee_number/new_guy", new byte[] {0,0,0,3}));
		Assert.assertArrayEquals("Received 21 bytes\n".getBytes(), RestHelpers.put(MEMBRANE_URL + "new_topic/key1", "Existing user's story".getBytes(StandardCharsets.UTF_8)));
		Assert.assertArrayEquals("Received 15 bytes\n".getBytes(), RestHelpers.put(MEMBRANE_URL + "new_topic/new_guy", "New guy's story".getBytes(StandardCharsets.UTF_8)));
		
		// Get the JSON documents for the old key as well as the new.
		String key1String = new String(RestHelpers.get(MEMBRANE_URL + "json/key1"), StandardCharsets.UTF_8);
		JsonObject key1 = Json.parse(key1String).asObject();
		Assert.assertEquals("Existing user's story", key1.get("new_topic").asString());
		Assert.assertEquals(1, key1.get("employee_number").asInt());
		String key2String = new String(RestHelpers.get(MEMBRANE_URL + "json/new_guy"), StandardCharsets.UTF_8);
		JsonObject key2 = Json.parse(key2String).asObject();
		Assert.assertEquals("New guy's story", key2.get("new_topic").asString());
		Assert.assertEquals(3, key2.get("employee_number").asInt());
		
		// Shut down.
		Assert.assertArrayEquals("Shutting down\n".getBytes(), RestHelpers.delete(MEMBRANE_URL + "exit"));
	}

	private void _testAvmDotSh() throws Throwable {
		// NOTE:  Because the AVM isn't fully modified for Laminar, keys MUST be exactly 32 bytes long.
		// First, we crudely package the test class as a JAR.
		Manifest manifest = new Manifest();
		Attributes mainAttributes = manifest.getMainAttributes();
		mainAttributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
		mainAttributes.put(Attributes.Name.MAIN_CLASS, CounterProgram.class.getName());
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		JarOutputStream jarStream = new JarOutputStream(byteStream);
		jarStream.putNextEntry(new ZipEntry(JarFile.MANIFEST_NAME));
		manifest.write(jarStream);
		jarStream.closeEntry();
		String internalName = CounterProgram.class.getCanonicalName().replace(".", "/") + ".class";
		jarStream.putNextEntry(new ZipEntry(internalName));
		InputStream resource = CounterProgram.class.getClassLoader().getResourceAsStream(internalName);
		int oneByte = resource.read();
		while(-1 != oneByte) {
			jarStream.write(oneByte);
			oneByte = resource.read();
		}
		jarStream.closeEntry();
		jarStream.close();
		byteStream.close();
		byte[] jar = byteStream.toByteArray();
		
		// Post the AVM program.
		Assert.assertArrayEquals("counter\n".getBytes(), RestHelpers.post(MEMBRANE_URL + "counter", _createPostMap("integer", jar, new byte[0], false)));
		Assert.assertArrayEquals("name\n".getBytes(), RestHelpers.post(MEMBRANE_URL + "name", _createPostMap("String", new byte[0], new byte[0], false)));
		
		// These writes to the counter topic will assign numbers for these keys, even though they aren't given any data.
		Assert.assertArrayEquals("Received 0 bytes\n".getBytes(), RestHelpers.put(MEMBRANE_URL + "counter/12345678901234567890123456789012", new byte[0]));
		Assert.assertArrayEquals("Received 0 bytes\n".getBytes(), RestHelpers.put(MEMBRANE_URL + "counter/12345678901234567890123456789013", new byte[0]));
		Assert.assertArrayEquals("Received 20 bytes\n".getBytes(), RestHelpers.put(MEMBRANE_URL + "name/12345678901234567890123456789012", "First character name".getBytes(StandardCharsets.UTF_8)));
		Assert.assertArrayEquals("Received 21 bytes\n".getBytes(), RestHelpers.put(MEMBRANE_URL + "name/12345678901234567890123456789013", "Second character name".getBytes(StandardCharsets.UTF_8)));
		
		// Get the JSON documents.
		String key1String = new String(RestHelpers.get(MEMBRANE_URL + "json/12345678901234567890123456789012"), StandardCharsets.UTF_8);
		JsonObject key1 = Json.parse(key1String).asObject();
		Assert.assertEquals("First character name", key1.get("name").asString());
		Assert.assertEquals(1, key1.get("counter").asInt());
		String key2String = new String(RestHelpers.get(MEMBRANE_URL + "json/12345678901234567890123456789013"), StandardCharsets.UTF_8);
		JsonObject key2 = Json.parse(key2String).asObject();
		Assert.assertEquals("Second character name", key2.get("name").asString());
		Assert.assertEquals(2, key2.get("counter").asInt());
		
		// Shut down.
		Assert.assertArrayEquals("Shutting down\n".getBytes(), RestHelpers.delete(MEMBRANE_URL + "exit"));
	}

	private Map<String, byte[]> _createPostMap(String type, byte[] code, byte[] arguments, boolean allowExisting) {
		Map<String, byte[]> map = new HashMap<>();
		map.put("type", type.getBytes(StandardCharsets.UTF_8));
		map.put("code", code);
		map.put("arguments", arguments);
		if (allowExisting) {
			map.put("allowExisting", new byte[0]);
		}
		return map;
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
