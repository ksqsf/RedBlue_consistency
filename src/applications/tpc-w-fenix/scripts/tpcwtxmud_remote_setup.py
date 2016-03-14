#! /usr/bin/env python
"""
remote setup scripts
command line scripts for configuring remote clients 
"""

import os
import sys
import base64
import getpass
import traceback
import datetime
import time
import socket
import paramiko
from optparse import OptionParser
from optparse import OptionGroup
from binascii import hexlify

#globals
#path to: svn co  https://svn.mpi-sws.org/DPS/txstore/java /var/tmp/dcfp/txmudroot
txmud_root = "/home/chengli/java"
tpcw_root = txmud_root+"/src/applications/tpc-w-fenix"

class Log():

    def __init__(self,filename):
            dir = "logs/"
            if not os.path.exists(dir):
                os.makedirs(dir)
            self.logname=dir+filename
            self.logfile = open(self.logname,"a")
            
    def __del__(self):
        if self.logfile!=None:
            self.logfile.close()
    
    def log(self,message):
        if self.logfile != None:
            str=get_str_timestamp()+"\t"+message
            print str
            self.logfile.write(str+"\n")
            
class Node():
                
    def __init__(self, hostname, username, password,keyfile=None):
        self.hostname = hostname
        self.username = username
        self.password = password
        self.keyfile = keyfile
        strfilename=hostname +".log"
        self.log_handler = Log(filename=strfilename)
    
    def __str__(self):
        return "<"+self.hostname+","+self.username+","+self.password+">"

class Connector():
    """Connect to remote host with SSH and issue commands.
    
    This is a facade/wrapper that uses Paraminko to spawn and control an SSH client. 
    You must have paraminko library installed. 
        
    @ivar node: Host name or IP address, User name, Password
    @ivar port: Ssh port
    @ivar ssh: instance of ssh client
    """
        
    def __init__(self, node):
        """
            
        @ivar node connection parameters
        
        """
        self.node = node    
        self.port = 22  #default SSH port
        self.ssh = None
        str=self.node.log_handler.logname+".ssh"
        paramiko.util.log_to_file(str)
        #self.logger = paramiko.util.logging.getLogger()

    def __del__(self):
        """Close the socket in case it was left opened
        
            
        """
        if self.ssh != None:
            self.ssh.close()
   
   
    def agent_auth(self,transport, username):
        """
        Attempt to authenticate to the given transport using any of the private
        keys available from an SSH agent.
        """
        agent = paramiko.Agent()
        agent_keys = agent.get_keys()
        if len(agent_keys) == 0:
            return
    
        for key in agent_keys:
#            print 'Trying ssh-agent key %s' % hexlify(key.get_fingerprint()),
            try:
                transport.auth_publickey(username, key)
#                print '... success!'
                return
            except paramiko.SSHException:
 #               print '... nope.'
                return
   
    def open(self):
        username = self.node.username
        hostname = self.node.hostname
        port = self.port
       
        # now connect
       
        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.connect((hostname, port))
        except Exception, e:
            print '*** Connect failed: ' + str(e) +" check your network connection"
            sys.exit(1)

        t = paramiko.Transport(sock)

        try:
            t.start_client()
        except paramiko.SSHException:
            print '*** SSH negotiation failed. first attempt'
            sys.exit(1)
 
        
        try:
            keys = paramiko.util.load_host_keys(os.path.expanduser('~/.ssh/known_hosts'))
        except IOError:
            try:
                keys = paramiko.util.load_host_keys(os.path.expanduser('~/ssh/known_hosts'))
            except IOError:
                print '*** Unable to open host keys file'
                keys = {}
        # check server's host key -- this is important.
#            key = t.get_remote_server_key()
#            if not keys.has_key(hostname):
#                print '*** WARNING: Unknown host key!'
#            elif not keys[hostname].has_key(key.get_name()):
#                print '*** WARNING: Unknown host key!'
#            elif keys[hostname][key.get_name()] != key:
#                print '*** WARNING: Host key has changed!!!'
#                sys.exit(1)
#            else:
#                print '*** Host key OK.'
        # get username
        self.agent_auth(t, username)
        self.ssh=t
        if not t.is_authenticated():
            t.close()
            self.open_old()
        
    

    def open_old(self):
        """Connect to a remote host and login.
        
        """
       
        # get host key, if we know one
        hostkeytype = None
        hostkey = None
        host_keys = None
        try:
            host_keys = paramiko.util.load_host_keys(os.path.expanduser('~/.ssh/known_hosts'))
        except IOError:
            try:
                # try ~/ssh/ too, because windows can't have a folder named ~/.ssh/
                host_keys = paramiko.util.load_host_keys(os.path.expanduser('~/ssh/known_hosts'))
            except IOError:
                print '*** Unable to open host keys file'
                host_keys = {}
        
        if host_keys.has_key(self.node.hostname):
            hostkeytype = host_keys[self.node.hostname].keys()[0]
            hostkey = host_keys[self.node.hostname][hostkeytype]
            #print 'Using host key of type %s' % hostkeytype
        
        
        # now, connect and use paramiko Transport to negotiate SSH2 across the connection
        #print self.node
        #it might use password or an RSA key file
        #print " parameters",self.node.hostname, self.port
   
        try:
            self.ssh = paramiko.Transport((self.node.hostname, self.port))
            key=None
            if self.node.keyfile!= None:
                try:
                    key = paramiko.RSAKey.from_private_key_file(self.node.keyfile)
                except paramiko.PasswordRequiredException:
                    key = paramiko.RSAKey.from_private_key_file(self.node.keyfile, self.node.password)
                    
                self.ssh.connect(username=self.node.username, pkey=key)
            else:
                self.ssh.connect(username=self.node.username, password=self.node.password, hostkey=hostkey)
        except socket.gaierror, e:
            print 'Error: hostname %s is unreacheable, check your connection trying to ping to it ' % (self.node.hostname)
            sys.exit(1)

        except paramiko.AuthenticationException, e:
            print 'Error: Autentication Failed for hostname %s , check in node.db file whether the credentials for connection to this node are correct' % (self.node.hostname)
            sys.exit(1)
        except IOError, e:
            print "\tFile not found!"
            print "Error: keyfile not found, check the node.db file again!"
            sys.exit(1)
        except Exception, e:
            print '*** Caught exception: %s --> %s: %s' % (self.node.hostname,e.__class__, e)
            traceback.print_exc()
            try:
                self.ssh.close()
            except:
                pass
            sys.exit(1)
    
    
    
    def close(self):
        """Close the connection to the remote host.
            
        """
        self.ssh.close()
    
        
                
    def run_command(self, str_command, background=False):
        """Run a command on the remote host.
            
        @param command: Unix command
        @return: Command output
        @rtype: String
        """ 
        chan = self.ssh.open_session()

        chan.exec_command(command=str_command)
        str = ""
        if not background:
            #wait for the output
            while(True):
                strbuff = chan.recv(1024)
                if len(strbuff) == 0:
                    break
                str = str + strbuff
        chan.close()
        return str
    
    def get(self,remote_path,local_path):
        #print "Downloading file from source ",remote_path," to destination",local_path
        try:
            sftp = paramiko.SFTPClient.from_transport(self.ssh)
            sftp.get(remote_path,local_path)
        except IOError, e:
            print "\tFile not found!"
 
