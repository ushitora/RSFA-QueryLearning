package rsfalearning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import org.sat4j.specs.TimeoutException;

import automata.sfa.SFA;
import automata.sfa.SFAEpsilon;
import automata.sfa.SFAInputMove;
import automata.sfa.SFAMove;
import theory.BooleanAlgebra;
import theory.intervals.IntPred;
import theory.intervals.IntegerSolver;

public class RandomIntegerIntervalSFAFactory {

	private Integer stateNum;
	private Integer transitionNum;
	private Integer actualIntervalNum;
	private Double initailStateProbability;
	private Double finalStateProbability;
	private Random random;
	private BooleanAlgebra<IntPred, Integer> ba;
	
	// [intMin, intMax]
	public RandomIntegerIntervalSFAFactory(Integer stateNum, Integer transitionNum, Integer actualIntervalNum,
			Double initailStateProbability, Double finalStateProbability, Long randomSeed) {
		this.stateNum = Integer.valueOf(stateNum);
		this.transitionNum = Integer.valueOf(transitionNum);
		this.actualIntervalNum = Integer.valueOf(actualIntervalNum);
		this.initailStateProbability = Double.valueOf(initailStateProbability);
		this.finalStateProbability = Double.valueOf(finalStateProbability);
		this.random = new Random(randomSeed);
		ba = new IntegerSolver();
	}
	
	public SFA<IntPred, Integer> generate() throws TimeoutException{
		final Integer pseudoInitialStateIdx = stateNum;
		List<SFAMove<IntPred, Integer>> transitions = new ArrayList<>();
		List<Integer> finalStates = new ArrayList<>();

		ArrayList<Integer> borders = new ArrayList<>();
		for(int i=0;i<2 * actualIntervalNum;i++) {
			borders.add(random.nextInt());
		}
		Collections.sort(borders);

		ArrayList<IntPred> actualIntervals = new ArrayList<>();
		for(int i=0;i<actualIntervalNum;i++) {
			actualIntervals.add(new IntPred(borders.get(2 * i), borders.get(2 * i + 1)));
		}

		for(Integer stateIdx=0;stateIdx<stateNum;stateIdx++) {
			if (random.nextDouble() < initailStateProbability) {
				transitions.add(new SFAEpsilon<IntPred, Integer>(pseudoInitialStateIdx, stateIdx));
			}
			if (random.nextDouble() < finalStateProbability) {
				finalStates.add(stateIdx);
			}

			Map<Integer, List<IntPred>> edge = new HashMap<>();
			for(Integer i=0;i<transitionNum;i++) {
				Integer to = random.nextInt(stateNum);
				IntPred interval = actualIntervals.get(random.nextInt(actualIntervalNum));
				if (!edge.containsKey(to)) {
					edge.put(to, new ArrayList<>());
				}
				edge.get(to).add(interval);
			}

			for(Entry<Integer, List<IntPred>> entry : edge.entrySet()) {
				IntPred phi = ba.False();
				for (IntPred interval : entry.getValue()) {
					phi = ba.MkOr(interval, phi);
				}
				transitions.add(new SFAInputMove<IntPred, Integer>(stateIdx, entry.getKey(), phi));
			}
		}

		return SFA.MkSFA(transitions, pseudoInitialStateIdx, finalStates, ba, true, false, true).determinize(ba).minimize(ba);
	}
}