package hoge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sat4j.specs.TimeoutException;

import algebralearning.AlgebraLearner;
import algebralearning.AlgebraLearnerFactory;
import algebralearning.oracles.EquivalenceOracle;
import algebralearning.oracles.MembershipOracle;
import automata.Move;
import automata.sfa.SFA;
import automata.sfa.SFAEpsilon;
import automata.sfa.SFAInputMove;
import automata.sfa.SFAMove;
import theory.BooleanAlgebra;
import utilities.Pair;


public class RSFAAlgebraLearner<P, D> extends AlgebraLearner <SFA <P,D>, List <D>> {
	
	private RSFAObservationTable<D> table;
    private MembershipOracle <List <D>> membOracle;

    private SFA <P,D> model;
	private ArrayList<Integer> stateIdxToRowIdx;
	private Set<Integer> rowIdxQ0;
	private Set<Integer> rowIdxF;

	private Integer pseudoInitialStateIdx;
	private List<Integer> finalStates;
	private ArrayList<SFAMove <P,D>> transitions; 
	private HashMap<Pair<Integer, Integer>, Integer> rowIdxPairToTransitionIdx;
    
    private HashMap <Pair<Integer,Integer>, AlgebraLearner <P,D>> algebraLearners;
    private HashMap <Pair<Integer, Integer>, P> modelGuards;
    private BooleanAlgebra <P,D> ba;
    private AlgebraLearnerFactory <P,D> baLearnerFactory;
	private Hashtable <String, Integer> perfCounters;
	
    public RSFAAlgebraLearner(MembershipOracle<List<D>> m, BooleanAlgebra<P,D> b, AlgebraLearnerFactory <P,D> balf) {
    	table = null;
    	membOracle = m;
    	
    	model = null;
    	stateIdxToRowIdx = null;
    	rowIdxQ0 = null;
    	rowIdxF = null;

		algebraLearners = new HashMap <>();
		modelGuards = new HashMap<>();
		ba = b;
		baLearnerFactory = balf;
		perfCounters = new Hashtable <>();

		perfCounters.put("CEGuardUpdates", 0);
		perfCounters.put("CETableUpdates", 0);
		perfCounters.put("Condition1GuardUpdates", 0);
		perfCounters.put("Condition1TableUpdates", 0);
		perfCounters.put("Condition2GuardUpdates", 0);
		perfCounters.put("Condition2TableUpdates", 0);
		perfCounters.put("Condition3GuardUpdates", 0);
		perfCounters.put("Condition3TableUpdates", 0);
    }

    private void incPerfCounter(String key) {
		if (!perfCounters.containsKey(key)) {
			throw new AssertionError("Invalid performance counter requested");
		}
		perfCounters.put(key, perfCounters.get(key) + 1);
		return;
	}
	
	private SFA <P,D> copyModelClean() throws TimeoutException {    		    	
		return SFA.MkSFA(model.getTransitions(), model.getInitialState(), model.getFinalStates(), ba);    	
	}
    
	private void updateTransition(Integer srcStateIdx, Integer dstStateIdx, D ce) throws TimeoutException {
		Integer srcRowIdx = stateIdxToRowIdx.get(srcStateIdx);
		Integer dstRowIdx = stateIdxToRowIdx.get(dstStateIdx);
		
		Pair<Integer, Integer> rowIdxPair = new Pair<>(srcRowIdx, dstRowIdx);
		AlgebraLearner<P,D> learner = algebraLearners.get(rowIdxPair);
		assert learner != null;
		
		{
			P phi = modelGuards.get(rowIdxPair);
			P psi = learner.getModel();
			assert ba.AreEquivalent(phi, psi);
		}

		P phi = learner.updateModel(ce);
		modelGuards.put(rowIdxPair, phi);

		if (!ba.AreEquivalent(phi, ba.False())) {
			if (rowIdxPairToTransitionIdx.containsKey(rowIdxPair)) {
				transitions.set(rowIdxPairToTransitionIdx.get(rowIdxPair), new SFAInputMove<P, D>(srcStateIdx, dstStateIdx, phi));
			}else {
				transitions.add(new SFAInputMove<P, D>(srcStateIdx, dstStateIdx, phi));
				rowIdxPairToTransitionIdx.put(rowIdxPair, transitions.size() - 1);
			}
		}else {
			if (rowIdxPairToTransitionIdx.containsKey(rowIdxPair)) {
				transitions.remove((int)rowIdxPairToTransitionIdx.get(rowIdxPair));
				rowIdxPairToTransitionIdx.remove(rowIdxPair);
			}
		}

		model = SFA.MkSFA(transitions, pseudoInitialStateIdx, finalStates, ba, false, false, true);
	}
	
