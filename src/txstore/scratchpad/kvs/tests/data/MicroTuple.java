package txstore.scratchpad.kvs.tests.data;

public class MicroTuple
{
	public MicroTuple() {
		// do nothing
	}
	public MicroTuple(long a, long b, int c, int d, String e) {
		super();
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;
	}
	public long a;
	public long b;
	public int c;
	public int d;
	public String e;
	@Override
	public String toString() {
		return "MicroTuple [a=" + a + ", b=" + b + ", c=" + c + ", d=" + d + ", e=" + e + "]";
	}
	
	public boolean equals( Object obj) {
		return obj instanceof MicroTuple && ((MicroTuple)obj).a == a && ((MicroTuple)obj).b == b &&
		((MicroTuple)obj).c == c && ((MicroTuple)obj).d == d && ((MicroTuple)obj).e.equals(e);
	}
}
