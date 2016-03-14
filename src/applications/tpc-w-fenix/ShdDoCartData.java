import java.io.IOException;
import java.util.Vector;

import txstore.scratchpad.rdbms.util.tpcw.DBTPCWShdDoCart1;
import txstore.scratchpad.rdbms.util.tpcw.DBTPCWShdDoCart2;
import txstore.scratchpad.rdbms.util.tpcw.DBTPCWShdDoCart3;
import txstore.scratchpad.rdbms.util.tpcw.DBTPCWShdDoCart4;
import txstore.scratchpad.rdbms.util.tpcw.DBTPCWShdDoCart5;
import txstore.scratchpad.rdbms.util.tpcw.DBTPCWShdDoCart6;
import txstore.scratchpad.rdbms.util.tpcw.DBTPCWShdDoCart7;

/* 
 * ShdDoCartData - Class used to store the results of the do 
 *                         cart web interaction
 *
 ************************************************************************
 * contain all paramerters we need to generate shadow operations
 ************************************************************************/

public class ShdDoCartData {
	int shopping_id;
	int insert_i_id;
	boolean isInsert;
	Vector<Integer> up_id_v;
	Vector<Integer> up_q_v;
	Vector<Integer> r_id_v;// removal
	long access_time;

	ShdDoCartData(int shopping_id) {
		this.shopping_id = shopping_id;
		up_id_v = null;
		up_q_v = null;
		r_id_v = null;
		isInsert = false;
	}
	
	public void setVectorInfo(	Vector<Integer> up_id_v, Vector<Integer> up_q_v,
			Vector<Integer> r_id_v){
		this.up_id_v = up_id_v;
		this.up_q_v = up_q_v;
		this.r_id_v = r_id_v;
	}
	
	public void setInsertInfo(int id){
		this.isInsert = true;
		this.insert_i_id = id;
	}
	
	public void setUpdateInfo(int id, int quantity){
		if(this.up_id_v == null){
			this.up_id_v = new Vector<Integer>();
			this.up_q_v = new Vector<Integer>();
		}
		this.up_id_v.add(id);
		this.up_q_v.add(quantity);
	}
	
	public void setRemoveInfo(int id){
		if(this.r_id_v == null){
			this.r_id_v = new Vector<Integer>();
		}
		this.r_id_v.add(id);
		if(id == this.insert_i_id){
			this.isInsert = false;
		}
	}

	public void setAccessTime(long access_time){
		this.access_time = access_time;	
	}
	
	public DBTPCWShdDoCart1 getDoCart1(){
		DBTPCWShdDoCart1 dDc1 = null;
		try {
			dDc1 = DBTPCWShdDoCart1.createOperation(shopping_id, insert_i_id, access_time);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dDc1;
	}
	
	public DBTPCWShdDoCart2 getDoCart2(){
		DBTPCWShdDoCart2 dDc2 = null;
		try {
			dDc2 = DBTPCWShdDoCart2.createOperation(shopping_id, insert_i_id, up_id_v, up_q_v, access_time);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dDc2;
	}
	
	public DBTPCWShdDoCart3 getDoCart3(){
		DBTPCWShdDoCart3 dDc3 = null;
		try {
			dDc3 = DBTPCWShdDoCart3.createOperation(shopping_id, up_id_v, up_q_v, access_time);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dDc3;
	}
	
	public DBTPCWShdDoCart4 getDoCart4(){
		DBTPCWShdDoCart4 dDc4 = null;
		try {
			dDc4 = DBTPCWShdDoCart4.createOperation(shopping_id, up_id_v, up_q_v, r_id_v,access_time);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dDc4;
	}
	
	public DBTPCWShdDoCart5 getDoCart5(){
		DBTPCWShdDoCart5 dDc5 = null;
		if(this.up_id_v == null){
			this.up_id_v = new Vector<Integer>();
			this.up_q_v = new Vector<Integer>();
		}
		try {
			dDc5 = DBTPCWShdDoCart5.createOperation(shopping_id, insert_i_id,up_id_v, up_q_v, r_id_v,access_time);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dDc5;
	}
	
	public DBTPCWShdDoCart6 getDoCart6() {
		DBTPCWShdDoCart6 dDc6 = null;
		try {
			dDc6 = DBTPCWShdDoCart6.createOperation(shopping_id, access_time);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dDc6;
	}
	
	public DBTPCWShdDoCart7 getDoCart7() {
		DBTPCWShdDoCart7 dDc7 = null;
		try {
			dDc7 = DBTPCWShdDoCart7.createOperation(shopping_id, r_id_v, access_time);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dDc7;
	}
	
	public boolean isInserted(){
		if(this.isInsert){
			return true;
		}
		return false;
	}
	
	public boolean isUpdated(){
		if(this.up_id_v == null)
			return false;
		if(this.up_id_v.size() > 0)
			return true;
		return false;
	}
	
	public boolean isRemoved(){
		if(this.r_id_v == null)
			return false;
		if(this.r_id_v.size() > 0)
			return true;
		return false;
	}
}
