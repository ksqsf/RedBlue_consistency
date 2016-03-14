package applications.microbenchmark.util;

import java.util.ArrayList;

import txstore.scratchpad.rdbms.util.DBShadowOperation;
import txstore.util.Operation;

public class requestList {
	
	int color;
	ArrayList<String> commands;
	DBShadowOperation op = null;
	
	public requestList(){
		color = 0;
		commands = new ArrayList<String>();
	}
	
	public void addRequest(String s){
		commands.add(s);
	}
	
	public ArrayList<String> getCommands(){
		return commands;
	}
	
	public void setColor(int c){
		if(color != c){
			color = c;
		}
	}
	
	public int getColor(){
		return color;
	}
	
	public void setShadowOp(DBShadowOperation op){
		this.op = op;
	}
	
	public DBShadowOperation getShadowOp(){
		return op;
	}

}