	private Boolean checkCondition1() throws TimeoutException {
		assert !stateIdxToRowIdx.isEmpty();
		for(Integer stateIdxq1=0;stateIdxq1<stateIdxToRowIdx.size();stateIdxq1++) {
			Integer rowIdxq1 = stateIdxToRowIdx.get(stateIdxq1);
			for(Integer stateIdxq2=0;stateIdxq2<stateIdxToRowIdx.size();stateIdxq2++) {
				Integer rowIdxq2 = stateIdxToRowIdx.get(stateIdxq2);

				if (stateIdxq1.equals(stateIdxq2)) continue;
				if (!table.isSubseteq(table.T.get(rowIdxq1), table.T.get(rowIdxq2))) continue;
				
				for(Integer stateIdxx=0;stateIdxx<stateIdxToRowIdx.size();stateIdxx++) {
					Integer rowIdxx = stateIdxToRowIdx.get(stateIdxx);

					P phi1 = modelGuards.get(new Pair<Integer, Integer>(rowIdxq1, rowIdxx));
					P phi2 = modelGuards.get(new Pair<Integer, Integer>(rowIdxq2, rowIdxx));
					
					P phi1AndNotphi2 = ba.MkAnd(phi1, ba.MkNot(phi2));
					if (!ba.IsSatisfiable(phi1AndNotphi2)) {
    					continue;
    				}
					
					D a = ba.generateWitness(phi1AndNotphi2);
					assert ba.HasModel(phi1, a);
					assert !ba.HasModel(phi2, a);
					
					ArrayList<Boolean> rowx = table.T.get(rowIdxx);
					
					List<D> q1a = new ArrayList<D>(table.U.get(rowIdxq1));
					q1a.add(a);
					if (!table.isSubseteq(rowx, table.getTempRow(q1a))) {
						updateTransition(stateIdxq1, stateIdxx, a);
						incPerfCounter("Condition1GuardUpdates");
						return false;
					}
					
					List<D> q2a = new ArrayList<D>(table.U.get(rowIdxq2));
					q2a.add(a);
					ArrayList<Boolean> rowq2a = table.getTempRow(q2a);
					if (table.isSubseteq(rowx, rowq2a)) {
						updateTransition(stateIdxq2, stateIdxx, a);
						incPerfCounter("Condition1GuardUpdates");
						return false;
					}
					
					for(Integer k=0;k<rowx.size();k++) {
						if (rowx.get(k) && !rowq2a.get(k)) {
							List<D> av = new ArrayList<D>();
							av.add(a);
							av.addAll(table.V.get(k));
							
							table.addCol(av);
							incPerfCounter("Condition1TableUpdates");
							throw new VIsExtendedException();
						}
					}
					
					// never reach this
					assert false;
				}
			}
		}
		return true;
	}
	
