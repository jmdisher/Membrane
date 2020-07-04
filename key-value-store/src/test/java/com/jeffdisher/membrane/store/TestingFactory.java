package com.jeffdisher.membrane.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import com.jeffdisher.laminar.types.TopicName;
import com.jeffdisher.membrane.store.codecs.ICodec;
import com.jeffdisher.membrane.store.connection.IConnectionFactory;
import com.jeffdisher.membrane.store.connection.IReadingConnection;
import com.jeffdisher.membrane.store.connection.IWritingConnection;


public class TestingFactory implements IConnectionFactory {
	private final List<TestingReader<?,?>> _readers = new ArrayList<>();
	private TestingWriter _writer;

	@Override
	public <K, V> IReadingConnection openListener(TopicName topic, IListenerTopicShim<K, V> shim, ICodec<K> keyCodec, ICodec<V> valueCodec) throws IOException {
		TestingReader<?,?> reader = new TestingReader<>(topic, shim, keyCodec, valueCodec);
		_readers.add(reader);
		return reader;
	}

	@Override
	public IWritingConnection openWriter() throws IOException {
		// We only ever use a shared writer.
		Assert.assertNull(_writer);
		_writer = new TestingWriter();
		return _writer;
	}

	public List<TestingReader<?,?>> getReaders() {
		return _readers;
	}

	public TestingWriter getWriter() {
		Assert.assertNotNull(_writer);
		return _writer;
	}

	public void verifyClosed(int expectedReaders) {
		Assert.assertEquals(expectedReaders, _readers.size());
		for (TestingReader<?,?> reader : _readers) {
			Assert.assertTrue(reader.isClosed);
		}
		
		if (null != _writer) {
			Assert.assertTrue(_writer.isClosed);
		}
	}
}
