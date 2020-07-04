package com.jeffdisher.membrane.store;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;

import com.jeffdisher.laminar.types.TopicName;
import com.jeffdisher.membrane.store.codecs.ICodec;
import com.jeffdisher.membrane.store.connection.IReadingConnection;


public class TestingReader<K, V> implements IReadingConnection {
	public final TopicName topic;
	public final IListenerTopicShim<K, V> shim;
	public final ICodec<K> keyCodec;
	public final ICodec<V> valueCodec;

	public boolean isClosed;

	public TestingReader(TopicName topic, IListenerTopicShim<K, V> shim, ICodec<K> keyCodec, ICodec<V> valueCodec) {
		this.topic = topic;
		this.shim = shim;
		this.keyCodec = keyCodec;
		this.valueCodec = valueCodec;
	}

	@Override
	public void close() throws IOException {
		Assert.assertFalse(this.isClosed);
		this.isClosed = true;
	}

	public void putString(String keyString, String valueString, long intentionOffset) {
		K key = this.keyCodec.deserialize(keyString.getBytes(StandardCharsets.UTF_8));
		V value = this.valueCodec.deserialize(valueString.getBytes(StandardCharsets.UTF_8));
		this.shim.put(key, value, intentionOffset);
	}

	public void deleteString(String keyString, long intentionOffset) {
		K key = this.keyCodec.deserialize(keyString.getBytes(StandardCharsets.UTF_8));
		this.shim.delete(key, intentionOffset);
	}
}