#------------------------------------------------------------------------------

def get_str_timestamp():
    timestamp = datetime.datetime.now()
    time = str(timestamp).replace(":",".").replace(" ","_")
    return time

def setup_coordinator(options,node):
    """
    setup the coordinator into a remote host
    """
   
    node.log_handler.log("Starting to configure the coordinator "+node.hostname)
    if options.role != "coordinator":
        node.log_handler.log("Error: invalid configuration role")
        return -1
      
    if options.dir == None:
       options.dir = "/var/tmp/txmud/stable_new_message/coordinator/"
       node.log_handler.log("\tWarning: coordinator's configuration path not set, using default:"+options.dir)
       
    #check for all parameters of the coordinator!!!
    if options.coordinatorId is None  or options.cdPort is None or \
       options.dcNum is None or options.remoteDcAddrList is None  or options.remoteDcPortList is None:
       node.log_handler.log("Error: insufficient parameters for configuring the coordinator")
       return -1
    
    #format list of address and port do match the configuration file format    
    remote_coordinators_addr_lst = options.remoteDcAddrList.split()
    tmp_lst=[]
    for addr in remote_coordinators_addr_lst:
        addr = '"'+addr+'"'
        tmp_lst.append(addr)
    #print tmp_lst
    options.remoteDcAddrList = ",".join(tmp_lst)
    #print "List of remote addrs: "+options.remoteDcAddrList
    
    remote_coordinators_port_lst = options.remoteDcPortList.split()
    options.remoteDcPortList = ",".join(remote_coordinators_port_lst)
    #print "List of remote ports: "+options.remoteDcPortList
    
    if len(remote_coordinators_addr_lst) != len(remote_coordinators_port_lst):
        node.log_handler.log("Error: number of coordinator's ports and address does not match!")
        return -1
    
    #deploy commands to remote host
    node.log_handler.log("\tTrying to connect to remote host")
    conn = Connector(node)
    conn.open()
    
    node.log_handler.log("\tBacking up previous coordinator configuration")
    bkpfile="Coordinator.py.back_"+get_str_timestamp()
    strconfig="cd "+options.dir+" && "+"mv Coordinator.py "+bkpfile
    #print strconfig
    conn.run_command(strconfig)
    
    #setting up environment
    strconfig="cd "+options.dir+" && " #reassigned
    strconfig = strconfig+"cat "+bkpfile+" | "
    strconfig = strconfig+"sed '/^period/ s/=.*/= "+options.cdperiod+"/' | "
    strconfig = strconfig+"sed '/^dcId/ s/=.*/= "+options.dcId+"/' | "
    strconfig = strconfig+"sed '/^coordinatorId/ s/=.*/= "+options.coordinatorId+"/' | "
    strconfig = strconfig+"sed '/^cdPort/ s/=.*/= "+options.cdPort+"/' | "
    strconfig = strconfig+"sed '/^dcNum/ s/=.*/= "+options.dcNum+"/' | "
    strconfig = strconfig+"sed '/^remoteDcIPList/ s/(.*)/("+options.remoteDcAddrList+")/' | "
    strconfig = strconfig+"sed '/^remoteDcPortList/ s/(.*)/("+options.remoteDcPortList+")/'  "
    strconfig = strconfig+"> Coordinator.py"
    #print strconfig
    conn.run_command(strconfig)
    node.log_handler.log("Done!!!!\n")
    conn.close()
    del conn
    
