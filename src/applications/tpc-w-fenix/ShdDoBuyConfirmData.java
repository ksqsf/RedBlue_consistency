import java.io.IOException;
import java.util.Vector;

import txstore.scratchpad.rdbms.util.tpcw.DBTPCWShdDoBuyConfirm1;
import txstore.scratchpad.rdbms.util.tpcw.DBTPCWShdDoBuyConfirm2;
import txstore.scratchpad.rdbms.util.tpcw.DBTPCWShdDoBuyConfirm3;
import txstore.scratchpad.rdbms.util.tpcw.DBTPCWShdDoBuyConfirm4;
import txstore.scratchpad.rdbms.util.tpcw.DBTPCWShdDoBuyConfirm5;
import txstore.scratchpad.rdbms.util.tpcw.DBTPCWShdDoBuyConfirm6;

/* 
 * ShdDoBuyConfirmData - Class used to store the results of the buy 
 *                         confirm web interaction
 *
 ************************************************************************
 * contain all paramerters we need to generate shadow operations
 ************************************************************************/

public class ShdDoBuyConfirmData {
	int shopping_id;
	int customer_id;

	// address
	String addr_street1;
	String addr_street2;
	String addr_city;
	String addr_state;
	String addr_zip;
	int country_id;
	
	boolean newAddr;

	// order
	int o_id;
	long o_date;
	double sc_sub_total;
	double sc_total;
	long o_ship_date;
	int customer_addr_id;
	int ship_addr_id;

	// orderline
	Vector<Integer> it_id_v;
	Vector<Integer> it_q_v;
	Vector<String> it_c_v;// comment

	// modify stock
	Vector<Integer> it_s_v; // stock delta, should be positive

	// credit card
	String cc_type;
	long cc_number;
	String cc_name;
	long cc_expiry;
	long cc_pay_date;
	String shipping;
	double c_discount;

	ShdDoBuyConfirmData() {
		newAddr = false;
		it_id_v = null;
		it_q_v = null;
		it_c_v = null;
		it_s_v = null; 
	}

	public void setShoppingCartInfo(int shopping_id, int customer_id,
			double c_discount) {
		this.shopping_id = shopping_id;
		this.customer_id = customer_id;
		this.c_discount = c_discount;
	}

	public void setShipAddrId(int ship_addr_id) {
		this.ship_addr_id = ship_addr_id;
	}
	
	public void setAddrInfo(String addr_street1, String addr_street2,
			String addr_city, String addr_state, String addr_zip, int country_id) {
		
		this.addr_street1 = addr_street1;
		this.addr_street2 = addr_street2;
		this.addr_city = addr_city;
		this.addr_state = addr_state;
		this.addr_zip = addr_zip;
		this.country_id = country_id;
		
		newAddr = true;

	}

	public void setOrderInfo(int o_id, long o_date, double sc_sub_total,
			double sc_total, String shipping, long o_ship_date,
			int customer_addr_id) {

		this.o_id = o_id;
		this.o_date = o_date;
		this.sc_sub_total = sc_sub_total;
		this.sc_total = sc_total;
		this.shipping = shipping;
		this.o_ship_date = o_ship_date;
		this.customer_addr_id = customer_addr_id;
	}

	public void setVectorInfo(Vector<Integer> it_id_v, Vector<Integer> it_q_v,
			Vector<String> it_c_v, Vector<Integer> it_s_v) {
		this.it_id_v = it_id_v;
		this.it_q_v = it_q_v;
		this.it_c_v = it_c_v;
		this.it_s_v = it_s_v;
	}

