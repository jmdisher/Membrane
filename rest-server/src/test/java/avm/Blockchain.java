package avm;


/**
 * A fake stand-in for the AVM's Blockchain class just to get sample programs to compile.
 * This is here instead of bringing in the entire AVM userlib and tooling as a dependency since we don't need it for the
 * existing test.
 * This is NOT packaged with the test but just exists so it can compile cleanly as these are the same symbols the AVM
 * exposes, on which it depends.
 */
public class Blockchain {
	public static byte[] getData() {
		return null;
	}
	public static void putStorage(byte[] key, byte[] value) throws IllegalArgumentException {
	}
}
