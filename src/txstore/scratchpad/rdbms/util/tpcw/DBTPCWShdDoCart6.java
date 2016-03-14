/**
 * only insert
 */
package txstore.scratchpad.rdbms.util.tpcw;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

import txstore.scratchpad.ScratchpadException;
import txstore.scratchpad.rdbms.IDatabase;
import txstore.scratchpad.rdbms.IDefDatabase;
import txstore.scratchpad.rdbms.util.DBShadowOperation;
import util.Debug;

public class DBTPCWShdDoCart6 extends DBShadowOperation {
	int shopping_id;
	Date access_time;

	public static DBTPCWShdDoCart6 createOperation(
			DataInputStream dis) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		int shopping_id = dis.readInt();
		dos.writeInt(shopping_id);
		long access_time = dis.readLong();
		dos.writeLong(access_time);
		return new DBTPCWShdDoCart6(baos.toByteArray(), shopping_id,access_time);

	}

	public static DBTPCWShdDoCart6 createOperation(int shopping_id, long access_time) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		dos.writeByte(OP_SHADOWOP);
		dos.writeByte(OP_SHD_TPCW_DOCART6);
		dos.writeInt(shopping_id);
		dos.writeLong(access_time);
		return new DBTPCWShdDoCart6(baos.toByteArray(), shopping_id,access_time);

	}

	protected DBTPCWShdDoCart6(byte[] arr, int shopping_id, long access_time) {
		super(arr);
		this.shopping_id =shopping_id ; 
		this.access_time = new Date(access_time);

	}

	public DBTPCWShdDoCart6(byte[] arr) {
		super(arr);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void encode(DataOutputStream dos) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public int execute(IDatabase store) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void executeShadow(IDefDatabase iDefDatabase) {
		// TODO Auto-generated method stub
		Debug.println("execute shadow operation add to cart but only update the cart");
		
		try {
			String updateAccessTimeQuery = "UPDATE shopping_cart SET sc_time = '"+access_time+"' WHERE sc_id = "+shopping_id;
			iDefDatabase.executeUpdate(updateAccessTimeQuery);
		} catch( Exception e) {
			System.err.println("There was an exception when performing the shadow operation");
		}
	}

	@Override
	public boolean isQuery() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean registerIndividualOperations() {
		// TODO Auto-generated method stub
		return false;
	}

}