	private Boolean checkCondition2() throws TimeoutException {
		for(Pair<Integer, Integer> p : table.lengthSortedRowIdx) {
			// skip epsilon
			if(p.getFirst().equals(0)) continue;

			Integer rowIdxu1 = p.getSecond();
			Collection<Integer> states = model.getReachedStates(model.getInitialState(), table.U.get(rowIdxu1), ba);
			for(Integer stateIdxx1 : states) {
				if (stateIdxx1.equals(pseudoInitialStateIdx)) continue;
				// stateIdx : x1 in d(Q_0, u1)
				Integer rowIdxx1 = stateIdxToRowIdx.get(stateIdxx1);
				if(!table.isSubseteq(table.T.get(rowIdxx1), table.T.get(rowIdxu1))) {
					List<D> u2 = new ArrayList<D>(table.U.get(rowIdxu1));
					D a = u2.get(u2.size() - 1);
					u2.remove(u2.size() - 1);
					// u1 = u2a
					
					Collection<Integer> statesByu2 = model.getReachedStates(model.getInitialState(), u2, ba);
					for(Integer stateIdxx2 : statesByu2) {
						if (stateIdxx2.equals(pseudoInitialStateIdx)) continue;
						// stateIdxx2 : x2 in d(Q_0, u2)
						Collection<Move<P, D>> moves = model.getMovesFrom(stateIdxx2);
						for(Move<P, D> m : moves) {
							// check whether x1 in d(x2, a)
							if(m.to.equals(stateIdxx1) && m.hasModel(a, ba)) {
								Integer rowIdxx2 = stateIdxToRowIdx.get(stateIdxx2);
								List<D> x2a = new ArrayList<D>(table.U.get(rowIdxx2));
								x2a.add(a);

								ArrayList<Boolean> rowx2a = table.getTempRow(x2a);
								if(!table.isSubseteq(table.T.get(rowIdxx1), rowx2a)) {
									updateTransition(stateIdxx2, stateIdxx1, a);
									incPerfCounter("Condition2GuardUpdates");
									return false;
								}
								
								ArrayList<Boolean> rowu1 = table.T.get(rowIdxu1);
								for(Integer k=0;k<rowu1.size();k++) {
									if (rowx2a.get(k) && !rowu1.get(k)) {
										List<D> av = new ArrayList<D>();
										av.add(a);
										av.addAll(table.V.get(k));
										
										table.addCol(av);
										incPerfCounter("Condition2TableUpdates");
										throw new VIsExtendedException();
									}
								}

								// never reach this
								assert false;
							}
						}
					}
					
					// never reach this
					assert false;
				}
			}
		}
		return true;
	}

	private Boolean checkCondition3() throws TimeoutException {
		for(Pair<Integer, Integer> p : table.lengthSortedColIdx) {
			// skip epsilon
			if(p.getFirst().equals(0)) continue;

			Integer colIdxv1 = p.getSecond();
			
			List<D> v2 = new ArrayList<D>(table.V.get(colIdxv1));
			D a = v2.get(0);
			v2.remove(0);
			
			Integer colIdxv2 = table.strToColIdx.get(v2);
			// prefix-closed
			assert colIdxv2 != null;
			
			for(Integer stateIdxq=0;stateIdxq<stateIdxToRowIdx.size();stateIdxq++) {
				Integer rowIdxq = stateIdxToRowIdx.get(stateIdxq);
				if(!table.T.get(rowIdxq).get(colIdxv1)) {
					for(Move<P, D> m : model.getMovesFrom(stateIdxq)) {
						if(!m.hasModel(a, ba)) continue;
						Integer stateIdxx = m.to;
						Integer rowIdxx = stateIdxToRowIdx.get(stateIdxx);
						if(table.T.get(rowIdxx).get(colIdxv2)) {
							updateTransition(stateIdxq, stateIdxx, a);
							incPerfCounter("Condition3GuardUpdates");
							return false;
						}
					}
				}else {
					Boolean forallCondition = true;
					for(Move<P, D> m : model.getMovesFrom(stateIdxq)) {
						if(!m.hasModel(a, ba)) continue;
						Integer stateIdxx = m.to;
						Integer rowIdxx = stateIdxToRowIdx.get(stateIdxx);
						if (table.T.get(rowIdxx).get(colIdxv2)) {
							forallCondition = false;
							break;
						}
					}
					if(forallCondition) {
						List<D> qa = new ArrayList<D>(table.U.get(rowIdxq));
						qa.add(a);
						ArrayList<Boolean> rowqa = table.getTempRow(qa);
						
						for(Integer stateIdxx=0;stateIdxx<stateIdxToRowIdx.size();stateIdxx++) {
							Integer rowIdxx = stateIdxToRowIdx.get(stateIdxx);
							ArrayList<Boolean> rowx = table.T.get(rowIdxx);
							if (rowx.get(colIdxv2) && table.isSubseteq(rowx, rowqa)) {
								updateTransition(stateIdxq, stateIdxx, a);
								incPerfCounter("Condition3GuardUpdates");
								return false;
							}
						}
						
						table.addRow(qa);
						incPerfCounter("Condition3TableUpdates");
						throw new UIsExtendedException();
					}
				}
				
			}
		}
		return true;
	}
	
