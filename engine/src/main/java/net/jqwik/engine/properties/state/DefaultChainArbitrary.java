package net.jqwik.engine.properties.state;

import java.util.*;
import java.util.function.*;

import net.jqwik.api.*;
import net.jqwik.api.state.*;
import net.jqwik.engine.properties.arbitraries.*;
import net.jqwik.engine.support.*;

public class DefaultChainArbitrary<T> extends TypedCloneable implements ChainArbitrary<T> {

	private int size = 0;
	private final Function<Random, TransformerProvider<T>> providerGenerator;
	private final Supplier<T> initialSupplier;

	public DefaultChainArbitrary(
		Supplier<T> initialSupplier,
		List<Tuple.Tuple2<Integer, TransformerProvider<T>>> providerFrequencies
	) {
		this.initialSupplier = initialSupplier;
		this.providerGenerator = new ChooseRandomlyByFrequency<>(providerFrequencies);
	}

	@Override
	public RandomGenerator<Chain<T>> generator(int genSize) {
		final int effectiveSize =
			size != 0 ? size : (int) Math.max(Math.round(Math.sqrt(genSize)), 10);
		return random -> new ShrinkableChain<T>(random.nextLong(), initialSupplier, providerGenerator, effectiveSize);
	}

	@Override
	public ChainArbitrary<T> ofMaxSize(int size) {
		DefaultChainArbitrary<T> clone = typedClone();
		clone.size = size;
		return clone;
	}

	@Override
	public EdgeCases<Chain<T>> edgeCases(int maxEdgeCases) {
		return EdgeCases.none();
	}
}