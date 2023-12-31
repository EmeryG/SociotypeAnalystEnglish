package org.socionicasys.analyst.predicates;

import org.socionicasys.analyst.types.Aspect;
import org.socionicasys.analyst.types.Sociotype;
import org.socionicasys.analyst.util.EqualsUtil;
import org.socionicasys.analyst.util.HashUtil;

/**
 * Предикат исполняется, если функция с заданным аспектом имеет размерность не ниже указанной.
 */
public class DimensionPredicate implements Predicate {
	/**
	 * Аспект предиката.
	 */
	private final Aspect aspect;

	/**
	 * Минимальная размерность предиката.
	 */
	private final int dimension;

	public DimensionPredicate(Aspect aspect, int dimension) {
		if (dimension >= 1 && dimension <= 4) {
			this.dimension = dimension;
		} else {
			throw new IllegalArgumentException("Illegal dimension for DimensionPredicate");
		}
		this.aspect = aspect;
	}

	/**
	 * Возвращает название размерности по числовому значению размерности.
	 *
	 * @param dimension размерность, от 1 до 4
	 * @return название размерности
	 */
	public static String getDimensionName(int dimension) {
		switch (dimension) {
		case 1:
			return "Ex";
		case 2:
			return "Nr";
		case 3:
			return "St";
		case 4:
			return "Tm";
		default:
			throw new IllegalArgumentException("Invalid dimension");
		}
	}

	@Override
	public CheckResult check(Sociotype sociotype) {
		return CheckResult.fromBoolean(sociotype.getFunctionByAspect(aspect).getDimension() >= dimension);
	}

	@Override
	public String toString() {
		return getDimensionName(dimension);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DimensionPredicate)) {
			return false;
		}

		DimensionPredicate otherPredicate = (DimensionPredicate) obj;
		return EqualsUtil.areEqual(aspect, otherPredicate.aspect) && dimension == otherPredicate.dimension;
	}

	@Override
	public int hashCode() {
		HashUtil hashUtil = new HashUtil();
		hashUtil.hash(aspect);
		hashUtil.hash(dimension);
		return hashUtil.getComputedHash();
	}
}
