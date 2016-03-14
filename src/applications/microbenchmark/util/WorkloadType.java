package applications.microbenchmark.util;

public class WorkloadType {
	
	/*
	 * READONLY : select requests
	 * WRITEONLY: update requests
	 * HEAVYREAD: majority requests in one transaction will be select
	 * HEAVYWRITE: majority requests in one transaction will be insert/update/delete
	 * RANDOM : select/insert/update/delete will be randomly determined
	 */
	public static final int READONLY = 0;
	public static final int WRITEONLY = 1;
	public static final int HEAVYREAD = 2;
	public static final int HEAVYWRITE = 3;
	public static final int RANDOM = 4;
	public static final int HALF = 5;
}