	private void checkConditions() throws TimeoutException {
		if(model == null) {
			return;
		}

		while(true) {
			try {
				if (!checkCondition1()) continue;
				if (!checkCondition2()) continue;
				if (!checkCondition3()) continue;
			}catch (UIsExtendedException e) {
				model = null;
			}catch (VIsExtendedException e) {
				model = null;
				// TODO : Currently, waste all leaner.
				algebraLearners = new HashMap<>();
				modelGuards = new HashMap<>();
			}
			break;
		}
	}
	
	private SFA<P,D> constructModel() throws TimeoutException {
		while(model == null) {
			// build_model
	    	stateIdxToRowIdx = table.getRowIndicesOfQ();
	    	rowIdxQ0 = table.getRowIndicesOfQ0(stateIdxToRowIdx);
	    	rowIdxF = table.getRowIndicesOfF(stateIdxToRowIdx);

	    	pseudoInitialStateIdx = stateIdxToRowIdx.size();

	    	finalStates = new LinkedList <Integer>();
			for(int stateIdx=0;stateIdx<stateIdxToRowIdx.size();stateIdx++) {
				if (rowIdxF.contains(stateIdxToRowIdx.get(stateIdx))) {
					finalStates.add(stateIdx);
				}
			}

			transitions = new ArrayList <>();
	    	for(int stateIdx=0;stateIdx<stateIdxToRowIdx.size();stateIdx++) {
				if (rowIdxQ0.contains(stateIdxToRowIdx.get(stateIdx))) {
					transitions.add(new SFAEpsilon<P, D>(pseudoInitialStateIdx, stateIdx));
				}
			}

			rowIdxPairToTransitionIdx = new HashMap<>();
			try {
		    	for(int srcStateIdx=0;srcStateIdx<stateIdxToRowIdx.size();srcStateIdx++) {
		    		Integer srcRowIdx = stateIdxToRowIdx.get(srcStateIdx);
		    		for(int dstStateIdx=0;dstStateIdx<stateIdxToRowIdx.size();dstStateIdx++) {
		    			Integer dstRowIdx = stateIdxToRowIdx.get(dstStateIdx);

		    			Pair<Integer, Integer> rowIdxPair = new Pair<Integer, Integer>(srcRowIdx, dstRowIdx);
		    			P phi;
		    			if (modelGuards.containsKey(rowIdxPair)) {
		    				phi = modelGuards.get(rowIdxPair);
		    			}else {
			    			RSFABALearnerSimulatedMembershipOracle<D> simOracle = 
			        				new RSFABALearnerSimulatedMembershipOracle<D>(table, srcRowIdx, dstRowIdx);
							AlgebraLearner<P,D> learner = 
									baLearnerFactory.getBALearner(simOracle);
							
							// update_transition
							phi = learner.getModel();
							assert phi != null;
							
							algebraLearners.put(rowIdxPair, learner);
							modelGuards.put(rowIdxPair, phi);
		    			}
		    			if (!ba.AreEquivalent(phi, ba.False())) {
							transitions.add(new SFAInputMove<P, D>(srcStateIdx, dstStateIdx, phi));
							rowIdxPairToTransitionIdx.put(rowIdxPair, transitions.size() - 1);
						}
		    		}
		    	}
			}catch (UIsExtendedException e){
				model = null;
				continue;
			}

			model = SFA.MkSFA(transitions, pseudoInitialStateIdx, finalStates, ba, false, false, true);

			checkConditions();
		}
		
		
		
		return copyModelClean();
	}