def setup_mshim(options,node):
    """
    setup the coordinator into a remote host
    """

    node.log_handler.log("Starting to configure the mysqlshim "+node.hostname)
    if options.role != "mshim":
        node.log_handler.log("Error: invalid configuration role")
        return -1
      
    if options.dir == None:
       options.dir = "/var/tmp/txmud/stable_new_message/storageshim/"
       node.log_handler.log("\tWarning: mshim's configuration path not set, using default:"+options.dir)
    
     #check for all parameters of the shim!!!
    if options.msPort is None or options.mshimId is None or \
       options.dbHost is None or options.dcNum is None or     \
       options.dbPort is None  or options.dbUser is None or   \
       options.dbPasswd is None  or options.dbName is None or \
       options.cdAddr is None  or options.cdPort is None or   \
       options.remoteStoreAddrList is None  or options.remoteStorePortList is None:
       node.log_handler.log("Error: insufficient parameters for configuring mshim")
       return -1
    
    
    #format list of address and port do match the configuration file format    
    mshim_peers_addr_lst = options.remoteStoreAddrList.split()
    tmp_lst=[]
    for addr in mshim_peers_addr_lst:
        addr = '"'+addr+'"'
        tmp_lst.append(addr)
    #print tmp_lst
    options.remoteStoreAddrList = ",".join(tmp_lst)
    #print "List of remote addrs: "+options.remoteStoreAddrList
    
    mshim_peers_port_lst = options.remoteStorePortList.split()
    options.remoteStorePortList = ",".join(mshim_peers_port_lst)
    #print "List of remote ports: "+options.remoteStorePortList
    
    if len(mshim_peers_addr_lst) != len(mshim_peers_port_lst):
        node.log_handler.log("Error: number of coordinator's ports and address does not match!")
        return -1
    
    
    node.log_handler.log("\tTrying to connect to remote host")
    conn = Connector(node)
    conn.open()
    
    node.log_handler.log("\tBacking up previous mshim configuration")
    bkpfile="mysqlShim.py.back_"+get_str_timestamp()
    strconfig="cd "+options.dir+" && "+"mv mysqlShim.py "+bkpfile
    #print strconfig
    conn.run_command(strconfig)
    
    #setting up environment
    strconfig="cd "+options.dir+" && " #reassigned
    strconfig = strconfig+"cat "+bkpfile+" | "
   
    #datacenter configuration
    strconfig = strconfig+"sed '/^dcId/ s/=.*/= "+options.dcId+"/' | "
    #daemon parameters 
    strconfig = strconfig+"sed '/^msPort/ s/=.*/= "+options.msPort+"/' | "
    strconfig = strconfig+"sed '/^mysqlshimId/ s/=.*/= "+options.mshimId+"/' | "
    strconfig = strconfig+"sed '/^dcNum/ s/=.*/= "+options.dcNum+"/' | "
    #database configuration 
    strconfig = strconfig+"sed '/^dbHost/ s/=.*/= \""+options.dbHost+"\"/' | "
    strconfig = strconfig+"sed '/^dbPort/ s/=.*/= "+options.dbPort+"/' | "
    strconfig = strconfig+"sed '/^dbUser/ s/=.*/= \""+options.dbUser+"\"/' | "
    strconfig = strconfig+"sed '/^dbPasswd/ s/=.*/= \""+options.dbPasswd+"\"/' | "
    strconfig = strconfig+"sed '/^dbDatabase/ s/=.*/= \""+options.dbName+"\"/' | "
    #coordinator configuration
    strconfig = strconfig+"sed '/^cdIP/ s/=.*/= \""+options.cdAddr+"\"/' | "     
    strconfig = strconfig+"sed '/^cdPort/ s/=.*/= "+options.cdPort+"/' | "
    #peer configuration
    strconfig = strconfig+"sed '/^period/ s/=.*/= "+options.period+"/' | "
    strconfig = strconfig+"sed '/^remoteStoreIPList/ s/(.*)/("+options.remoteStoreAddrList+")/' | "
    strconfig = strconfig+"sed '/^remoteStorePort/ s/(.*)/("+options.remoteStorePortList+")/' "
    
    strconfig = strconfig+"> mysqlShim.py"
        #print strconfig
    conn.run_command(strconfig)
    node.log_handler.log("Done!!!!\n")

#####################################################################################
    node.log_handler.log("\tBacking up previous Scratchpad configuration")
    bkpfile="Scratchpad.py.back_"+get_str_timestamp()
    strconfig="cd "+options.dir+" && "+"mv Scratchpad.py "+bkpfile
    #print strconfig
    conn.run_command(strconfig)
    
    #setting up environment
    strconfig="cd "+options.dir+" && " #reassigned
    strconfig = strconfig+"cat "+bkpfile+" | "
    #database configuration 
    strconfig = strconfig+"sed '/^dbHost/ s/=.*/= \""+options.dbHost+"\"/' | "
    strconfig = strconfig+"sed '/^dbPort/ s/=.*/= "+options.dbPort+"/' | "
    strconfig = strconfig+"sed '/^dbUser/ s/=.*/= \""+options.dbUser+"\"/' | "
    strconfig = strconfig+"sed '/^dbPasswd/ s/=.*/= \""+options.dbPasswd+"\"/' | "
    strconfig = strconfig+"sed '/^dbDatabase/ s/=.*/= \""+options.dbName+"\"/'  "
    #coordinator configuration
    strconfig = strconfig+"> Scratchpad.py"

    #print strconfig
    conn.run_command(strconfig)
    node.log_handler.log("Done!!!!\n")
    conn.close()
    del conn
            
def setup_proxy(options,node):
    """
    setup the proxy into a remote host
    """
   
    node.log_handler.log("Starting to configure  the proxy "+node.hostname)
    if options.role != "proxy":
        node.log_handler.log("Error: invalid configuration role")
        return -1
      
    if options.dir == None:
       options.dir = "/var/tmp/txmud/stable_new_message/webshim/"
       node.log_handler.log("\tWarning: proxy's configuration path not set, using default:"+options.dir)
       
#    #check for all parameters of the coordinator!!!
#    if options.coordinatorId is None  or options.cdPort is None or \
#       options.dcNum is None or options.remoteDcAddrList is None  or options.remoteDcPortList is None:
#       node.log_handler.log("Error: insufficient parameters for configuring the proxy")
#       return -1
    
    #format list of address and port do match the configuration file format    
#    mshim_peers_addr_lst = options.mshimAddrList.split()
#    tmp_lst=[]
#    for addr in mshim_peers_addr_lst:
#        addr = '"'+addr+'"'
#        tmp_lst.append(addr)
#    #print tmp_lst
#    options.mshimAddrList = ",".join(tmp_lst)
#    #print "List of remote addrs: "+options.mshimAddrList
#    
#    mshim_peers_port_lst = options.mshimPortList.split()
#    options.mshimPortList = ",".join(mshim_peers_port_lst)
    #print "List of remote ports: "+options.mshimPortList
    
    
#    if len(mshim_peers_addr_lst) != len(mshim_peers_port_lst):
#        node.log_handler.log("Error: number of mShim's ports and address does not match!")
#        return -1
    
    #deploy commands to remote host
    node.log_handler.log("\tTrying to connect to remote host")
    conn = Connector(node)
    conn.open()
    
    node.log_handler.log("\tBacking up previous coordinator configuration")
    bkpfile="webShim.py.back_"+get_str_timestamp()
    strconfig="cd "+options.dir+" && mv webShim.py "+bkpfile
    #print strconfig
    conn.run_command(strconfig)
    
    #setting up environment
    strconfig="cd "+options.dir+" && " #reassigned
    strconfig = strconfig+"cat "+bkpfile+" | "
    strconfig = strconfig+"sed '/^webShimId/ s/=.*/= "+options.proxyId+"/' | "
    strconfig = strconfig+"sed '/^dcId/ s/=.*/= "+options.dcId+"/' | "
    strconfig = strconfig+"sed '/^cdIP/ s/=.*/= \""+options.xcdAddr+"\"/' | "
    strconfig = strconfig+"sed '/^cdPort/ s/=.*/= "+options.xcdPort+"/' | "
    strconfig = strconfig+"sed '/^dcNum/ s/=.*/= "+options.dcNum+"/' | "
    strconfig = strconfig+"sed '/^webProxyPort/ s/=.*/= "+options.pxport+"/' | "
    strconfig = strconfig+"sed '/^storageAddrTable/ s/{.*}/{"+options.mshimpeers+"}/'  "
    strconfig = strconfig+"> webShim.py"
    #print strconfig
    conn.run_command(strconfig)
    node.log_handler.log("Done!!!\n")
    conn.close()
    del conn
    

