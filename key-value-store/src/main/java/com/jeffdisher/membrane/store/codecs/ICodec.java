package com.jeffdisher.membrane.store.codecs;


public interface ICodec <T> {
	T deserialize(byte[] bytes);
	byte[] serialize(T object);
}
