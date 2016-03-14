package txstore.scratchpad.kvs;

import txstore.util.LogicalClock;

public class ScratchpadTuple
{
//	public transient boolean changed;
	public boolean del;
	public long ts;
	public String clock;
	public transient LogicalClock lc;
	public Object object;
	public transient boolean registered;
	public ScratchpadTuple() {
		del = false;
//		changed = true;
	}
	public ScratchpadTuple(boolean del, long ts, String clock, Object object) {
		super();
		this.del = del;
		this.ts = ts;
		this.clock = clock;
		this.object = object;
		this.lc = new LogicalClock(clock);
	}
	public ScratchpadTuple(boolean del, long ts, LogicalClock lc, Object object) {
		super();
		this.del = del;
		this.ts = ts;
		this.clock = lc.toString();
		this.object = object;
		this.lc = lc;
	}
	public boolean isDel() {
		return del;
	}
	public void setDel(boolean del) {
		this.del = del;
	}
	public long getTs() {
		return ts;
	}
	public void setTs(long ts) {
		this.ts = ts;
	}
	public String getClock() {
		return clock;
	}
	public void setClock(String clock) {
		this.clock = clock;
		this.lc = new LogicalClock(clock);
	}
	public Object getObject() {
		return object;
	}
	public void setObject(Object object) {
		this.object = object;
	}
	public LogicalClock getLc() {
		return lc;
	}
	public void setLc(LogicalClock lc) {
		this.lc = lc;
		clock = lc.toString();
	}

}
