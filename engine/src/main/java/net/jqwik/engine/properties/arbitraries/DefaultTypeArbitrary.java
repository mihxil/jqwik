package net.jqwik.engine.properties.arbitraries;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;

import org.junit.platform.commons.support.*;

import net.jqwik.api.*;
import net.jqwik.api.arbitraries.*;
import net.jqwik.api.constraints.*;
import net.jqwik.api.providers.*;

import static org.junit.platform.commons.support.ModifierSupport.*;

import static net.jqwik.engine.discovery.JqwikKotlinSupport.*;

public class DefaultTypeArbitrary<T> extends ArbitraryDecorator<T> implements TypeArbitrary<T> {

	private final Class<T> targetType;
	private final Set<Executable> explicitCreators = new HashSet<>();
	private final Set<Predicate<? super Constructor<?>>> constructorFilters = new HashSet<>();
	private final Set<Predicate<Method>> factoryMethodFilters = new HashSet<>();
	private boolean defaultsSet = true;

	private TraverseArbitrary<T> traverseArbitrary;

	public DefaultTypeArbitrary(Class<T> targetType) {
		this.targetType = targetType;
		this.addPublicConstructors();
		this.addPublicFactoryMethods();
		TraverseArbitrary.Traverser traverser = new TypeTraverser();
		this.traverseArbitrary = new DefaultTraverseArbitrary<>(targetType, traverser);
	}

	@Override
	protected Arbitrary<T> arbitrary() {
		return traverseArbitrary;
	}

	@Override
	public TypeArbitrary<T> use(Executable creator) {
		DefaultTypeArbitrary<T> clone = cloneWithoutDefaultsSet();
		clone.explicitCreators.add(creator);
		// clone.traverseArbitrary = clone.traverseArbitrary.use(creator);
		return clone;
	}

	private DefaultTypeArbitrary<T> cloneWithoutDefaultsSet() {
		DefaultTypeArbitrary<T> clone = typedClone();
		if (clone.defaultsSet) {
			clone.explicitCreators.clear();
			clone.constructorFilters.clear();
			clone.factoryMethodFilters.clear();
			clone.defaultsSet = false;
		}
		return clone;
	}

	@Override
	public TypeArbitrary<T> useConstructors(Predicate<? super Constructor<?>> filter) {
		DefaultTypeArbitrary<T> clone = cloneWithoutDefaultsSet();
		clone.addConstructors(filter);
		// clone.traverseArbitrary = clone.traverseArbitrary.useConstructors(filter);
		return clone;
	}

	private void addConstructors(Predicate<? super Constructor<?>> filter) {
		constructorFilters.add(filter);
	}


	@Override
	public TypeArbitrary<T> usePublicConstructors() {
		DefaultTypeArbitrary<T> clone = cloneWithoutDefaultsSet();
		clone.addPublicConstructors();
		// clone.traverseArbitrary = clone.traverseArbitrary.usePublicConstructors();
		return clone;
	}

	private void addPublicConstructors() {
		addConstructors(ModifierSupport::isPublic);
	}

	@Override
	public TypeArbitrary<T> useAllConstructors() {
		DefaultTypeArbitrary<T> clone = cloneWithoutDefaultsSet();
		clone.addAllConstructors();
		// clone.traverseArbitrary = clone.traverseArbitrary.useAllConstructors();
		return clone;
	}

	private void addAllConstructors() {
		addConstructors(ignore -> true);
	}

	@Override
	public TypeArbitrary<T> useFactoryMethods(Predicate<Method> filter) {
		DefaultTypeArbitrary<T> clone = cloneWithoutDefaultsSet();
		clone.addFactoryMethods(filter);
		// clone.traverseArbitrary = clone.traverseArbitrary.useFactoryMethods(filter);
		return clone;
	}

	private void addFactoryMethods(Predicate<Method> filter) {
		factoryMethodFilters.add(filter);
	}

	@Override
	public TypeArbitrary<T> usePublicFactoryMethods() {
		DefaultTypeArbitrary<T> clone = cloneWithoutDefaultsSet();
		clone.addPublicFactoryMethods();
		// clone.traverseArbitrary = clone.traverseArbitrary.usePublicFactoryMethods();
		return clone;
	}

	private void addPublicFactoryMethods() {
		addFactoryMethods(ModifierSupport::isPublic);
	}

	@Override
	public TypeArbitrary<T> useAllFactoryMethods() {
		DefaultTypeArbitrary<T> clone = cloneWithoutDefaultsSet();
		clone.addAllFactoryMethods();
		// clone.traverseArbitrary = clone.traverseArbitrary.useAllFactoryMethods();
		return clone;
	}

	private void addAllFactoryMethods() {
		addFactoryMethods(any -> true);
	}


	@Override
	public TypeArbitrary<T> allowRecursion() {
		DefaultTypeArbitrary<T> clone = typedClone();
		clone.traverseArbitrary = clone.traverseArbitrary.enableRecursion();
		return clone;
	}

	@Override
	public String toString() {
		return String.format("TypeArbitrary<%s>", traverseArbitrary);
	}

	class TypeTraverser implements TraverseArbitrary.Traverser {

		@Override
		public Set<Executable> findCreators(TypeUsage target) {
			Set<Executable> creators = new HashSet<>();
			if (target.isOfType(targetType)) {
				creators.addAll(explicitCreators);
			}
			for (Predicate<Method> filter : factoryMethodFilters) {
				appendFactoryMethods(creators, target, filter);
			}
			for (Predicate<? super Constructor<?>> filter : constructorFilters) {
				appendConstructors(creators, target, filter);
			}
			return creators;
		}

		private void appendConstructors(Set<Executable> creators, TypeUsage target, Predicate<? super Constructor<?>> filter) {
			if (isAbstract(target.getRawType())) {
				return;
			}
			Arrays.stream(target.getRawType().getDeclaredConstructors())
				  .filter(constructor -> !isOverloadedConstructor(constructor))
				  .filter(filter)
				  .forEach(creators::add);

		}

		private void appendFactoryMethods(Set<Executable> creators, TypeUsage target, Predicate<Method> filter) {
			Arrays.stream(target.getRawType().getDeclaredMethods())
				  .filter(ModifierSupport::isStatic)
				  .filter(creator -> hasFittingReturnType(creator, target))
				  .filter(filter)
				  .forEach(creators::add);
		}

		private boolean hasFittingReturnType(Executable creator, TypeUsage target) {
			TypeUsage returnType = TypeUsage.forType(creator.getAnnotatedReturnType().getType());
			return returnType.canBeAssignedTo(target);
		}

	}
}
