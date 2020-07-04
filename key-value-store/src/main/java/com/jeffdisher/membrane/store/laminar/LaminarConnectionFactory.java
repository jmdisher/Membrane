package com.jeffdisher.membrane.store.laminar;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.jeffdisher.laminar.client.ClientConnection;
import com.jeffdisher.laminar.types.TopicName;
import com.jeffdisher.laminar.utils.Assert;
import com.jeffdisher.membrane.store.IListenerTopicShim;
import com.jeffdisher.membrane.store.codecs.ICodec;
import com.jeffdisher.membrane.store.connection.IConnectionFactory;
import com.jeffdisher.membrane.store.connection.IReadingConnection;
import com.jeffdisher.membrane.store.connection.IWritingConnection;


public class LaminarConnectionFactory implements IConnectionFactory {
	private final InetSocketAddress _server;

	public LaminarConnectionFactory(InetSocketAddress server) {
		_server = server;
	}

	@Override
	public <K, V> IReadingConnection openListener(TopicName topic, IListenerTopicShim<K, V> shim, ICodec<K> keyCodec, ICodec<V> valueCodec) throws IOException {
		return new TopicListener<K, V>(_server, topic, shim, keyCodec, valueCodec);
	}

	@Override
	public IWritingConnection openWriter() throws IOException {
		ClientConnection client = ClientConnection.open(_server);
		try {
			client.waitForConnectionOrFailure();
		} catch (IOException e) {
			throw e;
		} catch (InterruptedException e) {
			// We don't use interruption.
			throw Assert.unexpected(e);
		}
		return new LaminarWritingConnection(client);
	}
}
