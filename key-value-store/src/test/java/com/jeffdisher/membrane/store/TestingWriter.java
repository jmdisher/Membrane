package com.jeffdisher.membrane.store;

import java.io.IOException;

import org.junit.Assert;

import com.jeffdisher.laminar.types.CommitInfo;
import com.jeffdisher.laminar.types.TopicName;
import com.jeffdisher.membrane.store.connection.IWritingConnection;


public class TestingWriter implements IWritingConnection {
	public boolean isClosed;
	public CommitInfo topicCreate;

	@Override
	public void close() throws IOException {
		Assert.assertFalse(this.isClosed);
		this.isClosed = true;
	}

	@Override
	public CommitInfo synchronousCreateTopic(TopicName name, byte[] code, byte[] arguments) {
		Assert.assertNotNull(this.topicCreate);
		try {
			return this.topicCreate;
		} finally {
			this.topicCreate = null;
		}
	}

	@Override
	public CommitInfo synchronousPut(TopicName name, byte[] key, byte[] value) {
		Assert.fail("Not called in test");
		return null;
	}

	@Override
	public CommitInfo synchronousDelete(TopicName name, byte[] key) {
		Assert.fail("Not called in test");
		return null;
	}
}
