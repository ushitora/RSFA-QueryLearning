package hoge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.sat4j.specs.TimeoutException;

import algebralearning.oracles.MembershipOracle;
import utilities.Pair;

public class RSFAObservationTable<D> {
	public ArrayList<List<D>> U, V;
	public HashMap<List<D>, Integer> strToRowIdx, strToColIdx;
	public TreeSet<Pair<Integer, Integer>> lengthSortedRowIdx, lengthSortedColIdx;
	public ArrayList<ArrayList<Boolean>> T;
	private MembershipOracle<List<D>> oracle;
	
	public RSFAObservationTable(MembershipOracle<List<D>> m) throws TimeoutException {
		oracle = m;
		U = new ArrayList<List<D>>();
		V = new ArrayList<List<D>>();
		T = new ArrayList<ArrayList<Boolean>>();
		strToRowIdx = new HashMap<List<D>, Integer>();
		strToColIdx = new HashMap<List<D>, Integer>();
		
		Comparator<Pair<Integer, Integer>> pairComp = (p1, p2) -> {
			if(p1.getFirst().equals(p2.getFirst())) return Integer.compare(p1.getSecond(), p2.getSecond());
			return Integer.compare(p1.getFirst(), p2.getFirst());
		};
		lengthSortedRowIdx = new TreeSet<Pair<Integer, Integer>>(pairComp);
		lengthSortedColIdx = new TreeSet<Pair<Integer, Integer>>(pairComp);

		U.add(new ArrayList<D>());
		V.add(new ArrayList<D>());
		strToRowIdx.put(new ArrayList<D>(), 0);
		strToColIdx.put(new ArrayList<D>(), 0);
		lengthSortedRowIdx.add(new Pair<Integer, Integer>(0, 0));
		lengthSortedRowIdx.add(new Pair<Integer, Integer>(0, 0));

		fill();
	}
	
	private void fill() throws TimeoutException {
		while (T.size() < U.size()) {
			T.add(new ArrayList<Boolean>());
		}

		for(Integer i_u=0;i_u<U.size();i_u++) {
			for(Integer i_v=T.get(i_u).size();i_v<V.size();i_v++) {
				List<D> uv = new ArrayList<D>(U.get(i_u));
				uv.addAll(V.get(i_v));
				T.get(i_u).add(oracle.query(uv));
			}
		}
	}
	
	// a <= b
	public boolean isSubseteq(ArrayList<Boolean> a, ArrayList<Boolean> b) {
		assert a.size() == b.size();
		for(Integer i=0;i<a.size();i++) {
			if(a.get(i) && !b.get(i)) {
				return false;
			}
		}
		return true;
	}
			
	public ArrayList<Integer> getRowIndicesOfQ(){
		ArrayList<Integer> primeIndices = new ArrayList<Integer>();
		for(Integer i=0;i<U.size();i++) {
			ArrayList<Boolean> subsetUnion = new ArrayList<Boolean>(Collections.nCopies(V.size(), false));
			for(Integer j=0;j<U.size();j++) {
				if (i == j) continue;
				if (isSubseteq(T.get(j), T.get(i))) {
					assert !T.get(i).equals(T.get(j));
					for(Integer k=0;k<V.size();k++) {
						subsetUnion.set(k, (subsetUnion.get(k) || T.get(j).get(k)));
					}
				}
			}
			if (!subsetUnion.equals(T.get(i))) {
				primeIndices.add(i);
			}
		}
		
		return primeIndices;
	}

	public Set<Integer> getRowIndicesOfQ0(ArrayList<Integer> indicesOfQ){
		ArrayList<Boolean> rowEps = T.get(0);
		Set<Integer> indicesOfQ0 = new HashSet<Integer>();
		for (Integer i : indicesOfQ) {
			if (isSubseteq(T.get(i), rowEps)) {
				indicesOfQ0.add(i);
			}
		}
		return indicesOfQ0;
	}
	
	public Set<Integer> getRowIndicesOfF(ArrayList<Integer> indicesOfQ){
		Set<Integer> indicesOfF = new HashSet<Integer>();
		for (Integer i : indicesOfQ) {
			if (T.get(i).get(0)) {
				indicesOfF.add(i);
			}
		}
		return indicesOfF;
	}
	
	public ArrayList<Boolean> getTempRow(List<D> u) throws TimeoutException {
		if(strToRowIdx.containsKey(u)) {
			return T.get(strToRowIdx.get(u));
		}

		ArrayList<Boolean> tempRow = new ArrayList<Boolean>(Collections.nCopies(V.size(), false));

		for(Integer k=0;k<V.size();k++) {
			List<D> uv = new ArrayList<D>(u);
			uv.addAll(V.get(k));
			tempRow.set(k, oracle.query(uv));
		}
		return tempRow;
	}
	
	public Boolean isNewPrime(ArrayList<Boolean> tempRow) {
		for(Integer i=0;i<U.size();i++) {
			if (tempRow.equals(T.get(i))) {
				return false;
			}
		}

		ArrayList<Boolean> subsetUnion = new ArrayList<Boolean>(Collections.nCopies(V.size(), false));
		for(Integer i=0;i<U.size();i++) {
			if (isSubseteq(T.get(i), tempRow)) {
				for(Integer k=0;k<V.size();k++) {
					subsetUnion.set(k, (subsetUnion.get(k) || T.get(i).get(k)));
				}
			}
		}
		return !subsetUnion.equals(tempRow);
	}
	
	public void addRow(List<D> u) throws TimeoutException{
		List<D> copied = new ArrayList<D>(u);

		U.add(copied);
		lengthSortedRowIdx.add(new Pair<>(copied.size(), U.size() - 1));
		strToRowIdx.put(copied, U.size() - 1);

		fill();
		System.out.println("U is extended : |U| = " + U.size());
		printTable();
	}
	
	public void addCol(List<D> v) throws TimeoutException {
		List<D> copied = new ArrayList<D>(v);

		V.add(copied);
		lengthSortedColIdx.add(new Pair<>(copied.size(), V.size() - 1));
		strToColIdx.put(copied, V.size() - 1);

		fill();
		System.out.println("V is extended : |V| = " + V.size());
		printTable();
	}
	
	public void addColAllSuf(List<D> v) throws TimeoutException {
		List<D> suffix = new LinkedList<D>(v);
		while(!suffix.isEmpty()) {
			if (!strToColIdx.containsKey(suffix)) {
				List<D> copied = new ArrayList<D>(suffix);
				
				V.add(copied);
				lengthSortedColIdx.add(new Pair<>(copied.size(), V.size() - 1));
				strToColIdx.put(copied, V.size() - 1);
			}
			suffix.remove(0);
		}
		fill();
		System.out.println("V is extended : |V| = " + V.size());
		printTable();
	}
	
	public void printTable() {
		for(Integer rowIdx=0;rowIdx<U.size();rowIdx++) {
			System.out.println("U[" + rowIdx + "] = " + U.get(rowIdx));
		}
		for(Integer colIdx=0;colIdx<V.size();colIdx++) {
			System.out.println("V[" + colIdx + "] = " + V.get(colIdx));
		}

		for(ArrayList<Boolean> row : T) {
			StringBuilder sb = new StringBuilder();
			for(Boolean b : row) {
				if(b) {
					sb.append('+');
				}else {
					sb.append('-');
				}
			}
			System.out.println(sb.toString());			
		}
	}
}
