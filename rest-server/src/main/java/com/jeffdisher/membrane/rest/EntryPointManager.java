package com.jeffdisher.membrane.rest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import com.jeffdisher.laminar.types.TopicName;
import com.jeffdisher.laminar.utils.Assert;
import com.jeffdisher.membrane.store.BoundTopic;
import com.jeffdisher.membrane.store.SynchronousStore;
import com.jeffdisher.membrane.store.codecs.StringCodec;


/**
 * Manages responding to and creating the handlers for the REST server.
 * Note that the given latch is counted down when a DELETE call is made to the "/exit" entry-point.
 */
public class EntryPointManager {
	private final RestServer _server;
	private final SynchronousStore _store;
	private final Object _lock;
	private final Map<String, BoundTopic<String, String>> _topics;

	public EntryPointManager(CountDownLatch stopLatch, RestServer server, SynchronousStore store) {
		_server = server;
		_store = store;
		_lock = new Object();
		_topics = new HashMap<>();
		
		// Install handlers for getting whole documents and posting new fields.
		_server.addDeleteHandler("/exit", 0, (HttpServletResponse response, String[] variables) -> {
			response.setContentType("text/plain;charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println("Shutting down");
			stopLatch.countDown();
		});
		_server.addGetHandler("", 1, (HttpServletResponse response, String[] variables) -> {
			String key = variables[0];
			Map<String, String> document = _getDocument(key);
			response.setContentType("text/plain;charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);
			JsonObject root = new JsonObject();
			for (Map.Entry<String, String> entry : document.entrySet()) {
				root.add(entry.getKey(), entry.getValue());
			}
			response.getWriter().println(root.toString(WriterConfig.PRETTY_PRINT));
		});
		_server.addPostHandler("", 1, (HttpServletResponse response, String[] variables) -> {
			String topicName = variables[0];
			_createField(topicName);
			response.setContentType("text/plain;charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println(topicName);
		});
	}

	/**
	 * Called in response to a top-level POST:  Creates a new field with the given name.
	 * Fundamentally, this maps into creating a new topic with this name.
	 * 
	 * @param name
	 */
	private void _createField(String name) {
		Assert.assertTrue(!name.contains("."));
		BoundTopic<String, String> topic = _store.defineTopic(TopicName.fromString(name), new byte[0], new byte[0], new StringCodec(), new StringCodec());
		Assert.assertTrue(null != topic);
		synchronized(_lock) {
			_topics.put(name, topic);
		}
		_server.addGetHandler("/" + name, 1, (HttpServletResponse response, String[] variables) -> {
			String value = topic.get(variables[0]);
			if (null != value) {
				response.setContentType("text/plain;charset=utf-8");
				response.setStatus(HttpServletResponse.SC_OK);
				response.getWriter().println(value);
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
		});
		_server.addPutHandler("/" + name, 1, (HttpServletResponse response, String[] variables, InputStream inputStream) -> {
			StringBuilder builder = new StringBuilder();
			byte[] buffer = new byte[1024];
			int readSize = inputStream.read(buffer);
			while (-1 != readSize) {
				builder.append(new String(buffer, 0, readSize, StandardCharsets.UTF_8));
				readSize = inputStream.read(buffer);
			}
			String wholeDocument = builder.toString();
			topic.put(variables[0], wholeDocument);
			response.setContentType("text/plain;charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println(wholeDocument);
		});
	}

	/**
	 * Called in response to a top-level GET to return the document elements, split across the store's topics.
	 * 
	 * @param key
	 * @return
	 */
	private Map<String, String> _getDocument(String key) {
		return _store.readWholeDocument(key).entrySet().stream().collect(Collectors.toMap(
				entry -> entry.getKey().string,
				entry -> (String)entry.getValue())
		);
	}
}
