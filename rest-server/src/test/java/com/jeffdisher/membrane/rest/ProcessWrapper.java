package com.jeffdisher.membrane.rest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;


/**
 * A very simple wrapper over an external Java process.
 * STDOUT and STDERR are inherited.
 */
public class ProcessWrapper {
	public static ProcessWrapper startJavaProcess(String jarPath, String successfulStartString, String... jarArgs) throws IOException, InterruptedException {
		String javaLauncherPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		List<String> args = new LinkedList<>();
		args.add(javaLauncherPath);
		args.add("-jar");
		args.add(jarPath);
		args.addAll(Arrays.asList(jarArgs));
		Process process = new ProcessBuilder(args)
				.redirectError(Redirect.INHERIT)
				.start()
		;
		InputStream stdout = process.getInputStream();
		CountDownLatch latch = new CountDownLatch(1);
		Thread stdoutReader = _createStreamReaderThread(System.out, latch, stdout, successfulStartString);
		stdoutReader.start();
		latch.await();
		return new ProcessWrapper(process, stdoutReader);
	}

	private static Thread _createStreamReaderThread(PrintStream output, CountDownLatch latch, InputStream stream, String filter) {
		return new Thread(() -> {
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			try {
				String line = reader.readLine();
				while (null != line) {
					output.println(line);
					if (line.contains(filter)) {
						latch.countDown();
					}
					// This will throw IOException if the stream closes.
					try {
						line = reader.readLine();
					} catch (IOException e) {
						// Stream closed.
						line = null;
					}
				}
				reader.close();
			} catch (Throwable t) {
				// We don't handle these in tests.
				Assert.fail(t.getLocalizedMessage());
			} 
		});
	}

	private final Process _process;
	private final Thread _stdoutReader;

	private ProcessWrapper(Process process, Thread stdoutReader) {
		_process = process;
		_stdoutReader = stdoutReader;
	}

	public int stop() throws InterruptedException {
		_process.destroy();
		_stdoutReader.join();
		return _process.waitFor();
	}
}
