package hoge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.sat4j.specs.TimeoutException;
import benchmark.algebralearning.RELearning;


public class Hoge {
    public static void main(String[] args) {
    	System.out.println("Total:" + Runtime.getRuntime().totalMemory() / 1024 / 1024 + "MB");
    	runRSFAExperiment();
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
