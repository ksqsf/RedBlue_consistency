package applications.microbenchmark.MemoryKVScratchpadTest;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.*;
import java.util.*;

import applications.simplestub.OperationList;
import txstore.scratchpad.*;
import txstore.util.LogicalClock;
import txstore.util.ProxyTxnId;
import util.Debug;

public class AppMemScratchpad
{
	MemKVScratchpadFactory factory;
	FakedMemKVWorkloadGenerator workload;
	CyclicBarrier barrier;
	CountDownLatch barrierEnd;
	
	long blue;
	long dcs;
	
	long totalTrxs;
	long totalLatency;
	
	AppMemScratchpad( MemKVScratchpadFactory factory, FakedMemKVWorkloadGenerator workload) {
		this.factory = factory;
		this.workload = workload;
		blue = 0;
		dcs = 0;
		totalTrxs = 0;
		totalLatency = 0;
	}
	
	protected synchronized void addResult( long trxs, long latency) {
		totalTrxs += trxs;
		totalLatency += latency;
	}
	
	
	protected synchronized LogicalClock nextClock( boolean isBlue) {
		if( isBlue)
			blue++;
		else
			dcs++;
		long[] darr = new long[1];
		darr[0] = dcs;
		return new LogicalClock( darr, blue);
	}
	
	protected void runThread( final int threadNum, final RemoteScratchpad pad, final long durationMSec, 
			final boolean readOnly) {
		new Thread( new Runnable() {

			@Override
			public void run() {
				try {
					long txCount = 0;
					long latencyTotal = 0;
					FakedMemKVWorkloadGenerator gen = workload.duplicate();
					pad.ping();
					
					barrier.await();
					long init = new Date().getTime();
					
					for( ; ; ) {
						OperationList list = workload.generate_commands(threadNum, readOnly);
						
						long initT = new Date().getTime();
						pad.beginTransaction( new MProxyTxnId( 0, threadNum, txCount));
						pad.execute( new OpListOperation(list));
						LogicalClock lc = nextClock( list.getColor() == 1);
						pad.commit(lc, null);
						txCount++;
						long end = new Date().getTime();
						latencyTotal = latencyTotal + end - initT;
						if( end - init > durationMSec)
							break;
					}
					addResult( txCount, latencyTotal);
					barrierEnd.countDown();
				} catch (Exception e) {
					e.printStackTrace();
					System.exit(0);
				}
				
			}
			
		}).start();
	}
	
	public void doitLocal( int nThreads, long durationMSec, boolean readOnly) throws Exception {
		barrier = new CyclicBarrier( nThreads);
		barrierEnd = new CountDownLatch( nThreads);
		RemoteScratchpad[] pads = new RemoteScratchpad[ nThreads]; 
		for( int i = 0; i < nThreads; i++) {
			try {
				pads[i] = factory.createScratchPad();
				runThread(i, pads[i], durationMSec, readOnly);	
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		barrierEnd.await();
		for( int i = 0; i < nThreads; i++) {
			try {
				pads[i].reset();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		System.out.println( "Throughput (tx/s): " + totalTrxs / (double)durationMSec * 1000);
		System.out.println( "Latency (msec): " + totalLatency / (double)totalTrxs);
	}
	
	public void doit( int nThreads, long durationMSec, boolean readOnly) throws InterruptedException {
		barrier = new CyclicBarrier( nThreads);
		barrierEnd = new CountDownLatch( nThreads);
		RemoteScratchpad[] pads = new RemoteScratchpad[ nThreads]; 
		for( int i = 0; i < nThreads; i++) {
			try {
				pads[i] = factory.createScratchPad();
				runThread(i, (RemoteScratchpad)UnicastRemoteObject.toStub(pads[i]), durationMSec, readOnly);	
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		barrierEnd.await();
		for( int i = 0; i < nThreads; i++) {
			try {
				pads[i].reset();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		System.out.println( "Throughput (tx/s): " + totalTrxs / (double)durationMSec * 1000);
		System.out.println( "Latency (msec): " + totalLatency / (double)totalTrxs);
	}
	
	public static void main( String[] args) {
		Debug.debug = false;
		int dcCount = 1;
		int numObjects = 10000;
		int maxThreads = 3;
		long durationMSec = 10000;
		boolean readOnly = false;
		double bluerate = 0.5;
		int readRequestNum = 2;
		int writeRequestNum = 2;
		
		FakedMemKVWorkloadGenerator workload = new FakedMemKVWorkloadGenerator( bluerate, readRequestNum, writeRequestNum,
				0, numObjects, 0);

		MemKVScratchpadFactory factory = new FakedMemKVScratchpadFactory( dcCount, numObjects);
		MemKVScratchpadFactory factoryDirect = new FakedMemKVDirectScratchpadFactory( dcCount, numObjects);


		System.out.println( "************* LOCAL ********************************");
		System.out.println( "memory key-value store scratchpad");
		try {
			for( int i = 1; i <= maxThreads; i++) {
				AppMemScratchpad pad = new AppMemScratchpad( factory, workload);
				pad.doitLocal( i, durationMSec, readOnly);
				pad = null;
				System.gc();
			}
		} catch( Exception e) {
			e.printStackTrace();
		}

		System.out.println( "memory key-value store direct");
		try {
			for( int i = 1; i <= maxThreads; i++) {
				AppMemScratchpad pad = new AppMemScratchpad( factoryDirect, workload);
				pad.doitLocal( i, durationMSec, readOnly);
				pad = null;
				System.gc();
			}
		} catch( Exception e) {
			e.printStackTrace();
		}

		System.out.println( "************* REMOTE ********************************");
		System.out.println( "memory key-value store scratchpad");
		try {
			for( int i = 1; i <= maxThreads; i++) {
				AppMemScratchpad pad = new AppMemScratchpad( factory, workload);
				pad.doit( i, durationMSec, readOnly);
				pad = null;
				System.gc();
			}
		} catch( Exception e) {
			e.printStackTrace();
		}

		System.out.println( "memory key-value store direct");
		try {
			for( int i = 1; i <= maxThreads; i++) {
				AppMemScratchpad pad = new AppMemScratchpad( factoryDirect, workload);
				pad.doit( i, durationMSec, readOnly);
				pad = null;
				System.gc();
			}
		} catch( Exception e) {
			e.printStackTrace();
		}
	
	}

}
