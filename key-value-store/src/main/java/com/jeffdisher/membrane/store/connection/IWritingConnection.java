package com.jeffdisher.membrane.store.connection;

import java.io.Closeable;

import com.jeffdisher.laminar.types.CommitInfo;
import com.jeffdisher.laminar.types.TopicName;

public interface IWritingConnection extends Closeable {

	CommitInfo synchronousCreateTopic(TopicName name, byte[] code, byte[] arguments);

	CommitInfo synchronousPut(TopicName name, byte[] key, byte[] value);

	CommitInfo synchronousDelete(TopicName name, byte[] key);

}
