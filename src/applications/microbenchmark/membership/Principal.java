package applications.microbenchmark.membership;
import util.Debug;

import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Principal{
    InetAddress host;
    int port;
    static int count = 0;
    int uniqueid;
    InetSocketAddress isa;

    protected static void resetUniqueIdentifiers(){
	count = 0;
    }

    public Principal(String host, int port){
	try{
		 Debug.println("I am host "+ host );
	    this.host = InetAddress.getByName(host);
	    Debug.println(this.host);
	}catch(Exception e){
	    throw new RuntimeException(e);
	}
	this.port = port;
	uniqueid = count++;
	this.isa = new InetSocketAddress(host, port);
	Debug.println("ip address is:  "+isa);
    }

    public Principal(InetAddress host, int port){
	this.host = host;
	this.port = port;
	uniqueid = count++;
	this.isa = new InetSocketAddress(host, port);
	Debug.println("ip address is:  "+isa);
    }
    
    public InetSocketAddress getInetSocketAddress(){
    return isa;
    }

    public InetAddress getHost(){
	return host;
    }

    public int getPort(){
	return port;
    }
	
    public int getUniqueId(){
	return uniqueid;
    }
    

    public String toString(){
	return "<"+getHost()+":"+getPort()+" ** "+getUniqueId()+">";
    }
}