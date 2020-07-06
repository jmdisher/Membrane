package com.jeffdisher.membrane.store;

import com.jeffdisher.laminar.types.CommitInfo;
import com.jeffdisher.laminar.types.TopicName;
import com.jeffdisher.membrane.store.codecs.ICodec;
import com.jeffdisher.membrane.store.connection.IWritingConnection;


public class BoundTopic<K, V> {
	private final IWritingConnection _sharedWriter;
	private final TopicName _topic;
	private final IClientTopicShim<K, V> _shim;
	private final ICodec<K> _keyCodec;

	public BoundTopic(IWritingConnection sharedWriter, TopicName topic, IClientTopicShim<K, V> shim, ICodec<K> keyCodec) {
		_sharedWriter = sharedWriter;
		_topic = topic;
		_shim = shim;
		_keyCodec = keyCodec;
	}

	public boolean put(K key, byte[] rawValue) {
		CommitInfo info = _sharedWriter.synchronousPut(_topic, _keyCodec.serialize(key), rawValue);
		boolean didPut = false;
		if (CommitInfo.Effect.VALID == info.effect) {
			_shim.updateIntentionOffset(info.intentionOffset);
			didPut = true;
		}
		return didPut;
	}

	public boolean delete(K key) {
		CommitInfo info = _sharedWriter.synchronousDelete(_topic, _keyCodec.serialize(key));
		boolean didDelete = false;
		if (CommitInfo.Effect.VALID == info.effect) {
			_shim.updateIntentionOffset(info.intentionOffset);
			didDelete = true;
		}
		return didDelete;
	}

	public V get(K key) {
		return _shim.get(key);
	}
}
