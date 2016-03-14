package applications.microbenchmark.FakedScratchpadTest;

import java.util.Random;

import util.Debug;

import applications.microbenchmark.util.WorkloadType;
import applications.simplestub.OperationList;
import applications.simplestub.Read;
import applications.simplestub.Write;

public class FakedWorkloadGenerator {
	int min_key;
	int max_key;
	double blueRatio;
	int keySelection;
	int readReqNum;
	int writeReqNum;

	public FakedWorkloadGenerator(double bluerate, int r, int w,
			int k1, int k2, int k3) {
		blueRatio = bluerate;
		readReqNum = r;
		writeReqNum = w;
		min_key = k1;
		max_key = k2;
		keySelection = k3;
	}

	public OperationList generate_commands(int userId, boolean isRead) {
		OperationList commands = new OperationList();
		for (int i = 0; i < readReqNum; i++) {
			int color = Math.random() < 0.5 ? 1 : 0;
			int primary_key = selectKey(userId);
			commands.addRead(new Read(primary_key, color));
		}
		
		if (!isRead) {
			for (int i = 0; i < writeReqNum; i++) {
				int color = Math.random() < blueRatio ? 1 : 0;
				int primary_key = selectKey(userId);
				commands.addWrite(new Write(primary_key, color));
			}
		}

		Debug.println("\t\t\tReads: ");
		for (int j = 0; j < commands.getReads().size(); j++) {
			Debug.print(commands.getReads().elementAt(j) + " ");
		}
		Debug.println("\t\t\tWrites:");
		for (int j = 0; j < commands.getWrites().size(); j++) {
			Debug.print(commands.getWrites().elementAt(j) + " ");
		}
		return commands;
	}

	public int selectKey(int userId) {
		Random randomGenerator = new Random();
		int primary_key = 0;
		switch (keySelection) {
		case 0:
			// random selection
			if(max_key == 0){
				primary_key = 0;
			}else
				primary_key = randomGenerator.nextInt(max_key);
			return primary_key;
		case 1:
			// all different keys
			primary_key = userId * 20 + min_key + 1;
			return primary_key;
		case 2:
			// the same key
			primary_key = min_key + 1;
			return primary_key;
		default:
			throw new RuntimeException("Wrong key selection!");
		}
	}

}
