package com.jeffdisher.membrane.rest;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


/**
 * A very simple wrapper over an external Java process.
 * STDOUT and STDERR are inherited.
 */
public class ProcessWrapper {
	public static ProcessWrapper startJavaProcess(String jarPath, String... jarArgs) throws IOException {
		String javaLauncherPath = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
		List<String> args = new LinkedList<>();
		args.add(javaLauncherPath);
		args.add("-jar");
		args.add(jarPath);
		args.addAll(Arrays.asList(jarArgs));
		Process process = new ProcessBuilder(args)
				.redirectOutput(Redirect.INHERIT)
				.redirectError(Redirect.INHERIT)
				.start()
		;
		
		return new ProcessWrapper(process);
	}


	private final Process _process;

	private ProcessWrapper(Process process) {
		_process = process;
	}

	public int stop() throws InterruptedException {
		_process.destroy();
		return _process.waitFor();
	}
}
