package org.socionicasys.analyst.predicates;

import org.socionicasys.analyst.types.Aspect;

import java.util.Arrays;

// Activates the predicate if the given aspect is evaluatory

public class SituationalPredicate extends PositionPredicate {
	public SituationalPredicate(Aspect aspect) {
		super(aspect, Arrays.asList(2, 3, 6, 7));
	}

	@Override
	public String toString() {
		return "Situational";
	}
}
