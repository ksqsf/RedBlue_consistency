package applications.microbenchmark.membership;
import util.Debug;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

//debug output
import util.Debug;

public class Membership{

	Datacenter datacenters[];
	Principal principals[];
	int datacenter;
	Role role;
	int roleid;
	int tokenSize;

	public Membership(String filename, int datacenter, Role role, int roleid) {
		this.datacenter = datacenter;
		this.role = role;
		this.roleid = roleid;

		readXml(filename);

	}

	private static final String DATACENTERCOUNT = "dataCenters";
	private static final String DATACENTERNUM = "dcNum";
	private static final String DATACENTER = "dataCenter";
	private static final String ZOOKEEPERCOUNT = "zooKeeper";
	private static final String ZOOKEEPERHOST = "zooIP";
	private static final String ZOOKEEPERPORT = "zooPort";
	private static final String APPSERVERCOUNT = "appServers";
	private static final String APPSERVERNUM = "asNum";
	private static final String APPSERVER = "appServer";
	private static final String APPSERVERHOST = "asIP";
	private static final String APPSERVERPORT = "asPort";

	private static final String USERCOUNT = "users";
	private static final String USERNUM = "uNum";
	private static final String USER = "user";
	private static final String USERHOST = "uIP";
	private static final String USERPORT = "uPort";