def setup_user(options,node):
    """
    setup the coordinator into a remote host
    """
    node.log_handler.log("Starting to configure the user agent "+node.hostname)
    if options.role != "user":
        node.log_handler.log("Error: invalid configuration role")
        return -1
      
    if options.dir == None:
       options.dir = "/var/tmp/txmud/stable_new_message/user/"
       node.log_handler.log("\tWarning: user's configuration path not set, using default:"+options.dir)
       
    #check for all parameters of the user!!!
#    if options.coordinatorId is None  or options.cdPort is None or \
#       options.dcNum is None or options.remoteDcAddrList is None  or options.remoteDcPortList is None:
#       node.log_handler.log("Error: insufficient parameters for configuring the coordinator")
#       return -1
   
    #deploy comandos to remote host
    node.log_handler.log("\tTrying to connect to remote host")
    conn = Connector(node)
    conn.open()
    
    node.log_handler.log("\tBacking up previous user' configuration")
    bkpfile="user.py.back_"+get_str_timestamp()
    strconfig="cd "+options.dir+" && "+"mv user.py "+bkpfile
    #print strconfig
    conn.run_command(strconfig)
    
    #setting up environment
    strconfig="cd "+options.dir+" && " #reassigned
    strconfig = strconfig+"cat "+bkpfile+" | "
    strconfig = strconfig+"sed '/^UserId/ s/=.*/= "+options.userId+"/' | "
    strconfig = strconfig+"sed '/^proxyIP/ s/=.*/= \""+options.proxyAddr+"\"/' | "
    strconfig = strconfig+"sed '/^proxyPort/ s/=.*/= "+options.proxyPort+"/' | "
    strconfig = strconfig+"sed '/^txn_num/ s/=.*/= "+options.transactions+"/' "
    strconfig = strconfig+"> user.py"
    #print strconfig
    
    conn.run_command(strconfig)
    node.log_handler.log("Done!!!\n")
    conn.close()
    del conn

def setup_storage(options,node):
    """
    setup the storage into a remote host
    """
    node.log_handler.log("Starting to configure the storage "+node.hostname)
    if options.role != "storage":
        node.log_handler.log("Error: invalid configuration role")
        return -1
      
    if options.dir == None:
       options.dir = "/var/tmp/txmud/experiments/microbenchmark/"
       node.log_handler.log("\tWarning: user's configuration path not set, using default:"+options.dir)
       
    if options.mysqlPath == None:
       options.mysqlPath = "/usr/local/mysql/bin/"
       node.log_handler.log("\tWarning: mysql daemon path not set, using default:"+options.mysqlPath)

    node.log_handler.log("Configuring database (schema procedures and initial data).")
    if options.sqlFile == None:
       options.sqlFile = "database.sql"
       node.log_handler.log("\tWarning: database creation script not set, using default:"+options.sqlFile)
    
    if options.sqlprocedures == None:
       options.sqlprocedures = "database_proc.sql"
       node.log_handler.log("\tWarning: database procedure script wasn't set, using default:"+options.sqlprocedures)
    if options.sqlinitialdata == None:
       options.sqlinitialdata = "initial.sql"
       node.log_handler.log("\tWarning: database initializing script wasn't set, using default:"+options.sqlinitialdata)

   
    #deploy comandos to remote host
    #node.log_handler.log("Trying to connect to remote host")
    conn = Connector(node)
    conn.open()
    
    node.log_handler.log("Checking whether mysql is running")
    output = conn.run_command("pidof mysqld").strip()
    if len(output) > 1:
        node.log_handler.log("\tWarning: Mysql is running, pid:"+output+" shutting down")
        conn.run_command("kill -9 "+output)
        time.sleep(5)
    else:
        node.log_handler.log("\tMysql was not running")
    node.log_handler.log("Starting mysql server on port "+options.mysqlPort)
    strconfig="nohup "+options.mysqlPath+"mysqld --innodb_lock_wait_timeout=1 --user=mysql --port "+options.mysqlPort+" &"
    #print strconfig
    conn.run_command(strconfig,True)
    node.log_handler.log("\tWarning: waiting while the daemon starts up")
    time.sleep(5)
    node.log_handler.log("Deploying database. Any existing data will be lost!")

    #setting up environment
    node.log_handler.log("\tdeploying Schema")
    strconfig="cd "+options.dir+" && " #reassigned
    strconfig = strconfig+options.mysqlPath+"mysql --host=127.0.0.1 --port="+options.mysqlPort
    strconfig = strconfig+" --user="+options.mysqlUser+" --password="+options.mysqlPasswd+"  < "+options.sqlFile 
    #print strconfig
    conn.run_command(strconfig)
    
    node.log_handler.log("\tinstalling stored procedures")
    strconfig="cd "+options.dir+" && " #reassigned
    strconfig = strconfig+options.mysqlPath+"mysql --host=127.0.0.1 --port="+options.mysqlPort
    strconfig = strconfig+" -D "+options.mysqlDb+" --user="+options.mysqlUser+" --password="+options.mysqlPasswd+"  < "+options.sqlprocedures 
    #print strconfig
    conn.run_command(strconfig)

    node.log_handler.log("\tfilling up initial data")
    strconfig="cd "+options.dir+" && " #reassigned
    strconfig = strconfig+options.mysqlPath+"mysql --host=127.0.0.1 --port="+options.mysqlPort
    strconfig = strconfig+" -D "+options.mysqlDb+" --user="+options.mysqlUser+" --password="+options.mysqlPasswd+"  < "+options.sqlinitialdata 
    #print strconfig
    conn.run_command(strconfig)
    
    
    node.log_handler.log("Done!!!!\n")
    conn.close()
    del conn
    
