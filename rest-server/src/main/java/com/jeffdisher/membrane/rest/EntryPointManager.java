package com.jeffdisher.membrane.rest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;
import com.jeffdisher.laminar.types.TopicName;
import com.jeffdisher.laminar.utils.Assert;
import com.jeffdisher.membrane.store.BoundTopic;
import com.jeffdisher.membrane.store.SynchronousStore;
import com.jeffdisher.membrane.store.codecs.ICodec;
import com.jeffdisher.membrane.store.codecs.IntegerCodec;
import com.jeffdisher.membrane.store.codecs.StringCodec;


/**
 * Manages responding to and creating the handlers for the REST server.
 * Note that the given latch is counted down when a DELETE call is made to the "/exit" entry-point.
 */
public class EntryPointManager {
	private static final int VALUE_SIZE_BYTES = 32 * 1024;
	private static final StringCodec STRING_CODEC = new StringCodec();

	private final RestServer _server;
	private final SynchronousStore _store;
	private final Object _lock;
	private final Map<String, BoundTopic<String, ?>> _topics;

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
			Map<String, Object> document = _getDocument(key);
			response.setContentType("text/plain;charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);
			JsonObject root = new JsonObject();
			for (Map.Entry<String, Object> entry : document.entrySet()) {
				Object value = entry.getValue();
				// We know that the data is one of our predefined types but we don't have that information by this point.
				JsonValue json;
				if (value instanceof String) {
					json = Json.value((String) value);
				} else if (value instanceof Integer) {
					json = Json.value((Integer) value);
				} else {
					throw Assert.unreachable("Unknown type in map: " + value.getClass().getName());
				}
				root.add(entry.getKey(), json);
			}
			response.getWriter().println(root.toString(WriterConfig.PRETTY_PRINT));
		});
		_server.addPostHandler("", 1, (HttpServletResponse response, String[] variables, Map<String, byte[]> parts) -> {
			// We get the topic name from the path variables.
			String topicName = variables[0];
			// We get everything else from the multi-part post vars.
			// NOTE:  Using multi-part doesn't seem to be as common in REST as just encoding the binary as part of a
			// large JSON document and then parsing it and decoding it on the server so this may change, in the future.
			Type type = Type.mapFromString(_asString(parts.get("type")));
			byte[] code = parts.get("code");
			byte[] arguments = parts.get("arguments");
			if ((null != type) && (null != code) && (null != arguments)) {
				boolean allowExisting = (null != parts.get("allowExisting"));
				_createField(type, topicName, allowExisting);
				response.setContentType("text/plain;charset=utf-8");
				response.setStatus(HttpServletResponse.SC_OK);
				response.getWriter().println(topicName);
			} else {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("Required POST variables:  type, code, arguments");
			}
		});
	}

	/**
	 * Called in response to a top-level POST:  Creates a new field with the given name.
	 * Fundamentally, this maps into creating a new topic with this name.
	 * 
	 * @param type 
	 * @param name
	 * @param allowExisting
	 */
	private void _createField(Type type, String name, boolean allowExisting) {
		Assert.assertTrue(!name.contains("."));
		BoundTopic<String, ?> topic = _store.defineTopic(TopicName.fromString(name), new byte[0], new byte[0], STRING_CODEC, type.codec, allowExisting);
		Assert.assertTrue(null != topic);
		synchronized(_lock) {
			_topics.put(name, topic);
		}
		_server.addGetHandler("/" + name, 1, (HttpServletResponse response, String[] variables) -> {
			Object value = topic.get(variables[0]);
			if (null != value) {
				response.setContentType("text/plain;charset=utf-8");
				response.setStatus(HttpServletResponse.SC_OK);
				response.getWriter().println(value);
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
		});
		_server.addPutHandler("/" + name, 1, (HttpServletResponse response, String[] variables, InputStream inputStream) -> {
			byte[] buffer = new byte[VALUE_SIZE_BYTES];
			int readSize = inputStream.read(buffer);
			int start = 0;
			while (-1 != readSize) {
				start += readSize;
				readSize = inputStream.read(buffer, start, buffer.length - start);
			}
			byte[] value = new byte[start];
			System.arraycopy(buffer, 0, value, 0, start);
			topic.put(variables[0], value);
			response.setContentType("text/plain;charset=utf-8");
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println("Received " + start + " bytes");
		});
	}

	/**
	 * Called in response to a top-level GET to return the document elements, split across the store's topics.
	 * 
	 * @param key
	 * @return
	 */
	private Map<String, Object> _getDocument(String key) {
		return _store.readWholeDocument(key).entrySet().stream().collect(Collectors.toMap(
				entry -> entry.getKey().string,
				entry -> entry.getValue())
		);
	}

	private String _asString(byte[] bytes) {
		String string = null;
		if (null != bytes) {
			string = new String(bytes, StandardCharsets.UTF_8);
		}
		return string;
	}


	private static enum Type {
		STRING(STRING_CODEC),
		INTEGER(new IntegerCodec()),
		;
		
		public final ICodec<?> codec;
		
		private Type(ICodec<?> codec) {
			this.codec = codec;
		}
		
		public static Type mapFromString(String name) {
			Type type = null;
			if (null != name) {
				try {
					type = Type.valueOf(name.toUpperCase());
				} catch (IllegalArgumentException e) {
					// Must be an invalid name.
					type = null;
				}
			}
			return type;
		}
	}
}
