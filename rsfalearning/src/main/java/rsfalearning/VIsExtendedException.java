package rsfalearning;

@SuppressWarnings("serial")
public class VIsExtendedException extends RuntimeException {
	public Integer addedColNum;
	
	VIsExtendedException(Integer addedNum){
		super("V is extended");
		this.addedColNum = addedNum;
	}
}
