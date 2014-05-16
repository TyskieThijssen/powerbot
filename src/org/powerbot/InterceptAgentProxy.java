package org.powerbot;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class InterceptAgentProxy implements ClassFileTransformer {
	private static boolean registered = false;

	public static boolean isRegistered() {
		return registered;
	}

	@Override
	public byte[] transform(final ClassLoader loader, final String className, final Class<?> classBeingRedefined,
	                        final ProtectionDomain protectionDomain, final byte[] classfileBuffer) throws IllegalClassFormatException {
		return new byte[0];
	}
}