	public void setCreditCartInfo(String cc_type, long cc_number,
			String cc_name, long cc_expiry, long cc_pay_date) {
		
		this.cc_type = cc_type;
		this.cc_number = cc_number;
		this.cc_name = cc_name;
		this.cc_expiry = cc_expiry;
		this.cc_pay_date = cc_pay_date;

	}
	
	
	public DBTPCWShdDoBuyConfirm1 getShdDoBC1() {
		DBTPCWShdDoBuyConfirm1 dDb = null;
		try {
			dDb = DBTPCWShdDoBuyConfirm1.createOperation(shopping_id,
					customer_id, addr_street1, addr_street2, addr_city,
					addr_state, addr_zip, country_id, o_id, o_date,
					sc_sub_total, sc_total, o_ship_date, customer_addr_id,
					ship_addr_id, it_id_v, it_q_v, it_c_v, it_s_v, cc_type,
					cc_number, cc_name, cc_expiry, cc_pay_date, shipping,
					c_discount);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dDb;
	}

	public DBTPCWShdDoBuyConfirm2 getShdDoBC2() {
		DBTPCWShdDoBuyConfirm2 dDb = null;
		try {
			dDb = DBTPCWShdDoBuyConfirm2.createOperation(shopping_id,
					customer_id, addr_street1, addr_street2, addr_city,
					addr_state, addr_zip, country_id, o_id, o_date,
					sc_sub_total, sc_total, o_ship_date, customer_addr_id,
					ship_addr_id, it_id_v, it_q_v, it_c_v, it_s_v, cc_type,
					cc_number, cc_name, cc_expiry, cc_pay_date, shipping,
					c_discount);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dDb;
	}
	

	public DBTPCWShdDoBuyConfirm3 getShdDoBC3() {
		DBTPCWShdDoBuyConfirm3 dDb = null;
		try {
			dDb = DBTPCWShdDoBuyConfirm3.createOperation(shopping_id,
					customer_id, o_id, o_date, sc_sub_total, sc_total,
					o_ship_date, customer_addr_id, ship_addr_id, it_id_v,
					it_q_v, it_c_v, it_s_v, cc_type, cc_number, cc_name,
					cc_expiry, cc_pay_date, shipping, c_discount);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dDb;
	}

	public DBTPCWShdDoBuyConfirm4 getShdDoBC4() {
		DBTPCWShdDoBuyConfirm4 dDb = null;
		try {
			dDb = DBTPCWShdDoBuyConfirm4.createOperation(shopping_id,
					customer_id, o_id, o_date, sc_sub_total, sc_total,
					o_ship_date, customer_addr_id, ship_addr_id, it_id_v,
					it_q_v, it_c_v, it_s_v, cc_type, cc_number, cc_name,
					cc_expiry, cc_pay_date, shipping, c_discount);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dDb;
	}

	

	public DBTPCWShdDoBuyConfirm5 getShdDoBC5() {
		DBTPCWShdDoBuyConfirm5 dDb = null;
		try {
			dDb = DBTPCWShdDoBuyConfirm5.createOperation(shopping_id,
					customer_id, o_id, o_date, sc_sub_total, sc_total,
					o_ship_date, customer_addr_id, ship_addr_id,  addr_street1, addr_street2, addr_city,
					addr_state, addr_zip, country_id,cc_type, cc_number, cc_name,
					cc_expiry, cc_pay_date, shipping, c_discount);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dDb;
	}

	public DBTPCWShdDoBuyConfirm6 getShdDoBC6() {
		DBTPCWShdDoBuyConfirm6 dDb = null;
		try {
			dDb = DBTPCWShdDoBuyConfirm6.createOperation(shopping_id,
					customer_id, o_id, o_date, sc_sub_total, sc_total,
					o_ship_date, customer_addr_id, ship_addr_id, cc_type, cc_number, cc_name,
					cc_expiry, cc_pay_date, shipping, c_discount);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dDb;
	}
	
	public boolean isIncreased(){
		for(int i = 0; i < this.it_s_v.size(); i++){
			int stock = this.it_s_v.get(i).intValue();
			if(stock < 0)
				return false;
		}
		return true;
	}
	
	public boolean isNewAddr(){
		return newAddr;
	}
	
	public boolean isCartEmpty(){
		if(this.it_s_v == null)
			return true;
		else if (this.it_s_v.size() == 0)
			return true;
		return false;
	}

}
