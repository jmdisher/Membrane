package com.jeffdisher.membrane.store;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.jeffdisher.laminar.types.CommitInfo;
import com.jeffdisher.laminar.types.TopicName;
import com.jeffdisher.laminar.utils.Assert;
import com.jeffdisher.membrane.store.codecs.ICodec;
import com.jeffdisher.membrane.store.connection.IConnectionFactory;
import com.jeffdisher.membrane.store.connection.IReadingConnection;
import com.jeffdisher.membrane.store.connection.IWritingConnection;


public class SynchronousStore implements Closeable {
	private final Object _lock;
	private final IConnectionFactory _factory;
	private final IWritingConnection _client;
	private final Map<TopicName, TopicData<?, ?>> _topics;

	public SynchronousStore(IConnectionFactory factory) throws IOException {
		_lock = new Object();
		_factory = factory;
		_client = _factory.openWriter();
		_topics = new HashMap<>();
	}

	@Override
	public void close() throws IOException {
		_client.close();
		for (TopicData<?, ?> data : _topics.values()) {
			data.listener.close();
		}
	}

	public <K, V> BoundTopic<K, V> defineTopic(TopicName name, byte[] code, byte[] arguments, ICodec<K> keyCodec, ICodec<V> valueCodec) {
		// Try to create the topic (we accept failure here since it may already exist).
		// We don't update our commit offset since this only changes validity of calls, not consistent local data.
		boolean didCreate = (CommitInfo.Effect.VALID == _client.synchronousCreateTopic(name, code, arguments).effect);
		BoundTopic<K, V> bound = null;
		if (didCreate) {
			bound = _registerTopic(name, keyCodec, valueCodec);
		}
		return bound;
	}

	public <K, V> BoundTopic<K, V> attachToExistingTopic(TopicName name, ICodec<K> keyCodec, ICodec<V> valueCodec) {
		return _registerTopic(name, keyCodec, valueCodec);
	}

	public Map<TopicName, Object> readWholeDocument(Object key) {
		synchronized(_lock) {
			Map<TopicName, Object> document = new HashMap<>();
			for (Map.Entry<TopicName, TopicData<?,?>> entry : _topics.entrySet()) {
				TopicData<?,?> data = _topics.get(entry.getKey());
				_waitForReadSync(_lock, data);
				Object value = data.map.get(key);
				if (null != value) {
					document.put(entry.getKey(), value);
				}
			}
			return document;
		}
	}


	private <K, V> BoundTopic<K, V> _registerTopic(TopicName name, ICodec<K> keyCodec, ICodec<V> valueCodec) {
		TopicData<K, V> data = new TopicData<>();
		try {
			data.listener = _factory.openListener(name, new ListenerShim<>(_lock, data), keyCodec, valueCodec);
		} catch (IOException e) {
			// This is a demonstration, so we don't want to handle this and hide usage errors (although we could just fail to create the BoundTopic).
			throw Assert.unexpected(e);
		}
		BoundTopic<K, V> bound = new BoundTopic<K, V>(_client, name, new LockShim<K, V>(_lock, data), keyCodec, valueCodec);
		synchronized(_lock) {
			TopicData<?, ?> removed = _topics.put(name, data);
			// We don't handle this error - it is just incorrect usage.
			Assert.assertTrue(null == removed);
		}
		return bound;
	}

	private static void _waitForReadSync(Object heldLock, TopicData<?, ?> data) {
		long initialWrittenIndex = data.lastWrittenIntentionOffset;
		while (data.lastReadIntentionOffset < initialWrittenIndex) {
			try {
				heldLock.wait();
			} catch (InterruptedException e) {
				// We don't use interruption.
				throw Assert.unexpected(e);
			}
		}
	}


	private static class ListenerShim<K, V> implements IListenerTopicShim<K, V> {
		private final Object _sharedLock;
		private final TopicData<K, V> _data;
		
		public ListenerShim(Object sharedLock, TopicData<K, V> data) {
			_sharedLock = sharedLock;
			_data = data;
		}
		
		@Override
		public void delete(K key, long intentionOffset) {
			synchronized (_sharedLock) {
				_data.map.remove(key);
				_data.lastReadIntentionOffset = intentionOffset;
				_sharedLock.notifyAll();
			}
		}
		
		@Override
		public void put(K key, V value, long intentionOffset) {
			synchronized (_sharedLock) {
				_data.map.put(key, value);
				_data.lastReadIntentionOffset = intentionOffset;
				_sharedLock.notifyAll();
			}
		}
		
		@Override
		public void create(long intentionOffset) {
			// Just verify the map is empty.
			synchronized (_sharedLock) {
				Assert.assertTrue(_data.map.isEmpty());
			}
		}
		
		@Override
		public void destroy(long intentionOffset) {
			// Clear the map.
			synchronized (_sharedLock) {
				_data.map.clear();
			}
		}
	}


	private static class LockShim<K, V> implements IClientTopicShim<K, V> {
		private final Object _sharedLock;
		private final TopicData<K, V> _data;
		
		public LockShim(Object sharedLock, TopicData<K, V> data) {
			_sharedLock = sharedLock;
			_data = data;
		}
		
		@Override
		public void updateIntentionOffset(long offset) {
			synchronized (_sharedLock) {
				_data.lastWrittenIntentionOffset = offset;
				// Nobody wait for the written index to increase so we just return.
			}
		}
		
		@Override
		public V get(K key) {
			synchronized (_sharedLock) {
				_waitForReadSync(_sharedLock, _data);
				return _data.map.get(key);
			}
		}
	}


	private static class TopicData<K, V> {
		public final Map<K, V> map;
		public long lastWrittenIntentionOffset;
		public long lastReadIntentionOffset;
		public IReadingConnection listener;
		
		public TopicData() {
			this.map = new HashMap<>();
			this.lastWrittenIntentionOffset = 0L;
			this.lastReadIntentionOffset = 0L;
		}
	}
}
