package com.jeffdisher.membrane.store.codecs;

import java.nio.ByteBuffer;


public class IntegerCodec implements ICodec<Integer> {
	@Override
	public Integer deserialize(byte[] bytes) {
		return ByteBuffer.wrap(bytes).getInt();
	}

	@Override
	public byte[] serialize(Integer object) {
		return ByteBuffer.allocate(Integer.BYTES).putInt(object).array();
	}
}
