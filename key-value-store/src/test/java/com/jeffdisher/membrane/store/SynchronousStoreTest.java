package com.jeffdisher.membrane.store;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.jeffdisher.laminar.types.CommitInfo;
import com.jeffdisher.laminar.types.TopicName;
import com.jeffdisher.membrane.store.codecs.ICodec;
import com.jeffdisher.membrane.store.codecs.StringCodec;


public class SynchronousStoreTest {
	@Test
	public void testStartStop() throws Throwable {
		TestingFactory factory = new TestingFactory();
		TopicName topic = TopicName.fromString("test");
		ICodec<String> keyCodec = new StringCodec();
		ICodec<String> valueCodec = new StringCodec();
		SynchronousStore store = new SynchronousStore(factory);
		store.attachToExistingTopic(topic, keyCodec, valueCodec);
		Assert.assertEquals(1, factory.getReaders().size());
		store.close();
		factory.verifyClosed(1);
	}

	@Test
	public void testCreateTopic() throws Throwable {
		TestingFactory factory = new TestingFactory();
		TopicName topic = TopicName.fromString("test");
		ICodec<String> keyCodec = new StringCodec();
		ICodec<String> valueCodec = new StringCodec();
		SynchronousStore store = new SynchronousStore(factory);
		byte[] code = new byte[] {1};
		byte[] arguments = new byte[] {2};
		factory.getWriter().topicCreate = CommitInfo.create(CommitInfo.Effect.VALID, 1L);
		store.defineTopic(topic, code, arguments, keyCodec, valueCodec);
		Assert.assertEquals(1, factory.getReaders().size());
		store.close();
		factory.verifyClosed(1);
	}

	@Test
	public void testListenToData() throws Throwable {
		TestingFactory factory = new TestingFactory();
		TopicName topic = TopicName.fromString("test");
		ICodec<String> keyCodec = new StringCodec();
		ICodec<String> valueCodec = new StringCodec();
		SynchronousStore store = new SynchronousStore(factory);
		BoundTopic<String, String> bound = store.attachToExistingTopic(topic, keyCodec, valueCodec);
		Assert.assertEquals(1, factory.getReaders().size());
		TestingReader<?,?> reader = factory.getReaders().get(0);
		// The reader is expected to be on a background thread so run the callbacks that way.
		Thread thread = new Thread(()->{
			reader.shim.create(1L);
			reader.putString("key", "value1", 2L);
			reader.putString("key", "value2", 3L);
			reader.putString("key2", "final", 4L);
		});
		thread.start();
		thread.join();
		
		Assert.assertEquals("value2", bound.get("key"));
		Assert.assertEquals("final", bound.get("key2"));
		Map<TopicName, Object> document = store.readWholeDocument("key");
		Assert.assertEquals(1, document.size());
		Assert.assertEquals("value2", document.get(topic));
		Assert.assertEquals(0, store.readWholeDocument("notFound").size());
		
		store.close();
		factory.verifyClosed(1);
	}

	@Test
	public void testMultipleDocuments() throws Throwable {
		TestingFactory factory = new TestingFactory();
		TopicName topic1 = TopicName.fromString("topic1");
		TopicName topic2 = TopicName.fromString("topic2");
		ICodec<String> keyCodec = new StringCodec();
		ICodec<String> valueCodec = new StringCodec();
		SynchronousStore store = new SynchronousStore(factory);
		BoundTopic<String, String> bound1 = store.attachToExistingTopic(topic1, keyCodec, valueCodec);
		BoundTopic<String, String> bound2 = store.attachToExistingTopic(topic2, keyCodec, valueCodec);
		Assert.assertEquals(2, factory.getReaders().size());
		
		// The reader is expected to be on a background thread so run the callbacks that way.
		TestingReader<?,?> reader1 = factory.getReaders().get(0);
		Thread thread1 = new Thread(()->{
			reader1.shim.create(1L);
			reader1.putString("key", "value1", 2L);
			reader1.putString("key", "value2", 3L);
			reader1.putString("key2", "final", 4L);
		});
		thread1.start();
		thread1.join();
		
		// The reader is expected to be on a background thread so run the callbacks that way.
		TestingReader<?,?> reader2 = factory.getReaders().get(1);
		Thread thread2 = new Thread(()->{
			reader2.shim.create(5L);
			reader2.putString("key", "value1", 6L);
			reader2.putString("key", "value2", 7L);
			reader2.deleteString("key", 8L);
			reader2.putString("key2", "other", 9L);
		});
		thread2.start();
		thread2.join();
		
		Assert.assertEquals("value2", bound1.get("key"));
		Assert.assertNull(bound2.get("key"));
		Map<TopicName, Object> document1 = store.readWholeDocument("key");
		Assert.assertEquals(1, document1.size());
		Assert.assertEquals("value2", document1.get(topic1));
		Map<TopicName, Object> document2 = store.readWholeDocument("key2");
		Assert.assertEquals(2, document2.size());
		Assert.assertEquals("final", document2.get(topic1));
		Assert.assertEquals("other", document2.get(topic2));
		
		store.close();
		factory.verifyClosed(2);
	}
}