	private void readXml(String file) {
		if (datacenters != null)
			throw new RuntimeException("xml file already parsed!");
		Principal.resetUniqueIdentifiers();
		Datacenter dcs[] = null;
		AppServer as[] = null;
		User u[] = null;
		ZooKeeper z = null;

		int curDC = 0;
		int curAppServer = 0;
		int curUser = 0;

		Vector<Principal> princ = new Vector<Principal>();

		try {
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			InputStream in = new FileInputStream(file);
			XMLEventReader eventReader = inputFactory.createXMLEventReader(in);

			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();

				if (event.isStartElement()) {
					StartElement startElement = event.asStartElement();

					// If this is the DC_COUNT entry, then create the
					// datacenters array
					if (startElement.getName().getLocalPart() == DATACENTERCOUNT) {
						// Debug.println(startElement);
						Iterator<Attribute> attributes = startElement.getAttributes();
						while (attributes.hasNext()) {
							Attribute attribute = attributes.next();
							if (attribute.getName().toString().equals(
									DATACENTERNUM)) {
								int dcCount = Integer.parseInt(attribute
										.getValue());
								if (dcs != null)
									throw new RuntimeException(
											"malformed xml file.  Too many <dataCenters dcNum=\"XXX\" lines.");
								dcs = new Datacenter[dcCount];
							}
						}
					}
					
					// if it is appServer count, then create appServer count
					if (startElement.getName().getLocalPart() == APPSERVERCOUNT) {
						// Debug.println("\t\t"+startElement);
						Iterator<Attribute> attributes = startElement
								.getAttributes();
						while (attributes.hasNext()) {
							Attribute attribute = attributes.next();
							if (attribute.getName().toString().equals(APPSERVERNUM)) {
								int scount = Integer.parseInt(attribute
										.getValue());
								if (as != null && scount != as.length)
									throw new RuntimeException(
											"Bad XML:  different appServer counts");
								else
									as = new AppServer[scount];

							}
						}
					}

					// if it is appServer count, then create appServer count
					if (startElement.getName().getLocalPart() == ZOOKEEPERCOUNT) {
						// Debug.println("\t\t"+startElement);
						Iterator<Attribute> attributes = startElement
								.getAttributes();
						String zhost = null;
						int zport = 0;
						while (attributes.hasNext()) {
							Attribute attribute = attributes.next();
							if (attribute.getName().toString()
									.equals(ZOOKEEPERHOST))
								zhost = attribute.getValue();
							else if (attribute.getName().toString().equals(
									ZOOKEEPERPORT))
								zport = Integer.parseInt(attribute.getValue());
							else
								throw new RuntimeException("invalid attribute");
						}
						z = new ZooKeeper(zhost, zport);
						princ.add(z);
					}

					// if its an appServer entry then add it to the end of the appServer
					// list
					if (startElement.getName().getLocalPart() == APPSERVER) {
						// Debug.println("\t\t\t"+startElement);
						Iterator<Attribute> attributes = startElement
								.getAttributes();
						String chost = null;
						int cport = -1;
						while (attributes.hasNext()) {
							Attribute attribute = attributes.next();
							if (attribute.getName().toString()
									.equals(APPSERVERHOST))
								chost = attribute.getValue();
							else if (attribute.getName().toString().equals(
									APPSERVERPORT))
								cport = Integer.parseInt(attribute.getValue());
							else
								throw new RuntimeException("invalid attribute");
						}
						as[curAppServer++] = new AppServer(curAppServer, curDC, chost, cport);
						princ.add(as[curAppServer - 1]);
					}
					
					// if it is user count, then create user count
					if (startElement.getName().getLocalPart() == USERCOUNT) {
						// Debug.println("\t\t"+startElement);
						Iterator<Attribute> attributes = startElement
								.getAttributes();
						while (attributes.hasNext()) {
							Attribute attribute = attributes.next();
							if (attribute.getName().toString().equals(USERNUM)) {
								int scount = Integer.parseInt(attribute
										.getValue());
								if (u != null && scount != u.length)
									throw new RuntimeException(
											"Bad XML:  different user counts");
								else
									u = new User[scount];

							}
						}
					}

					// if its an user entry then add it to the end of the user
					// list
					if (startElement.getName().getLocalPart() == USER) {
						// Debug.println("\t\t\t"+startElement);
						Iterator<Attribute> attributes = startElement
								.getAttributes();
						String chost = null;
						int cport = -1;
						while (attributes.hasNext()) {
							Attribute attribute = attributes.next();
							if (attribute.getName().toString()
									.equals(USERHOST))
								chost = attribute.getValue();
							else if (attribute.getName().toString().equals(
									USERPORT))
								cport = Integer.parseInt(attribute.getValue());
							else
								throw new RuntimeException("invalid attribute");
						}
						u[curUser++] = new User(curUser, curDC, chost, cport);
						princ.add(u[curUser - 1]);
					}

				} else if (event.isEndElement()) {
					EndElement endElement = event.asEndElement();

					// end of data center then create the DC object and reset
					// the local counters
					if (endElement.getName().getLocalPart() == (DATACENTER)) {
						// Debug.println("\t"+endElement);
						dcs[curDC++] = new Datacenter(curDC, as, u, z);
						if(as != null){
						if (curAppServer != as.length)
							throw new RuntimeException("wrong appServer count: "
									+ curAppServer + " " + as.length);
						}
						if (curUser != u.length)
							throw new RuntimeException("wrong user count: "
									+ curUser + " " + u.length);
						as = null;
						u = null;
						z = null;
						curAppServer = 0;
						curUser = 0;
					}

					if (endElement.getName().equals(DATACENTERCOUNT)) {
						if (curDC != dcs.length)
							throw new RuntimeException(
									"wrong number of data centers "
											+ "looking for " + dcs.length
											+ " found " + curDC);
					}

				} else
					;// throw new
						// RuntimeException("invalid xml element: "+event);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		datacenters = dcs;
		principals = new Principal[princ.size()];
		for (int i = 0; i < principals.length; i++) {
			principals[i] = princ.get(i);
			Debug.println(principals[i]);
		}

	}

    public Datacenter[] getDatacenters(){
    	return datacenters;
        }

        public Datacenter getDatacenter(int i){
        	Debug.printf("get datacenter %d\n",i);
    	return datacenters[i];
        }


        public Principal getMe(){
    	return datacenters[datacenter].getPrincipal(role, roleid);
        }

        public Principal getPrincipal(int datacenter, Role role, int roleid){
    	return getDatacenter(datacenter).getPrincipal(role, roleid);
        }

        public int getDatacenterCount(){
    	return datacenters.length;
        }

        public int getAppServerCount(){
    	return datacenters[0].getAppServersCount();//TODO: need to fix this part, including datacenter id
        }
        
        public int getUserCount(){
        	return datacenters[0].getUsersCount();//Can be removed
            }
        
        public int getUserCount(int dcId){
        	return datacenters[dcId].getUsersCount();//TODO: need to fix this part, including datacenter id
        }
        
        public int getAllUserCount(){
        	int uCount = 0;
        	for(int i =0 ;i< datacenters.length; i++){
        		uCount += datacenters[i].getUsersCount();
        	}
        	return uCount;
        }

        public int getRoleCount(){
    	Debug.println("TBD:  FIX THIS!.  Membership.getRoleCount"); //TODO:
    	return 3;
        }

        public int getPrincipalCount(){
        	Debug.printf("principal count %d:\n", principals.length);
    	return principals.length;
        }

        public Principal getPrincipal(int index){
    	return principals[index];
        }


        public static void main(String args[]){
    	Membership m = new Membership(args[0], 0, Role.APPSERVER, 0);

    	Datacenter dcs[] = m.getDatacenters();
    	Debug.println("datacenters: "+dcs.length);
    	for (int i = 0; i < dcs.length; i++){
    	    Debug.println("datacenter " + i+" : "+dcs[i]);
    	}
    	
    	for(int i = 0; i < m.getPrincipalCount(); i++){
    		Debug.println("principal " + Integer.toString(i) + ":" + m.getPrincipal(i));
    	}
    	
    	m = new Membership(args[0], 1, Role.USER, 0);


    	for(int i = 0; i < m.getPrincipalCount(); i++){
    		Debug.println("principal " + Integer.toString(i) + ":" + m.getPrincipal(i));
    	}


        }

}
