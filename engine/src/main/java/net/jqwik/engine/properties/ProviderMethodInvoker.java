package net.jqwik.engine.properties;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import net.jqwik.api.*;
import net.jqwik.api.providers.ArbitraryProvider.*;
import net.jqwik.api.providers.*;
import net.jqwik.engine.facades.*;
import net.jqwik.engine.support.*;

import static net.jqwik.engine.support.JqwikReflectionSupport.*;

class ProviderMethodInvoker {

	ProviderMethodInvoker(Object instance, SubtypeProvider subtypeProvider) {
		this.instance = instance;
		this.subtypeProvider = subtypeProvider;
	}

	private final Object instance;
	private final SubtypeProvider subtypeProvider;

	Set<Arbitrary<?>> invoke(Method providerMethod, TypeUsage targetType) {
		List<MethodParameter> parameters = JqwikReflectionSupport.getMethodParameters(providerMethod, instance.getClass());
		Set<Function<List<Object>, Arbitrary<?>>> baseInvoker = Collections.singleton(
			argList -> invokeProviderMethod(providerMethod, argList)
		);
		Set<Supplier<Arbitrary<?>>> suppliers = createInvoker(providerMethod, targetType, baseInvoker, parameters, Collections.emptyList());
		return mapSet(suppliers, Supplier::get);
	}

	private Arbitrary<?> invokeProviderMethod(Method providerMethod, List<Object> argList) {
		return (Arbitrary<?>) invokeMethodPotentiallyOuter(providerMethod, instance, argList.toArray());
	}

	private Set<Supplier<Arbitrary<?>>> createInvoker(
		Method providerMethod,
		TypeUsage targetType,
		Set<Function<List<Object>, Arbitrary<?>>> invokers,
		List<MethodParameter> parameters,
		List<Object> args
	) {
		if (parameters.isEmpty()) {
			return mapSet(invokers, invoker -> () -> invoker.apply(args));
		}
		List<MethodParameter> newParameters = new ArrayList<>(parameters);
		MethodParameter first = newParameters.remove(0);
		if (isForAllParameter(first)) {
			TypeUsage parameterType = TypeUsageImpl.forParameter(first);
			Set<Arbitrary<?>> parameterArbitraries = subtypeProvider.apply(parameterType);
			if (parameterArbitraries.isEmpty()) {
				throw new CannotFindArbitraryException(parameterType, first.getAnnotation(ForAll.class), providerMethod);
			}
			// return parameterArbitraries.stream()
			// 						   .flatMap(arbitrary -> {
			// 							   List<Object> newArgs = new ArrayList<>(args);
			// 							   newArgs.add(null);
			// 							   return mapInvokers(invokers, invoker -> () -> invoker.apply(args)).stream();
			// 						   }).collect(Collectors.toSet());
			throw new RuntimeException("NOT YET IMPLEMENTED");
		} else {
			List<Object> newArgs = new ArrayList<>(args);
			newArgs.add(resolvePlainParameter(first.getRawParameter(), providerMethod, targetType));
			return createInvoker(providerMethod, targetType, invokers, newParameters, newArgs);
		}
	}

	private <T, R> Set<R> mapSet(Set<T> invokers, Function<T, R> mapper) {
		return invokers.stream().map(mapper).collect(Collectors.toSet());
	}

	private boolean isForAllParameter(MethodParameter parameter) {
		return parameter.isAnnotated(ForAll.class);
	}

	protected Object resolvePlainParameter(Parameter parameter, Method providerMethod, TypeUsage targetType) {
		if (parameter.getType().isAssignableFrom(TypeUsage.class)) {
			return targetType;
		} else if (parameter.getType().isAssignableFrom(SubtypeProvider.class)) {
			return subtypeProvider;
		} else {
			String message = String.format(
				"Parameter [%s] cannot be resolved in @Provide method [%s]." +
					"%nMaybe you want to add annotation `@ForAll`?",
				parameter,
				providerMethod
			);
			throw new JqwikException(message);
		}
	}
}