def set_role(options,node):
     #deploy comandos to remote host
    if options.dir == None:
        options.dir = "/var/tmp/txmud/"
    node.log_handler.log("Trying to connect to remote host")
    conn = Connector(node)
    conn.open()
    
    strconfig = " echo '"+options.role+"' > "+options.dir+"role" 
    node.log_handler.log("setting role "+options.role+" for "+node.hostname)
    #print strconfig
    conn.run_command(strconfig)
    
    node.log_handler.log("Done")
    conn.close()
    del conn

def get_role(options,node):
     #deploy comandos to remote host
    if options.dir == None:
        options.dir = "/var/tmp/txmud/"
    node.log_handler.log("Trying to connect to remote host")
    conn = Connector(node)
    conn.open()
    
    strconfig = " cat "+options.dir+"role" 
    #print strconfig
    output = conn.run_command(strconfig)
    node.log_handler.log("Currently "+node.hostname+" is playing "+output)
    conn.close()
    del conn

def reset(node,options):
    #deploy comandos to remote host
    global txmud_root
    #node.log_handler.log("Trying to connect to remote host")
    conn = Connector(node)
    conn.open()
    
    while True:
        output = conn.run_command("ps aux | grep $USER | grep mysql | grep -v grep  | wc -l")
        if int(output) != 0 :
            print node.hostname+" killing mysql"
            conn.run_command("killall -9 mysqld mysql mysqld_safe")
            time.sleep(1)
        else :
            print node.hostname+" mysql killed "
            break

    #pidcoord='ps ax | grep java | grep coordinator | cut -f 2 -d " "'
    #pidstorageshim='ps ax | grep java | grep coordinator | cut -f 2 -d " "'
    #pidtomcat='ps ax | grep java | grep tomcat | cut -f 1 -d " "'
    while True : 
        output = conn.run_command("ps aux | grep $USER | grep 'coordinator\|storageshim\|tomcat\|rbe' | grep -v grep  | wc -l")
        if int(output) != 0 :
            print node.hostname+" killing java "+output
            conn.run_command("killall -9 java")
            time.sleep(1)
        else :
            print node.hostname+" java killed"
            break
        
    conn.run_command("cd "+txmud_root+" && ant dist ")
    conn.run_command("cd "+tpcw_root+" && ant inst ")
    conn.close()
    del conn

def test(node,options):
     #deploy comandos to remote host
    #print node
#    return 1
    #print "Trying to connect to remote host "+node.hostname
    conn = Connector(node)
    
    conn.open()
#    strconfig = "svn up /var/tmp/txmud " 
    strconfig = "ls / " 
    #print strconfig
    output = conn.run_command(strconfig)
#    print output
    if len(output) > 1:
        print "Trying to connect to remote host "+node.hostname+" Success!!"
    else:
        print "Trying to connect to remote host "+node.hostname+" Failure!!"
    conn.close()
    del conn

def update(node,options):
     #deploy comandos to remote host
    #print node
#    return 1
    global txmud_root
    conn = Connector(node)
    
    conn.open()
    strconfig = "svn up  "+txmud_root 
    #print strconfig
    output = conn.run_command(strconfig)
    print output
    if len(output) > 1:
        print "Trying to connect to "+node.hostname+" Success"
        node.log_handler.log(node.hostname+" update command: " + output)
    else:
        print "Trying to connect to "+node.hostname+" Failure"
    conn.close()
    del conn
    
def run(node,options,background=False):
     #deploy comandos to remote host
    #print node
#    return 1
    print "deploy commands"
    
    '''while True:'''
    conn = Connector(node)
    
    conn.open()
    command=""
    if background:
        print "command will be running in background"
        node.log_handler.log(node.hostname+" command: " + options.runbg)
        command=options.runbg
    else:
        node.log_handler.log(node.hostname+" command: " + options.run)
        command=options.run
    
    output = conn.run_command(command,background)
    node.log_handler.log(node.hostname+" "+output)
    print output
    if len(output) > 1:
        print "Trying to connect to "+node.hostname+" Success"
        node.log_handler.log(node.hostname+" update command: " + output)
        '''break'''
    else:
        print "Trying to connect to "+node.hostname+" Failure"
    conn.close()
    del conn    
    
    
def start(node,options): 
     #deploy comandos to remote host
    if options.dir == None:
        options.dir = "/var/tmp/txmud/"
    node.log_handler.log("Trying to connect to remote host")
    conn = Connector(node)
    conn.open()

    
    if options.role == "user":
        strconfig = "cd "+options.dir+"stable_new_message/user && "
        strconfig = strconfig + "nohup python "
        strconfig = strconfig+options.dir+"stable_new_message/user"
        strconfig = strconfig+"/user.py "
        strconfig = strconfig+options.userId+" "
        strconfig = strconfig+options.proxyAddr+" "
        strconfig = strconfig+options.description+" "
        strconfig = strconfig+options.txtype+" "        
        if options.txtype == 'm':
            strconfig = strconfig+str(options.txratio)
        strconfig = strconfig+" 2>&1 > /dev/null &"        
        node.log_handler.log(strconfig)
        conn.run_command(strconfig,True)
        time.sleep(3)
        output = conn.run_command("ps ax | grep user.py | grep -v grep").strip()
        if len(output) > 1:
            node.log_handler.log("\tUser is running! success!")
        else:
            node.log_handler.log("\tWarning, user might not be  running or it's finished too fast")
    elif options.role == "proxy":
        strconfig = "cd "+options.dir+"stable_new_message/webshim && "
        strconfig = strconfig + "nohup python "
        strconfig = strconfig+options.dir+"stable_new_message/webshim"
        strconfig = strconfig+"/webClient.py "
        strconfig = strconfig+options.description+" 2>&1 > /dev/null &"
        node.log_handler.log(strconfig)
        conn.run_command(strconfig,True)
        time.sleep(3)
        output = conn.run_command("ps ax | grep webClient.py | grep -v grep").strip()
        if len(output) > 1:
            node.log_handler.log("\tProxy is running! success!")
        else:
            node.log_handler.log("\tError, Proxy is not running!!")    
    elif options.role == "coordinator":
        strconfig = "cd "+options.dir+"stable_new_message/coordinator && "
        strconfig = strconfig + "nohup python "
        strconfig = strconfig+options.dir+"stable_new_message/coordinator"
        strconfig = strconfig+"/Coordinator.py "
        strconfig = strconfig+options.description+" 2>&1 > /dev/null &"
        node.log_handler.log(strconfig)
        conn.run_command(strconfig,True)
        time.sleep(3)
        output = conn.run_command("ps ax | grep Coordinator.py | grep -v grep").strip()
        if len(output) > 1:
            node.log_handler.log("\tCoordinator is running! success!")
        else:
            node.log_handler.log("\tError, Coordinator is not running!!")    
    elif options.role == "mshim":
        strconfig = "cd "+options.dir+"stable_new_message/storageshim && "
        strconfig = strconfig + "nohup python "
        strconfig = strconfig+options.dir+"stable_new_message/storageshim"
        strconfig = strconfig+"/mysqlShim.py "
        strconfig = strconfig+options.description+" 2>&1 > /dev/null &"
        node.log_handler.log(strconfig)
        conn.run_command(strconfig,True)
        time.sleep(3)
        output = conn.run_command("ps ax | grep mysqlShim.py | grep -v grep").strip()
        if len(output) > 1:
            node.log_handler.log("\tMysqlShim is running! success!")
        else:
            node.log_handler.log("\tError, MysqlShim is not running!!")    
    elif options.role == "storage":
        node.log_handler.log("Checking whether mysql is running")
        output = conn.run_command("pidof mysqld").strip()
        if len(output) > 1:
            node.log_handler.log("\tWarning: Mysql is running, pid:"+output+" do nothing")
        else:
            node.log_handler.log("\tMysql was not running - starting")
            print "mysql path",options.mysqlPath
            print "mysql port",options.mysqlPort
            
            strconfig="nohup "
            strconfig= strconfig+options.mysqlPath+"mysqld --innodb_lock_wait_timeout=1 --user=mysql --port "+options.mysqlPort+" &"
            node.log_handler.log(strconfig)
            conn.run_command(strconfig,True)
            node.log_handler.log("Warning: waiting while the daemon loads up..")
            time.sleep(5)

    conn.close()
    del conn


