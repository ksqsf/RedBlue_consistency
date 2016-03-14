package applications.microbenchmark.util;
import txstore.scratchpad.rdbms.util.DBShadowOperation;
import txstore.scratchpad.rdbms.util.micro.DBMICROEMPTY;
import txstore.scratchpad.rdbms.util.micro.DBMICROTX3;
import txstore.scratchpad.rdbms.util.micro.DBMICROTX4;
import txstore.util.Operation;
import util.Debug;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.Iterator;
import applications.microbenchmark.util.WorkloadType;
import applications.simplestub.Read;
import applications.simplestub.Write;


public class MicroWorkloadGenerator {

	public ArrayList<Database> dbList = new ArrayList<Database>();

	public ArrayList<String> redTableList = new ArrayList<String>();

	public ArrayList<String> blueTableList = new ArrayList<String>();

	public HashMap<String, TableSchema> tableSchemaList = new HashMap<String, TableSchema>();
	
	public double blueRatio;
	
	int readReqNum = 0;
	
	int writeReqNum = 0;
	
	public int min_key = 0;
	
	public int max_key = 10000;
	
	public int keySelection = 0;
	
	public int objectCount = 0;
	
	static final String charSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	static Random randomGenerator;
	
	public int txnNum =0;
	public int micro3Num = 0;
	public int micro4Num = 0;

	public MicroWorkloadGenerator(ArrayList<Database> l, int dcId, double bluerate, int r, int w,
			int minK, int maxK, int keySel) {
		randomGenerator = new Random();
		dbList = l;
		blueRatio = bluerate;
		readReqNum = r;
		writeReqNum = w;
		min_key = minK;
		max_key = maxK;
		keySelection = keySel;
		objectCount = maxK - minK;
		for (int i = 0; i < dbList.size(); i++) {
			Database db = dbList.get(i);
			if (db.dc_id == dcId) {
				redTableList.addAll(db.redTableList);
				blueTableList.addAll(db.blueTableList);
				tableSchemaList.putAll(db.tableSchemaList);
			}
		}
	}

	public String get_random_string(int length) {
		StringBuilder rndString = new StringBuilder(length);
		for(int i = 0;i < length; i ++){
			rndString.append(charSet.charAt(randomGenerator.nextInt(charSet.length())));
		}
		return rndString.toString();
	}

