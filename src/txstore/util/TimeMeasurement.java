/***************************************************************
Project name: txstore
Class file name: TimeMeasurement.java
Created at 2:44:03 PM by chengli

Copyright (c) 2014 chengli.
All rights reserved. This program and the accompanying materials
are made available under the terms of the GNU Public License v2.0
which accompanies this distribution, and is available at
http://www.gnu.org/licenses/old-licenses/gpl-2.0.html

Contributors:
    chengli - initial API and implementation

Contact:
    To distribute or use this code requires prior specific permission.
    In this case, please contact chengli@mpi-sws.org.
****************************************************************/

package txstore.util;

// TODO: Auto-generated Javadoc
/**
 * The Class TimeMeasurement.
 *
 * @author chengli
 */
public class TimeMeasurement {
	
	/** The execution str. */
	public static String EXECUTION_STR = "EXECUTION";
	
	/** The classification str. */
	public static String CLASSIFICATION_STR = "CLASSIFYICATION";
	
	/** The commit str. */
	public static String COMMIT_STR = "COMMT";
	
	/** The replication str. */
	public static String REPLICATION_STR = "REPLICATION";
	
	/** The Constant EXECUTION. */
	public final static byte EXECUTION = 1;
	
	/** The Constant CLASSIFICATION. */
	public final static byte CLASSIFICATION = 2;
	
	/** The Constant COMMIT. */
	public final static byte COMMIT = 3;
	
	/** The Constant REPLICATION. */
	public final static byte REPLICATION = 4;
	
	
	/** The Constant READONLY. */
	public final static byte READONLY = 11;
	
	/** The Constant UPDATE. */
	public final static byte UPDATE = 12;
	
	/**
	 * Gets the current time in ns.
	 *
	 * @return the current time in ns
	 */
	public static long getCurrentTimeInNS() {
		return System.nanoTime();
	}
	
	/**
	 * Compute latency in ms.
	 *
	 * @param startTime the start time
	 * @return the double
	 */
	public static double computeLatencyInMS(long startTime) {
		long endTime = System.nanoTime();
		double latency = (endTime - startTime) * 0.000001;
		return latency;
	}
	
	/**
	 * Gets the task name string.
	 *
	 * @param taskType the task type
	 * @return the task name string
	 */
	public static String getTaskNameString(byte taskType) {
		switch(taskType) {
		case EXECUTION:
			return EXECUTION_STR;
		case CLASSIFICATION:
			return CLASSIFICATION_STR;
		case COMMIT:
			return COMMIT_STR;
		case REPLICATION:
			return REPLICATION_STR;
			default:
				throw new RuntimeException("The task type is not support " + taskType);
		}
	}
	
	/**
	 * Gets the read only or update string.
	 *
	 * @param txnType the txn type
	 * @return the read only or update string
	 */
	public static String getReadOnlyOrUpdateString(byte txnType) {
		switch(txnType) {
		case READONLY:
			return "read";
		case UPDATE:
			return "update";
			default:
				throw new RuntimeException("This txnType is not support " + txnType);
		}
	}
	
	/**
	 * Output latency string.
	 *
	 * @param startTime the start time
	 * @param taskType the task type
	 * @param readonlyOrUpate the readonly or upate
	 */
	public static void outputLatencyString(long startTime, byte taskType, 
			byte readonlyOrUpate) {
		double latency = computeLatencyInMS(startTime);
		String taskName = getTaskNameString(taskType);
		String readOrUpdateStr = getReadOnlyOrUpdateString(readonlyOrUpate);
		String outputStr = taskName + " " + latency + " " + readOrUpdateStr;
		System.out.println(outputStr);
	}

}
