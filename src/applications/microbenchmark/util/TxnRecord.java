package applications.microbenchmark.util;

import applications.microbenchmark.messages.TxnRepMessage;
import txstore.util.ProxyTxnId;

public class TxnRecord {
	long start;
	long end;
	int color;
	TxnRepMessage msg;
	boolean readonly;

	ProxyTxnId id;

	final static long baseTime = System.nanoTime();

	public TxnRecord(ProxyTxnId id) {
		//this.start = System.nanoTime() - baseTime;
		this.start = System.nanoTime();
		this.id = id;
		this.color = 0;
		msg = null;
		readonly = true;
	}

	public ProxyTxnId getTxnId() {

		return id;
	}

	public void setEnd(long e) {
		end = e;
	}
	
	public void setColor(int c){
		color = c;
	}
	
	public int getColor(){
		return color;
	}

	public long latency() {
		return end - start;
	}
	
	public long getStartTime(){
		return start;
	}
	
	public long getEndTime(){
		return end;
	}
	
	public void setUpdate(){
		readonly = false;
	}
	
	public boolean isReadonly(){
		return readonly;
	}

	public String toString() {
		return id + " ( " + start + " -> " + end + " ) " + latency();
	}

	public void finish() {
		setEnd(System.nanoTime() - baseTime);
	}
}