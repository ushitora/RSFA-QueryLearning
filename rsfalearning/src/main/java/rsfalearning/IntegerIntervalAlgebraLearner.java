package rsfalearning;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.sat4j.specs.TimeoutException;

import com.google.common.collect.ImmutableList;

import algebralearning.AlgebraLearner;
import algebralearning.oracles.EquivalenceOracle;
import algebralearning.oracles.MembershipOracle;
import theory.BooleanAlgebra;
import theory.intervals.IntPred;

public class IntegerIntervalAlgebraLearner extends AlgebraLearner<IntPred, Integer> {

	private BooleanAlgebra<IntPred, Integer> ba;
	private MembershipOracle<Integer> memb;
	private TreeMap<Integer, Boolean> examples;
	
	// [intMax, intMin]
	public IntegerIntervalAlgebraLearner(MembershipOracle<Integer> memb, BooleanAlgebra<IntPred, Integer> ba) {
		this.ba = ba;
		this.memb = memb;
		examples = new TreeMap<>();
	}

	private IntPred generateModel() {
		List<ImmutablePair<Integer, Integer>> intervals = new ArrayList<ImmutablePair<Integer, Integer>>();

		Integer left = null;
		for(Map.Entry<Integer, Boolean> e : examples.entrySet()) {
			if (e.getValue()) {
				if(left == null) {
					left = e.getKey();
				}
			}else {
				if(left != null) {
					// e.getKey() must be greater than Integer.MIN_VALUE
					Integer right = e.getKey() - 1;
					assert left <= right;
					intervals.add(ImmutablePair.of(left, right));
					left = null;
				}
			}
		}
		if(left != null) {
			intervals.add(ImmutablePair.of(left, Integer.MAX_VALUE));
		}

		return new IntPred(ImmutableList.copyOf(intervals));
	}

	@Override
	public IntPred getModel() throws TimeoutException {
		examples.put(Integer.MIN_VALUE, memb.query(Integer.MIN_VALUE));
		return generateModel();
	}

	@Override
	public IntPred updateModel(Integer counterexample) throws TimeoutException {
		assert !examples.containsKey(counterexample);
		
		Boolean mqCE = memb.query(counterexample);
		if (mqCE.equals(ba.HasModel(generateModel(), counterexample))) {
			throw new AssertionError("Provided counterexample is not a counterexample");
		}

		Integer left = examples.lowerKey(counterexample), right = counterexample;
		assert !examples.get(left).equals(mqCE);

		while(right.longValue() - left.longValue() > 1) {
			Integer mid = left / 2 + right / 2 + (left % 2 + right % 2) / 2;
			Boolean mqMid = memb.query(mid);
			if(mqMid.equals(mqCE)) {
				right = mid;
			}else {
				left = mid;
			}
		}
		examples.put(right, mqCE);
		IntPred ret = generateModel();
		assert mqCE.equals(ba.HasModel(ret, counterexample));
		return ret;
	}

	@Override
	public IntPred getModelFinal(EquivalenceOracle<IntPred, Integer> equiv) throws TimeoutException {
		Integer ce;
		IntPred model = getModel();
		while ((ce = equiv.getCounterexample(model)) != null) {
			model = updateModel(ce);
		}
		return model;
	}
}