	private void processCounterexample(List<D> w) throws TimeoutException {
		Boolean MQw = membOracle.query(w);
		Boolean lhsEps = false;
		for(Integer rowIdx : rowIdxQ0) {
			List<D> qw = new ArrayList<D>(table.U.get(rowIdx));
			qw.addAll(w);
			if(membOracle.query(qw)) {
				lhsEps = true;
				break;
			}
		}
		if(!lhsEps.equals(MQw)) {
			table.addColAllSuf(w);
			incPerfCounter("CETableUpdates");
			throw new VIsExtendedException();
		}
		
		Integer left = 0, right = w.size();
		while(right - left > 1) {
			Integer uLen = (left + right) / 2;

			List<D> u = new ArrayList<D>(w.subList(0, uLen));
			D a = w.get(uLen);
			List<D> v = new ArrayList<D>(w.subList(uLen + 1, w.size()));

			Boolean lhsu = false;
			// d(Q_0, u)
			Collection<Integer> lhsStates = model.getReachedStates(model.getInitialState(), u, ba);
			for(Integer stateIdx : lhsStates) {
				if (stateIdx.equals(pseudoInitialStateIdx)) continue;
				Integer rowIdx = stateIdxToRowIdx.get(stateIdx);
				List<D> qav = new ArrayList<D>(table.U.get(rowIdx));
				qav.add(a);
				qav.addAll(v);
				if(membOracle.query(qav)) {
					lhsu = true;
					break;
				}
			}

			if(lhsu.equals(lhsEps)) {
				left = uLen;
			}else {
				right = uLen;
			}
		}
		
		List<D> u = new ArrayList<D>(w.subList(0, left));
		D a = w.get(left);
		List<D> v = new ArrayList<D>(w.subList(left + 1, w.size()));
		
		if(lhsEps) {
			Collection<Integer> lhsStates = model.getReachedStates(model.getInitialState(), u, ba);
			for(Integer stateIdxq1 : lhsStates) {
				if (stateIdxq1.equals(pseudoInitialStateIdx)) continue;
				Integer rowIdxq1 = stateIdxToRowIdx.get(stateIdxq1);
				List<D> q1av = new ArrayList<D>(table.U.get(rowIdxq1));
				q1av.add(a);
				q1av.addAll(v);
				if(membOracle.query(q1av)) {
					// q1 in d(Q_0, u) and MQ(q1av) = +
					List<D> q1a = new ArrayList<D>(table.U.get(rowIdxq1));
					q1a.add(a);
					
					Collection<Move<P, D>> moves = model.getMovesFrom(stateIdxq1);
					
					ArrayList<Boolean> rowq1a = table.getTempRow(q1a);
					for(Integer stateIdxq2=0;stateIdxq2<stateIdxToRowIdx.size();stateIdxq2++) {
						Integer rowIdxq2 = stateIdxToRowIdx.get(stateIdxq2);
						if(table.isSubseteq(table.T.get(rowIdxq2), rowq1a)) {
							Boolean notin = true;
							for(Move<P, D> m : moves) {
								if(m.to.equals(stateIdxq2) && m.hasModel(a, ba)) {
									notin = false;
									break;
								}
							}
							if(notin) {
								updateTransition(stateIdxq1, stateIdxq2, a);
								incPerfCounter("CEGuardUpdates");
								return;
							}
						}
					}
					
					Boolean found = false;
					for(Integer rowIdxu2=0;rowIdxu2<table.U.size();rowIdxu2++) {
						if(table.T.get(rowIdxu2).equals(rowq1a)) {
							List<D> u2v = new ArrayList<D>(table.U.get(rowIdxu2));
							u2v.addAll(v);
							if(membOracle.query(u2v)) {
								found = true;
								table.addColAllSuf(v);
								break;
							}
						}
					}
					if(!found) {
						table.addRow(q1a);
						table.addColAllSuf(v);
					}
					incPerfCounter("CETableUpdates");
					throw new VIsExtendedException();
				}
			}
			// never reach this
			assert false;
		}else {
			Collection<Integer> lhsStates = model.getReachedStates(model.getInitialState(), u, ba);
			for(Integer stateIdxq1 : lhsStates) {
				if (stateIdxq1.equals(pseudoInitialStateIdx)) continue;
				Integer rowIdxq1 = stateIdxToRowIdx.get(stateIdxq1);
				List<D> q1av = new ArrayList<D>(table.U.get(rowIdxq1));
				q1av.add(a);
				q1av.addAll(v);
				if(!membOracle.query(q1av)) {
					Collection<Move<P, D>> moves = model.getMovesFrom(stateIdxq1);
					for(Move<P, D> m : moves) {
						if (m.hasModel(a, ba)) {
							Integer stateIdxq2 = m.to;
							Integer rowIdxq2 = stateIdxToRowIdx.get(stateIdxq2);
							List<D> q2v = new ArrayList<D>(table.U.get(rowIdxq2));
							q2v.addAll(v);
							
							if (membOracle.query(q2v)) {
								// MQ(q1av) = - and MQ(q2v) = +
								List<D> q1a = q1av.subList(0, table.U.get(rowIdxq1).size() + 1);
								ArrayList<Boolean> rowq1a = table.getTempRow(q1a);
								ArrayList<Boolean> rowq2 = table.T.get(rowIdxq2);
								if(!table.isSubseteq(rowq2, rowq1a)) {
									updateTransition(stateIdxq1, stateIdxq2, a);
									incPerfCounter("CEGuardUpdates");
									return;
								}else {
									table.addColAllSuf(v);
									incPerfCounter("CETableUpdates");
									throw new VIsExtendedException();									
								}
							}
						}
					}
				}
			}
			// never reach this
			assert false;
		}
	}
	
