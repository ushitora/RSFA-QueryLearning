package rsfalearning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.sat4j.specs.TimeoutException;

import algebralearning.equality.EqualityAlgebraLearnerFactory;
import algebralearning.sfa.SFAAlgebraLearner;
import algebralearning.sfa.SFAEquivalenceOracle;
import algebralearning.sfa.SFAMembershipOracle;
import automata.sfa.SFA;
import benchmark.SFAprovider;
import benchmark.algebralearning.RELearning;
import theory.characters.CharPred;
import theory.intervals.UnaryCharIntervalSolver;


public class Main {
    public static void main(String[] args) {
    	// System.out.println("Total:" + Runtime.getRuntime().totalMemory() / 1024 / 1024 + "MB");
    	//runRSFAExperiment();
    	runRandomNFAExperiment();
    }
    
    private static void runRandomNFAExperiment() {
    	RandomSFAFactory factory = new RandomSFAFactory(10, 2, 2, 0.5, 0.5, 1L);
    	for(int i=0;i<100000;i++) {
    		SFA<CharPred, Character> target;
    		try {
        		target = factory.generate();
    		}catch (TimeoutException e) {
    			// try again
    			i--;
    			continue;
    		}
    		
    		assert target != null;

    		System.out.printf("%d,%d", i, target.stateCount());
    		// Deterministic SFA
    		try {
    			UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();
    			SFAMembershipOracle <CharPred, Character> memb = new SFAMembershipOracle<>(target, solver);  
    			SFAEquivalenceOracle <CharPred, Character> equiv = new SFAEquivalenceOracle<>(target, solver); 
    			EqualityAlgebraLearnerFactory <CharPred, Character> eqFactory = new EqualityAlgebraLearnerFactory <>(solver);
    			SFAAlgebraLearner <CharPred, Character> learner = new SFAAlgebraLearner<>(memb, solver, eqFactory);
    			SFA<CharPred, Character> output = learner.getModelFinal(equiv);
    			
    			Integer[] performances = {
    				output.stateCount(),
    				memb.getDistinctQueries(),
    				equiv.getDistinctCeNum(),
    				equiv.getCachedCeNum(),
    				learner.getNumCEGuardUpdates(),
    				learner.getNumCEStateUpdates(),
    				learner.getNumDetCE(),
    				learner.getNumCompCE()
    			};
    			for(Integer perf : performances) {
    				System.out.printf(",%d", perf);
    			}
    		}catch (TimeoutException e) {
    			for(int k=0;k<8;k++) {
    				System.out.printf(",-1");
    			}
    		}
    		
    		// Residual SFA
    		try {
    			UnaryCharIntervalSolver solver = new UnaryCharIntervalSolver();
    			SFAMembershipOracle <CharPred, Character> memb = new SFAMembershipOracle<>(target, solver);  
    			SFAEquivalenceOracle <CharPred, Character> equiv = new SFAEquivalenceOracle<>(target, solver); 
    			EqualityAlgebraLearnerFactory <CharPred, Character> eqFactory = new EqualityAlgebraLearnerFactory <>(solver);
    			RSFAAlgebraLearner<CharPred, Character> learner = new RSFAAlgebraLearner<>(memb, solver, eqFactory);
    			SFA<CharPred, Character> output = learner.getModelFinal(equiv);
    			
    			Integer[] performances = {
    				output.stateCount(),
    				memb.getDistinctQueries(),
    				equiv.getDistinctCeNum(),
    				equiv.getCachedCeNum(),
    				learner.getNumCEGuardUpdates(),
    				learner.getNumCETableUpdates(),
    				learner.Condition1GuardUpdates(),
    				learner.Condition1TableUpdates(),
    				learner.Condition2GuardUpdates(),
    				learner.Condition2TableUpdates(),
    				learner.Condition3GuardUpdates(),
    				learner.Condition3TableUpdates()
    			};
    			for(Integer perf : performances) {
    				System.out.printf(",%d", perf);
    			}
    		}catch (TimeoutException e) {
    			for(int k=0;k<12;k++) {
    				System.out.printf(",-1");
    			}
    		}
			System.out.printf("\n");
    		System.out.flush();
    	}
    }
    
    private static void runRSFAExperiment() {
    	System.out.println("RSFA Experiment");
    	int[] targets = {0, 1, 2, 3, 10, 11, 12, 13, 14};
    	Integer[][] results = new Integer[RSFARELearning.reBenchmarks.length][8];
	    for(int i : targets) {
	    	try {
	    		results[i] = RSFARELearning.learnREBenchmark(i);
    			for(int j=0;j<results[i].length;j++) {
    				System.out.printf(" %5d", results[i][j]);
    			}
                System.out.printf("\n");
	    	}catch (TimeoutException e) {
	            System.out.println("Timeout");
	            System.out.println(e);
	    	}
	    }
	    
	    for(int i : targets) {
		    System.out.println("i = " + i + " :");
	    	for(int j=0;j<results[i].length;j++) {
				System.out.printf(" %5d", results[i][j]);
			}
            System.out.printf("\n");
	    }
    }
    
    private static void runSFAExperiment() {
    	System.out.println("RSFA Experiment");
    	Integer[][] results = new Integer[RELearning.reBenchmarks.length][8];
	    for (int i = 0; i < RELearning.reBenchmarks.length; i ++) {
	    	try {
	    		results[i] = RELearning.learnREBenchmark(i);
    			for(int j=0;j<results[i].length;j++) {
    				System.out.printf(" %5d", results[i][j]);
    			}
                System.out.printf("\n");
	    	}catch (TimeoutException e) {
	            System.out.println("Timeout");
	            System.out.println(e);
	    	}
	    }
    }
}
