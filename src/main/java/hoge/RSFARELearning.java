package hoge;

import org.sat4j.specs.TimeoutException;

import algebralearning.equality.EqualityAlgebraLearnerFactory;
import algebralearning.sfa.SFAEquivalenceOracle;
import algebralearning.sfa.SFAMembershipOracle;
import automata.sfa.SFA;
import benchmark.SFAprovider;
import benchmark.algebralearning.RELearning;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;

public class RSFARELearning extends RELearning {

	public static Integer[] learnREBenchmark(Integer index) throws TimeoutException {
		Integer[] results = new Integer[8];
		
		if (index < 0 || index >= reBenchmarks.length) {
			return null; 
		}
		UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();
		SFAprovider provider = new SFAprovider(reBenchmarks[index], solver);
		SFA<CharPred, Character> targetSFA = provider.getSFA();

		SFAMembershipOracle <CharPred, Character> memb = new SFAMembershipOracle<>(targetSFA, solver);  
		SFAEquivalenceOracle <CharPred, Character> equiv = new SFAEquivalenceOracle<>(targetSFA, solver); 
		EqualityAlgebraLearnerFactory <CharPred, Character> eqFactory = new EqualityAlgebraLearnerFactory <>(solver);
		
		RSFAAlgebraLearner<CharPred, Character> learner = new RSFAAlgebraLearner<>(memb, solver, eqFactory);
		SFA<CharPred, Character> model = learner.getModelFinal(equiv);

		// The results are saved in the order that they are presented in the paper.
		results[0] = model.stateCount();
		results[1] = model.getTransitionCount();
		results[2] = memb.getDistinctQueries();
		results[3] = equiv.getDistinctCeNum();
		results[4] = equiv.getCachedCeNum();
		results[5] = learner.getNumCEGuardUpdates();
		// results[6] = learner.getNumDetCE();
		// results[7] = learner.getNumCompCE();
  		return results;
	}
}