def getresult(node,options): #TODO!!
     #deploy comandos to remote host
    if options.dir == None:
        options.dir = "/var/tmp/txmud/"
    node.log_handler.log("Trying to connect to remote host")
    conn = Connector(node)
    conn.open()

    if options.role != "Free":
        strconfig = options.dir+options.result
        node.log_handler.log("downloading "+strconfig)
        conn.get(strconfig,"res-"+options.result)
        cmd="mv "+strconfig+" "+strconfig+".backup."+get_str_timestamp()
        print "command: ",cmd
        conn.run_command(cmd)

    conn.close()
    del conn    

def get_status(options,node):    
    conn = Connector(node)
    conn.open()
    if options.role == "user":
        output = conn.run_command("ps ax | grep user.py | grep -v grep").strip()
        if len(output) > 1:
            node.log_handler.log("\tUser is running! success!")
        else:
            node.log_handler.log("\tWarning, user might not be  running or it's finished too fast")
    elif options.role == "coordinator":
        output = conn.run_command("ps ax | grep Coordinator.py | grep -v grep").strip()
        if len(output) > 1:
            node.log_handler.log("\tCoordinator is running! success!")
        else:
            node.log_handler.log("\tCoordinator is not running")
    elif options.role == "proxy":
        output = conn.run_command("ps ax | grep webClient.py | grep -v grep").strip()
        if len(output) > 1:
            node.log_handler.log("\tUser is running! success!")
        else:
            node.log_handler.log("\tCoordinator is not running")
    elif options.role == "mshim":
        output = conn.run_command("ps ax | grep mysqlShim.py | grep -v grep").strip()
        if len(output) > 1:
            node.log_handler.log("\tUser is running! success!")
        else:
            node.log_handler.log("\tCoordinator is not running")
    elif options.role == "storage":
        output = conn.run_command("ps ax | grep mysqld | grep -v grep").strip()
        if len(output) > 1:
            node.log_handler.log("\tUser is running! success!")
        else:
            node.log_handler.log("\tCoordinator is not running")
    conn.close()
    del conn    
    
    
'''    
def tpcw_mysql(node,options):
    TXM_DATABASE="139.19.158.67"
    TXM_PROXY="139.19.158.73"
    TXM_USER="139.19.158.66"
    TPCW_DIR="/home/dcfp/workspace/txmud_spd/src/applications/tpc-w-fenix"

    

    
    PREPARE="cd $TPCW_DIR && ant clean dist -DdcId 0  -DproxyId=0 -Dnum.eb=10 -Dnum.items=1000 -Dlogicalclock=0-0 -Dbackend=mysql -Dmysql_host=$TXM_DATABASE -Dmysql_port=53306 -Dtopologyfile=tpcw_txmud-allblue.xml"
    POPULATE="cd $TPCW_DIR && ant gendb" 
    IMAGES="cd $TPCW_DIR && ant genimg -Dnum.eb=10 -Dnum.items=1000"
    INST="cd $TPCW_DIR && ant inst "

    if options.tpcwcmd == "prepare":
    
    python tpcw_remote_setup.py -d $TXM_DATABASE  -u $USERNAME -p "$PASSWORD" --run "$TPCW_DIR/tpcw-clean.sh install_proxy"
    #python tpcw_remote_setup.py -d $TXM_DATABASE  -u $USERNAME -p "$PASSWORD" --run "$TPCW_DIR/tpcw-clean.sh install_database" 
    python tpcw_remote_setup.py -d $TXM_PROXY  -u $USERNAME -p "$PASSWORD" --run "$TPCW_DIR/tpcw-clean.sh install_proxy"
    python tpcw_remote_setup.py -d $TXM_USER  -u $USERNAME -p "$PASSWORD" --run "$TPCW_DIR/tpcw-clean.sh install_proxy"
elif [ $1 = "deploy" ]
    then
#prepare all nodes
    python tpcw_remote_setup.py -d $TXM_DATABASE  -u $USERNAME -p "$PASSWORD" --run "$PREPARE"
    python tpcw_remote_setup.py -d $TXM_PROXY  -u $USERNAME -p "$PASSWORD" --run "$PREPARE"
    python tpcw_remote_setup.py -d $TXM_USER  -u $USERNAME -p "$PASSWORD" --run "$PREPARE"
    
#database
    python tpcw_remote_setup.py -d $TXM_DATABASE  -u $USERNAME -p "$PASSWORD" --run "$POPULATE"
    
#proxy
    python tpcw_remote_setup.py -d $TXM_PROXY  -u $USERNAME -p "$PASSWORD" --run "$IMAGES"
    python tpcw_remote_setup.py -d $TXM_PROXY  -u $USERNAME -p "$PASSWORD" --run "$INST"
    
elif [ $1 = "reset" ]
    then    
    python tpcw_remote_setup.py -d $TXM_DATABASE  -u $USERNAME -p "$PASSWORD" -r storage --command "reset"
    python tpcw_remote_setup.py -d $TXM_PROXY  -u $USERNAME -p "$PASSWORD" -r proxy --command "reset"
    python tpcw_remote_setup.py -d $TXM_USER  -u $USERNAME -p "$PASSWORD" -r user --command "reset"
fi    
echo "done"
    
'''    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
        
    
    
    

