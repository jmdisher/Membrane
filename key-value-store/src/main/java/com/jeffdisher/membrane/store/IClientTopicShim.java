package com.jeffdisher.membrane.store;

public interface IClientTopicShim<K, V> {
	void updateIntentionOffset(long offset);
	V get(K key);
}
