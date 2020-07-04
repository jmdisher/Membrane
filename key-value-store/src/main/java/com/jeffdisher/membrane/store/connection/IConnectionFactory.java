package com.jeffdisher.membrane.store.connection;

import java.io.IOException;

import com.jeffdisher.laminar.types.TopicName;
import com.jeffdisher.membrane.store.IListenerTopicShim;
import com.jeffdisher.membrane.store.codecs.ICodec;


public interface IConnectionFactory {
	<K, V> IReadingConnection openListener(TopicName topic, IListenerTopicShim<K, V> shim, ICodec<K> keyCodec, ICodec<V> valueCodec) throws IOException;
	IWritingConnection openWriter() throws IOException;
}