def main():
 
    parser = OptionParser()
     
     
    
    parser.add_option("-r", "--role",action="store", type="string", dest="role",
                  help="configure the role: user, proxy, coordinator, mshim and storage")
    parser.add_option("-d", "--dest",action="store", type="string", dest="targetnode",
                  help="IP address of remote node to configure")
    parser.add_option("-u", "--user",action="store", type="string", dest="username",
                  help="username used to connect and configure the remote host")   
    parser.add_option("-p", "--passwd",action="store", type="string", dest="passwd",
                  help="password of user account or used on protecting keyfile")    
    parser.add_option("-k", "--keyfile",action="store", type="string", dest="keyfile",
                  help="path of keyfile used to connecto to remote host") 

    #Command options=====================================================================
    commands_opts = OptionGroup(parser, "Commands",
                               "these options are valid for all roles, they are used to control the experiment")
    commands_opts.add_option("--command", action="store", type="string", dest="command",
                  help="run the following commands start, reset, test, update or get. You must also specify othe role of the host")
    
    commands_opts.add_option("--run", action="store", type="string", dest="run",
                  help="command to run")
    commands_opts.add_option("--runbg", action="store", type="string", dest="runbg",
                  help="command to run in background")
        
    commands_opts.add_option("--result", action="store", type="string", dest="result",
                  help="this option is only applied to get command [above] and specifies the filename of the result to fetch on remote host")
    parser.add_option_group(commands_opts)
    #General options=====================================================================
    general_opts = OptionGroup(parser, "General options",
                               "these options are valid for all roles, unless mentioned differently")
    general_opts.add_option("--dcid","--datacenter-id", action="store",
                                type="string", dest="dcId",
                                help="Set the datacenter identifier number")
    #path to remote files
    general_opts.add_option("--dir","--base-dir", action="store",
                                type="string", dest="dir",
                                help="Set the location of configuration file on remote node")
    #dcNum  --ask cheng!!
    general_opts.add_option("--dcnum","--datacenters-number", action="store", 
                                type="string", dest="dcNum",
                                help="Total numbers of datacenters.\n It's applied for coordinators, proxies and shim")
    general_opts.add_option("--description", action="store", 
                                type="string", dest="description",
                                help="simulation description appended to filename",default="")
    #max number of connections
    general_opts.add_option("--max-connections",action="store",
                                type="string", dest="maxconnections",
                                help="Set the size of connection pool, this option is only applied for proxy and mysqlshim")

    parser.add_option_group(general_opts)
    #Coordinator options=================================================================
    coordinator_opts = OptionGroup(parser, "Coordinator options",
                    "these options are used to configure the coordinator")
    #identifier
    coordinator_opts.add_option("--cd-id","--coordinator-identifier", action="store",
                                type="string", dest="coordinatorId",
                                help="Coordinator's identifier for tracking transactions in log file")
    #daemon port
    coordinator_opts.add_option("--cd-port","--coordinator-port", action="store", 
                                type="string", dest="cdPort",
                                help="Port to bind the coordinator daemon")
    #daemon port
    coordinator_opts.add_option("--cd-period", action="store", 
                                type="string", dest="cdperiod",
                                help="period for exchanging data")
    
    #coordinators address list of 
    coordinator_opts.add_option("--cd-rdaddr-list","--remote-datacenter-addr_list", action="store", 
                                type="string", dest="remoteDcAddrList",
                                help='List of IP addresses of coordinators to connect between quotes'+
                                ' and separated by spaces ex: "192.168.0.1 192.168.0.2 10.0.0.1"')
    #coordinators port list of 
    coordinator_opts.add_option("--cd-rdport-list","--remote-datacenter-port", action="store", 
                                type="string", dest="remoteDcPortList",
                                help='List of remote ports on coordinators on each address between quotes'+
                                ' and separated by spaces ex: "50001 50001 50001"')
    parser.add_option_group(coordinator_opts)
    #Mshim options=======================================================================
    shim_opts = OptionGroup(parser, "Shim options",
                        "these options are used to configure mysql shim connector")
    #daemon parameters
    shim_opts.add_option("--ms-port","--mshim-port", action="store", 
                        type="string", dest="msPort",
                        help="Port number for binding mysql shim daemon")
    shim_opts.add_option("--ms-id","--mshim-identifier", action="store", 
                        type="string", dest="mshimId",
                        help="Port number for binding mysql shim daemon")
    #storage parameters
    shim_opts.add_option("--ms-dbaddr","--storage-addr", action="store", 
                        type="string", dest="dbHost",
                        help="IP address of Mysql database server")

    shim_opts.add_option("--ms-dbport","--storage-port", action="store", 
                        type="string", dest="dbPort",
                        help="Port number of remote Mysql database server")
    shim_opts.add_option("--ms-dbuser","--storage-user", action="store", 
                        type="string", dest="dbUser",
                        help="Username for connectiong to remote Mysql database server")
    shim_opts.add_option("--ms-dbpasswd","--storage-passwd", action="store", 
                        type="string", dest="dbPasswd",
                        help="Password for connectiong to remote Mysql database server")
    shim_opts.add_option("--ms-dbname","--storage-Name", action="store", 
                        type="string", dest="dbName",
                        help="Database on remote Mysql database server")
    #connection to coordinator
    shim_opts.add_option("--ms-cdaddr","--mshim-coordinator-addr", action="store", 
                        type="string", dest="cdAddr",
                        help="IP address of coordinator node")
    shim_opts.add_option("--ms-cdport","--mshim-coordinator-port", action="store", 
                        type="string", dest="cdPort",
                        help="Port number of coordinator node")
    #connection to shim peers
    shim_opts.add_option("--ms-paddr-list","--mshim-peer-addr-list", action="store", 
                        type="string", dest="remoteStoreAddrList",
                        help="IP address of MShim peer at a remote datacenter")
    shim_opts.add_option("--ms-pport-list","--mshim-peer-port-list", action="store", 
                        type="string", dest="remoteStorePortList",
                        help="Port of MShim peer at a remote datacenter")
    
    shim_opts.add_option("--ms-period",action="store", 
                        type="string", dest="period",
                        help="period of exchanging informations")
    
    parser.add_option_group(shim_opts)
    #User agent options================================================================================
    user_opts = OptionGroup(parser, "User options",
                               "these options configures user agents for simulating access to store")
    user_opts.add_option("--uid","--user-id", action="store",
                                type="string", dest="userId",
                                help="User identifier")
    user_opts.add_option("--proxy","--proxy-addr", action="store",
                                type="string", dest="proxyAddr",
                                help="Address of the proxy to connect")    
    user_opts.add_option("--pport","--proxy-port", action="store",
                                type="string", dest="proxyPort",
                                help="Proxy port on remote host")    
    user_opts.add_option("--transactions",action="store",
                            type="string", dest="transactions",
                            help="Proxy port on remote host")    

    parser.add_option_group(user_opts)
    
    user_opts2 = OptionGroup(parser, "User options - start up script",
                               "these options are used for starting user agents - only")
    user_opts2.add_option("--txtype",action="store",
                            type="string", dest="txtype",
                            help="transaction type: [r]ed, [b]lue or [m]ixed",default ='m')    
    user_opts2.add_option("--txratio",action="store",
                            type="int", dest="txratio",
                            help="percentage of red over blue operations",default =2)    
    parser.add_option_group(user_opts2)

    #Proxy options================================================================================
    proxy_opts = OptionGroup(parser, "Proxy options",
                        "these options are used to configure a proxy application")
    proxy_opts.add_option("--px-id","--proxy-identifier", action="store",
                                type="string", dest="proxyId",
                                help="Set the datacenter identifier number")
    proxy_opts.add_option("--px-port",action="store",
                                type="string", dest="pxport",
                                help="Set the datacenter identifier number")

    #connection to coordinator
    proxy_opts.add_option("--px-cdaddr","--proxy-coordinator-addr", action="store", 
                        type="string", dest="xcdAddr",
                        help="IP address of coordinator node")
    proxy_opts.add_option("--px-cdport","--proxy-coordinator-port", action="store", 
                        type="string", dest="xcdPort",
                        help="Port number of coordinator node")
    #connection to shim peers
    proxy_opts.add_option("--px-peers-list",action="store", 
                        type="string", dest="mshimpeers",
                        help="IP address of MShim peer at a remote datacenter")
