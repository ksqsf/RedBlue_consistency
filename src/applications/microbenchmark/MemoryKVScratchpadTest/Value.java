package applications.microbenchmark.MemoryKVScratchpadTest;

import txstore.util.LogicalClock;

public class Value
{
	public Value(LogicalClock lc, long version, long value) {
		super();
		this.lc = lc;
		this.version = version;
		this.value = value;
	}
	LogicalClock lc;
	long version;
	long value;
}
