package com.jeffdisher.membrane.store.laminar;

import java.io.IOException;

import com.jeffdisher.laminar.client.ClientConnection;
import com.jeffdisher.laminar.types.CommitInfo;
import com.jeffdisher.laminar.types.TopicName;
import com.jeffdisher.laminar.utils.Assert;
import com.jeffdisher.membrane.store.connection.IWritingConnection;

public class LaminarWritingConnection implements IWritingConnection {
	private final ClientConnection _client;

	public LaminarWritingConnection(ClientConnection client) {
		_client = client;
	}

	@Override
	public void close() throws IOException {
		_client.close();
	}

	@Override
	public CommitInfo synchronousCreateTopic(TopicName name, byte[] code, byte[] arguments) {
		try {
			return _client.sendCreateProgrammableTopic(name, code, arguments).waitForCommitted();
		} catch (InterruptedException e) {
			// We don't use interruption.
			throw Assert.unexpected(e);
		}
	}

	@Override
	public CommitInfo synchronousPut(TopicName name, byte[] key, byte[] value) {
		try {
			return _client.sendPut(name, key, value).waitForCommitted();
		} catch (InterruptedException e) {
			// We don't use interruption.
			throw Assert.unexpected(e);
		}
	}

	@Override
	public CommitInfo synchronousDelete(TopicName name, byte[] key) {
		try {
			return _client.sendDelete(name, key).waitForCommitted();
		} catch (InterruptedException e) {
			// We don't use interruption.
			throw Assert.unexpected(e);
		}
	}

}