#    proxy_opts.add_option("--px-pport-list","--proxy-mshim-port-list", action="store", 
#                        type="string", dest="mshimPortList",
#                        help="Port of MShim peer at a remote datacenter")
    
    parser.add_option_group(proxy_opts)
    
    #Mysql options-------------------------------------------------------------
    storage_opts = OptionGroup(parser, "Storage options",
                               "these options configure a mysql server on the remote host")
    storage_opts.add_option("--mysql-port","--mysql-port", action="store",
                                type="string", dest="mysqlPort",
                                help="set the port for mysql server listen to")
    storage_opts.add_option("--mysql-path","--mysql-daemon-path", action="store",
                                type="string", dest="mysqlPath",
                                help="Path to mysqld binary at remote host")
    storage_opts.add_option("--mysql-user","--mysql-username", action="store",
                                type="string", dest="mysqlUser",
                                help="username with root permitions for initializing database")
    storage_opts.add_option("--mysql-passwd","--mysql-password", action="store",
                                type="string", dest="mysqlPasswd",
                                help="password of user to access database")
    storage_opts.add_option("--mysql-dbname",action="store",
                                type="string", dest="mysqlDb",
                                help="sql script at a remote host used to create a database")
    storage_opts.add_option("--mysql-sqlfile",action="store",
                                type="string", dest="sqlFile",
                                help="sql script at a remote host used to create a database")
    storage_opts.add_option("--mysql-procedures",action="store",
                                type="string", dest="sqlprocedures",
                                help="sql script at a remote host used to install procedures into a database")
    storage_opts.add_option("--mysql-sqldata",action="store",
                                type="string", dest="sqlinitialdata",
                                help="sql script at a remote host used to inicialize a database")
    

    
    
    parser.add_option_group(storage_opts)
    #--------------------------------------------------------------------------
    (options, args) = parser.parse_args()
    #print options
    #print args    
    
    if options.role == "tpcw_mysql": 
       tpcw_mysql(options)
    if options.role == "tpcw_txmud": 
       tpcw_txmud(options)
   
        
    if options.targetnode is None or options.username is None: 
        print "Error: insuficient parameters for accessing remote node.\nType -h usage instructions"
        return -1

    if options.passwd is None and options.keyfile is None: 
        print "Error: you must provide at least password or a keyfile"
        return -1
    node=Node(hostname=options.targetnode,username=options.username,password=options.passwd,keyfile=options.keyfile)  
    
    if options.run != None:
        print "Lets deploy the command"
        run(node,options)
        return 1
    
    if options.runbg != None:
        print "Lets deploy the command"
        run(node,options,True)
        return 1
    


    if options.command == "reset":
        reset(node,options)
        return 1
    elif options.command == "test":
        test(node,options)
        return 1
    elif options.command == "start":
        start(node,options)
        return 1
    elif options.command == "get":
        getresult(node,options)
        return 1
    elif options.command == "update":
        update(node,options)
        return 1



    if options.dcId is None and (options.role != "user") and (options.run is not None):
        print "You need to set a datacenter ID"
        return -1
    
    if options.role == "user":
        setup_user(options,node)
    elif options.role == "proxy":
        setup_proxy(options,node)
    elif options.role == "mshim":
        setup_mshim(options,node)
    elif options.role == "coordinator":
        setup_coordinator(options,node)
    elif options.role == "storage":
        setup_storage(options,node)
    
        
   
if __name__ == "__main__":
    
    main()   

