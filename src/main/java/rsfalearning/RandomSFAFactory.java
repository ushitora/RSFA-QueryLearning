package rsfalearning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.sat4j.specs.TimeoutException;

import automata.sfa.SFA;
import automata.sfa.SFAEpsilon;
import automata.sfa.SFAInputMove;
import automata.sfa.SFAMove;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;

public class RandomSFAFactory {

	private Integer stateNum;
	private Integer transitionNum;
	private Integer actualCharSize;
	private Double initailStateProbability;
	private Double finalStateProbability;
	private Random random;
	private UnaryCharIntervalSolver ba;
	
	public RandomSFAFactory(Integer stateNum, Integer transitionNum, Integer actualCharSize,
			Double initailStateProbability, Double finalStateProbability, Long randomSeed) {
		this.stateNum = Integer.valueOf(stateNum);
		this.transitionNum = Integer.valueOf(transitionNum);
		this.actualCharSize = Integer.valueOf(actualCharSize);
		this.initailStateProbability = Double.valueOf(initailStateProbability);
		this.finalStateProbability = Double.valueOf(finalStateProbability);
		this.random = new Random(randomSeed);
		ba = new UnaryCharIntervalSolver();
	}
	
	public SFA<CharPred, Character> generate() throws TimeoutException{
		final Integer pseudoInitialStateIdx = stateNum;
		List<SFAMove<CharPred, Character>> transitions = new ArrayList<>();
		List<Integer> finalStates = new ArrayList<>();

		ArrayList<Character> actualChars = new ArrayList<>();
		for(int i=0;i<=Character.MAX_VALUE;i++) {
			actualChars.add((char)i);
		}
		Collections.shuffle(actualChars, random);
		actualChars.subList(actualCharSize, actualChars.size()).clear();
		assert actualCharSize.equals(actualChars.size());

		for(Integer stateIdx=0;stateIdx<stateNum;stateIdx++) {
			if (random.nextDouble() < initailStateProbability) {
				transitions.add(new SFAEpsilon<CharPred, Character>(pseudoInitialStateIdx, stateIdx));
			}
			if (random.nextDouble() < finalStateProbability) {
				finalStates.add(stateIdx);
			}

			Map<Integer, List<Character>> edge = new HashMap<>();
			for(Integer i=0;i<transitionNum;i++) {
				Integer to = random.nextInt(stateNum);
				Character c = actualChars.get(random.nextInt(actualCharSize));
				if (!edge.containsKey(to)) {
					edge.put(to, new ArrayList<>());
				}
				edge.get(to).add(c);
			}
			
			for(Entry<Integer, List<Character>> entry : edge.entrySet()) {
				CharPred phi = ba.False();
				for (Character c : entry.getValue()) {
					phi = ba.MkOr(ba.MkAtom(c), phi);
				}
				transitions.add(new SFAInputMove<CharPred, Character>(stateIdx, entry.getKey(), phi));
			}
		}

		return SFA.MkSFA(transitions, pseudoInitialStateIdx, finalStates, ba, true, false, true).determinize(ba).minimize(ba);
	}
}
