package com.jeffdisher.membrane.store.local;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.jeffdisher.laminar.types.CommitInfo;
import com.jeffdisher.laminar.types.TopicName;
import com.jeffdisher.membrane.store.IListenerTopicShim;
import com.jeffdisher.membrane.store.codecs.ICodec;
import com.jeffdisher.membrane.store.connection.IConnectionFactory;
import com.jeffdisher.membrane.store.connection.IReadingConnection;
import com.jeffdisher.membrane.store.connection.IWritingConnection;


public class LocalFactory implements IConnectionFactory {
	private final Map<TopicName, TopicData<?,?>> _data = new ConcurrentHashMap<>();
	private final AtomicLong _intentionOffset = new AtomicLong(1L);

	@Override
	public <K, V> IReadingConnection openListener(TopicName topic, IListenerTopicShim<K, V> shim, ICodec<K> keyCodec, ICodec<V> valueCodec) throws IOException {
		_data.putIfAbsent(topic, new TopicData<>(shim, keyCodec, valueCodec));
		
		return new IReadingConnection() {
			@Override
			public void close() throws IOException {
			}
		};
	}

	@Override
	public IWritingConnection openWriter() throws IOException {
		return new IWritingConnection() {
			@Override
			public void close() throws IOException {
			}
			@Override
			public CommitInfo synchronousCreateTopic(TopicName name, byte[] code, byte[] arguments) {
				long intentionOffset = _intentionOffset.getAndIncrement();
				return _data.containsKey(name)
						? CommitInfo.create(CommitInfo.Effect.INVALID, intentionOffset)
						: CommitInfo.create(CommitInfo.Effect.VALID, intentionOffset)
				;
			}
			@Override
			public CommitInfo synchronousPut(TopicName name, byte[] key, byte[] value) {
				TopicData<?,?> data = _data.get(name);
				long intentionOffset = _intentionOffset.getAndIncrement();
				CommitInfo.Effect effect;
				if (null != data) {
					effect = CommitInfo.Effect.VALID;
					data.put(key, value, intentionOffset);
				} else {
					effect = CommitInfo.Effect.INVALID;
				}
				return CommitInfo.create(effect, intentionOffset);
			}
			@Override
			public CommitInfo synchronousDelete(TopicName name, byte[] key) {
				TopicData<?,?> data = _data.get(name);
				long intentionOffset = _intentionOffset.getAndIncrement();
				CommitInfo.Effect effect;
				if (null != data) {
					effect = CommitInfo.Effect.VALID;
					data.delete(key, intentionOffset);
				} else {
					effect = CommitInfo.Effect.INVALID;
				}
				return CommitInfo.create(effect, intentionOffset);
			}
		};
	}


	private static class TopicData<K, V> {
		private final IListenerTopicShim<K, V> _shim;
		private final ICodec<K> _keyCodec;
		private final ICodec<V> _valueCodec;
		
		public TopicData(IListenerTopicShim<K, V> shim, ICodec<K> keyCodec, ICodec<V> valueCodec) {
			_shim = shim;
			_keyCodec = keyCodec;
			_valueCodec = valueCodec;
		}
		
		public void put(byte[] key, byte[] value, long intentionOffset) {
			_shim.put(_keyCodec.deserialize(key), _valueCodec.deserialize(value), intentionOffset);
		}
		
		public void delete(byte[] key, long intentionOffset) {
			_shim.delete(_keyCodec.deserialize(key), intentionOffset);
		}
	}
}
