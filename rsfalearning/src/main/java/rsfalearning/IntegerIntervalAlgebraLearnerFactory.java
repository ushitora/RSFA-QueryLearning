package rsfalearning;

import algebralearning.AlgebraLearner;
import algebralearning.AlgebraLearnerFactory;
import algebralearning.oracles.MembershipOracle;
import theory.BooleanAlgebra;
import theory.intervals.IntPred;

public class IntegerIntervalAlgebraLearnerFactory extends AlgebraLearnerFactory<IntPred, Integer> {

	private BooleanAlgebra<IntPred, Integer> ba;
	
	public IntegerIntervalAlgebraLearnerFactory(BooleanAlgebra<IntPred, Integer> ba) {
		this.ba = ba;
	}
	
	@Override
	public AlgebraLearner<IntPred, Integer> getBALearner(MembershipOracle<Integer> m) {
		return new IntegerIntervalAlgebraLearner(m, ba);
	}
}
