package com.jeffdisher.membrane.avm;

import avm.Blockchain;


/**
 * A VERY simple AVM demo program.  When invoked with a given key, will generate a consequence for that key with the
 * next integer in sequence (starting at 1) as the value.
 * Takes no value inputs and assigns a new number whether given a PUT or a DELETE.
 */
public class CounterProgram {
	private static int NEXT = 1;

	public static byte[] main() {
		byte[] key = Blockchain.getData();
		Blockchain.putStorage(key, _nextCounterBytes());
		return null;
	}


	private static byte[] _nextCounterBytes() {
		byte[] bytes = new byte[Integer.BYTES];
		bytes[0] = (byte)(0xff & (NEXT >> 24));
		bytes[1] = (byte)(0xff & (NEXT >> 16));
		bytes[2] = (byte)(0xff & (NEXT >> 8));
		bytes[3] = (byte)(0xff & NEXT);
		NEXT += 1;
		return bytes;
	}
}
