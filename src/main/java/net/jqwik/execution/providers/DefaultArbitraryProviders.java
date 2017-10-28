package net.jqwik.execution.providers;

import java.util.*;

import org.junit.platform.commons.support.ReflectionSupport;

public class DefaultArbitraryProviders {


	private final static List<ArbitraryProvider> defaultProviders = new ArrayList<>();

	public static List<ArbitraryProvider> getProviders() {
		return Collections.unmodifiableList(defaultProviders);
	}

	public static void register(Class<? extends ArbitraryProvider> providerClass) {
		if (noSuchProviderYet(providerClass)) {
			defaultProviders.add(0, ReflectionSupport.newInstance(providerClass));
		}
	}

	private static boolean noSuchProviderYet(Class<? extends ArbitraryProvider> providerClass) {
		return defaultProviders.stream().allMatch(p -> p.getClass() != providerClass);
	}

}