	public String get_update_query(String tableName, int primary_key, requestList rqList) {
		Debug.println("get update query");
		String sqlStr = "update " + tableName + " set";
		TableSchema ts = tableSchemaList.get(tableName);
		int num_column = ts.column_list.size();
		int colid = randomGenerator.nextInt(num_column);
		String pkStr = Integer.toString(primary_key);
		if(colid == 0)
			colid++;
		column_pair cp = ts.column_list.get(colid);
		String value = null;
		if (cp.column_type.contains("int")) {
			value =Integer.toString(randomGenerator.nextInt(max_key - min_key));
			sqlStr = sqlStr + " " + cp.column_name + " = " + value
					+ "  where " + ts.column_list.get(0).column_name + " = "
					+ pkStr + ";";
			try {
				DBMICROTX3 op = null;
				op = DBMICROTX3.createOperation(tableName, ts.column_list.get(0).column_name, 
						pkStr, cp.column_name, value);
				rqList.setShadowOp(op);
				micro3Num++;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			int strLength = getFieldSize(cp.column_type);
			value = get_random_string(strLength);
			sqlStr = sqlStr + " " + cp.column_name + " = '"
					+ value + "' where "
					+ ts.column_list.get(0).column_name + " ="
					+ pkStr + ";";
			try {
				DBMICROTX4 op = null;
				op = DBMICROTX4.createOperation(tableName, ts.column_list.get(0).column_name, 
						pkStr, cp.column_name, value);
				rqList.setShadowOp(op);
				micro4Num++;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		Debug.printf("update str %s\n", sqlStr);
		
		return sqlStr;
	}


	
	public String get_select_query(String tableName, int primary_key){
		Debug.println("get select query");
		String sqlStr = "";
		TableSchema ts = tableSchemaList.get(tableName);
		sqlStr = "select * from " + tableName + " where "
				+ ts.column_list.get(0).column_name + " = ";
		String value = "";
		if (ts.column_list.get(0).column_type.contains("int")) {
			value = Integer.toString(primary_key);
		} else {
			int strLength = getFieldSize(ts.column_list.get(0).column_type);
			value = get_random_string(strLength);
		}
		sqlStr = sqlStr + value + ";";
		Debug.printf("select query str%s\n",sqlStr);
		return sqlStr;
		
	}
	
	
	public requestList generate_commands(int userId, boolean isRead) {
		requestList rqList = new requestList();
		
		if(isRead){
			for(int i = 0 ; i < readReqNum+1; i++){
				int color = Math.random() < 0.5 ? 1 : 0;
				String tableName = select_table(color);
				int primary_key = select_key(userId);
				rqList.addRequest(get_select_query(tableName,primary_key));
			}
			
		}else{
			/*int color = Math.random() < 0.5 ? 1 : 0;
			String tableName = select_table(color);
			int primary_key = select_key(userId);
			rqList.addRequest(get_select_query(tableName,primary_key));*/
			
			for(int i = 0 ; i < writeReqNum; i++){
				int color = Math.random() < blueRatio ? 1: 0;
				String tableName = select_table(color);
				int primary_key = select_key(userId);
				if(color == 1){
					rqList.setColor(color);
				}
				rqList.addRequest(get_update_query(tableName, primary_key,rqList));
			}
		}
		if(rqList.op == null){
			DBMICROEMPTY op = null;
			Debug.println("create readonly empty shadow op");
			try {
				op = DBMICROEMPTY.createOperation();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			rqList.setShadowOp(op);
		}else{
			Debug.println("create update shadow op");
		}
		txnNum++;
		if(txnNum%2000 == 0){
			System.out.println("txnNum " + txnNum + " micro3 " + micro3Num + " micro4 " + micro4Num);
			micro3Num = 0;
			micro4Num = 0;
		}
		return rqList;
	}

	public String select_table(int color) {
		Debug.println("select table here");
		String tableStr = "";
		int table_num;
		int table_index;
		if (color == 0) {
			table_num = redTableList.size();
			table_index = randomGenerator.nextInt(table_num);
			tableStr = redTableList.get(table_index);
		} else {
			table_num = blueTableList.size();
			table_index = randomGenerator.nextInt(table_num);
			tableStr = blueTableList.get(table_index);
		}
		Debug.printf("selected table %s\n", tableStr);
		return tableStr;
	}
	
	public int select_key(int userId){
		int primary_key = 0;
		switch(keySelection){
		case 0:
			//random selection
			primary_key = (randomGenerator.nextInt(max_key - min_key) + min_key)%objectCount;
			return primary_key;
		case 1:
			//all different keys
			primary_key = (userId * 20 + 1 + min_key)%objectCount;
			return primary_key;
		case 2:
			//the same key
			primary_key = 1;
			return primary_key;
		default:
			throw new RuntimeException("Wrong key selection!");
		}
	}

	public int select_server(String sqlStr) {
		int i_server = -1;
		Set<String> words = tableSchemaList.keySet();
		for (Iterator<String> i = words.iterator(); i.hasNext();) {
			String key = i.next();
			if (sqlStr.contains(key)) {
				i_server = tableSchemaList.get(key).db_id;
				break;
			}
		}
		return i_server;
	}
	
	public int getFieldSize(String str){
		int size = 0;
		for(int i =0 ; i< str.length(); i++){
			if(Character.isDigit(str.charAt(i))){
				String numberStr = str.substring(i, str.length()-1);
				size = Integer.parseInt(numberStr);
				break;
			}
		}
		return size;
	}
	
	public ArrayList<String> getRedTableList(){
		return redTableList;
	}
	
	public ArrayList<String> getBlueTableList(){
		return blueTableList;
	}
	
	/*public String get_insert_query(String tableName, int primary_key) {
	Debug.println("get insert query");
	String sqlStr = "insert into " + tableName + " values(";
	TableSchema ts = tableSchemaList.get(tableName);
	int num_column = ts.column_list.size();
	Random randomGenerator = new Random();
	for (int i = 0; i < num_column; i++) {
		column_pair cp = ts.column_list.get(i);
		String value = "";
		if(i == 0){
			value = Integer.toString(primary_key);
		}else{
			if (cp.column_type.contains("int")) {
				value = Integer.toString(randomGenerator.nextInt(max_primary_key));
			} else {
				value = "'" + get_random_string(0) + "'";
			}
		}
		sqlStr = sqlStr + value + ",";
	}
	sqlStr = sqlStr.substring(0, sqlStr.length() - 2) + "');";
	Debug.printf("insert query str %s\n", sqlStr);
	return sqlStr;
}

public String get_delete_query(String tableName, int primary_key) {
	Debug.println("get delete query");
	String sqlStr = "";

	TableSchema ts = tableSchemaList.get(tableName);
	sqlStr = "delete from " + tableName + " where "
			+ ts.column_list.get(0).column_name + " = ";
	String value = "";
	if (ts.column_list.get(0).column_type.contains("int")) {
		value = Integer.toString(primary_key);
	} else {
		value = get_random_string(0);
	}
	sqlStr = sqlStr + value + ";";
	Debug.printf("delete query str %s\n", sqlStr);
	return sqlStr;
}*/

}