package hoge;

import java.util.ArrayList;
import java.util.List;

import org.sat4j.specs.TimeoutException;

import algebralearning.oracles.MembershipOracle;

public class RSFABALearnerSimulatedMembershipOracle<D> extends MembershipOracle<D> {
	
	private RSFAObservationTable<D> table;
	private Integer srcRowIdx;
	private Integer dstRowIdx;

	public RSFABALearnerSimulatedMembershipOracle(RSFAObservationTable<D> t, Integer s, Integer d) {
		table = t;
		srcRowIdx = s;
		dstRowIdx = d;
	}
	
	public boolean query(D a) throws TimeoutException {
		List<D> qa = new ArrayList<D>(table.U.get(srcRowIdx));
		qa.add(a);
		
		ArrayList<Boolean> tempRow = table.getTempRow(qa);
		if(table.isNewPrime(tempRow)) {
			table.addRow(qa);
			throw new UIsExtendedException();
		}
		
		return table.isSubseteq(table.T.get(dstRowIdx), tempRow);
	}
}