    /************************** Public Methods  ****************************/

    public Integer getNumCETableUpdates() { 
		return perfCounters.get("CETableUpdates");
    }
    
    public Integer getNumCEGuardUpdates() {
		return perfCounters.get("CEGuardUpdates");
    }
    
    public Integer Condition1GuardUpdates() {
		return perfCounters.get("Condition1GuardUpdates");
    }

    public Integer Condition1TableUpdates() {
		return perfCounters.get("Condition1TableUpdates");
	}

    public Integer Condition2GuardUpdates() {
		return perfCounters.get("Condition2GuardUpdates");
	}
	
	public Integer Condition2TableUpdates() {
		return perfCounters.get("Condition2TableUpdates");
	}
	
	public Integer Condition3GuardUpdates() {
		return perfCounters.get("Condition3GuardUpdates");
	}
	
	public Integer Condition3TableUpdates() {
		return perfCounters.get("Condition3TableUpdates");
	}
   
    /***********  Learning API  ***********/

    public SFA <P,D> getModel() throws TimeoutException {
    	table = new RSFAObservationTable<D>(membOracle);
    	return constructModel();
    }

    public SFA <P,D> updateModel(List <D> counterexample) throws TimeoutException {
        if (model == null) {
            throw new AssertionError("UpdateModel called without first building a model");
        } else if (model.accepts(counterexample, ba) == membOracle.query(counterexample)) {
        	throw new AssertionError("Counterexample given is not a counterexample");
        }
        try {
            processCounterexample(counterexample);
            checkConditions();
		}catch (UIsExtendedException e) {
			model = null;
		}catch (VIsExtendedException e) {
			model = null;
			// TODO : Currently, waste all leaner.
			algebraLearners = new HashMap<>();
			modelGuards = new HashMap<>();
		}
        return constructModel();
    }

    public SFA <P,D> getModelFinal(EquivalenceOracle <SFA <P, D>, List <D>> equiv) throws TimeoutException {
        List<D> ce;
        SFA<P,D> cleanModel = getModel();
        while ((ce = equiv.getCounterexample(cleanModel)) != null) {
        	// System.out.println("EQ throwed : ce = " + ce);
        	cleanModel = updateModel(ce);
        }
        return cleanModel;
    }
}
