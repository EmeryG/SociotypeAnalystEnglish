package org.socionicasys.analyst.predicates;

import org.socionicasys.analyst.types.Aspect;
import org.socionicasys.analyst.types.Sociotype;
import org.socionicasys.analyst.util.EqualsUtil;
import org.socionicasys.analyst.util.HashUtil;

/**
 * Предикат исполняется, если функции с заданными аспектами находятся в одном блоке.
 */
public class BlockPredicate implements Predicate {
	/**
	 * Первый аспект блока.
	 */
	private final Aspect sourceAspect;

	/**
	 * Второй аспект блока.
	 */
	private final Aspect destinationAspect;

	public BlockPredicate(Aspect sourceAspect, Aspect destinationAspect) {
		this.sourceAspect = sourceAspect;
		this.destinationAspect = destinationAspect;
	}

	@Override
	public CheckResult check(Sociotype sociotype) {
		int sourcePosition = sociotype.getFunctionByAspect(sourceAspect).getPosition();
		int destinationPosition = sociotype.getFunctionByAspect(destinationAspect).getPosition();
		int minPosition = Math.min(sourcePosition, destinationPosition);
		// Функции находятся в одном блоке, если их индексы отличаются на 1 и меньший из них — нечетный
		return CheckResult.fromBoolean(Math.abs(sourcePosition - destinationPosition) == 1 && minPosition % 2 != 0);
	}

	@Override
	public String toString() {
		return String.format("%s-%s", sourceAspect, destinationAspect);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof BlockPredicate)) {
			return false;
		}

		BlockPredicate otherPredicate = (BlockPredicate) obj;
		return EqualsUtil.areEqual(sourceAspect, otherPredicate.sourceAspect)
			&& EqualsUtil.areEqual(destinationAspect, otherPredicate.destinationAspect);
	}

	@Override
	public int hashCode() {
		HashUtil hashUtil = new HashUtil();
		hashUtil.hash(sourceAspect);
		hashUtil.hash(destinationAspect);
		return hashUtil.getComputedHash();
	}
}
