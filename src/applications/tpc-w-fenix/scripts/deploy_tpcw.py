#!/bin/python
import string
import os
import tempfile

USERNAME="dcfp"
#to use in thor nodes first use ssh-keygen to generate the key, then use ssh-copy-id to localhost to enable login using key file
AUTH_MODE="-k" #AUTH_MODE="-k"
CREDENTIAL=["/home/$USERNAME/.ssh/id_rsa","/home/$USERNAME/.ssh/id_rsa"]
TXMUD_ROOT="/home/"+USERNAME+"/java" 			  #TXMUD_ROOT="/home/$USERNAME/devel/txmud-trunk"
TPCW_ROOT=TXMUD_ROOT+"/src/applications/tpc-w-fenix"
TPCW_SCRIPTS=TPCW_ROOT+"/scripts"
TPCW_OUTPUTDIR="/var/tmp/"+USERNAME+"/output/tpcw"
TXMUD_OUTPUTDIR="/var/tmp/"+USERNAME+"/output/txmud"
TPCW_BINARIES=TPCW_OUTPUTDIR+"/dist"
TXMUD_BINARIES=TXMUD_OUTPUTDIR+"/dist"

#TPCW PARAMETERS
EB=50    #pay attention to database file. it should match with the numbers chosen 
ITEM=10000 
TPCW_MIX=2 #1 browsing, 2 shopping, 3 ordering
TPCW_WARMUPTIME=60
TPCW_TEARDOWNTIME=60
TPCW_MEASUREMENTINTERVAL=300
TPCW_THINKTIME=0.0
BACKEND="txmud" #options: mysql, txmud, txmud_ssc, scratchpad
RESULTFILE_TAG="TxMudRedBlue"  #it should be defined according to table color and the backend: TxMudAllBlue,TxMudRedBlue,Mysql
DATABASETGZ="mysql-5.5.18.zero.lc0-0-0.50eb10kitem.sharedcart.chengli.tgz"

#DATABASE
DBPORT=53306
DBUSER="sa"
DBUSERPW=""
DBNAME="mysql_redblue_semantics"

###############Datacenter configuration###############
cdPort=60000 # coordinator port
ssPort=60001
wpPort=60002
############THOR MPI################
dc0={
	"coordinator":'139.19.158.1', 
	"proxy":['139.19.158.2'], 
 	"database":['139.19.158.3'],
	"datawriter":['139.19.158.4'],
	"user":['139.19.158.5']
	}
dc1={
	"coordinator":'139.19.158.6', 
	"proxy":['139.19.158.7'], 
 	"database":['139.19.158.8'],
	"datawriter":['139.19.158.9'],
	"user":['139.19.158.10']
	}
DATACENTERS=[dc0,dc1]
DATACENTERNUM=len(DATACENTERS)
DATABASENUM=DATACENTERNUM # per datacenter
PROXYNUM=len(dc0['proxy'])+len(dc1['proxy'])
#-----------------------------------------------------------------------------
##update the configuration files accordingly - they will be overwritten!!!
TOPOLOGYXMLFILENAME="tpcwtxmud_multidc_"+USERNAME+".xml"
DATABASEXMLFILENAME="tpcwtxmud_multidcdb_"+USERNAME+".xml"
#experiment tag
EXPERIMENT_TAG=""
if len(DATACENTERS) > 1:
	EXPERIMENT_TAG="MultiDataCenter_"+BACKEND+"_"+RESULTFILE_TAG
else:
	EXPERIMENT_TAG="SingleDataCenter_"+BACKEND+"_"+RESULTFILE_TAG

#-----------------------------------------------------------------------------
#commands
CMD_INSTALL_DEPENDENCIES=TPCW_SCRIPTS+"/tpcwtxmud_setup.sh deploy_dependencies"
CMD_BUILD_TPCW_IMAGES="cd %s && ant genimg -Dnum.eb=%d -Dnum.item=%d -DoutputDir=%s"%(TPCW_ROOT,EB,ITEM,TPCW_OUTPUTDIR)
CMD_INSTALL_TPCW_WEBSITE="cd %s && ant inst -DoutputDir=%s"%(TPCW_ROOT,TPCW_OUTPUTDIR)
CMD_POPULATE_DATABASE="%s/tpcwtxmud_setup.sh populate %s localhost  %d %d %d"%(TPCW_SCRIPTS,BACKEND,DBPORT,EB,ITEM) 
#TODO fix the loop for more than one proxy per datacenter!
CMD_DBCLEANUP="rm -rf /tmp/mysql.sock /var/tmp/%s/mysql-5.5.18 && echo 'done'"%(USERNAME)
CMD_DBDEPLOY="tar xvf /home/%s/%s -C /var/tmp/%s && echo 'done'"%(USERNAME,DATABASETGZ,USERNAME)
CMD_WAIT_USERS="while  [ \`ps aux | grep $USERNAME | grep java | grep RBE | grep -v grep | wc -l\`  -ne 0 ]; do  echo 'waiting...' && sleep 10 ;  done"
#topology file content
def get_configuration_command(dcid=0,pxid=0):
	datacenter=DATACENTERS[dcid]
	database=datacenter['database'][0]
	str="cd %s && ant clean dist -DoutputDir=%s -DdcId=%d "%(TPCW_ROOT,TPCW_OUTPUTDIR,dcid)
	str+="-DproxyId=%d -Dtotaldc=%d -Dtotalproxy=%d "%(pxid,DATACENTERNUM,PROXYNUM)
	str+="-Dnum.eb=%d -Dnum.item=%d -Dlogicalclock=%s -Dbackend=%s "%(EB,ITEM,get_logical_clock(),BACKEND)
	str+="-Dmysql_host=%s -Dmysql_port=%d -Dtopologyfile=%s "%(database,DBPORT,TOPOLOGYXMLFILENAME)
	str+="-Dthinktime=%d "%(TPCW_THINKTIME)
	return str

def get_start_user_command(dcid=0,proxyid=0,users=1):
	str="cd %s && "%(TPCW_BINARIES) 	
	str+=" ./rbe.sh" 
	str+=" -w http://%s:8080/tpcw/"%(proxyid) 
	str+=" -b %s "%(RESULTFILE_TAG) 	
	str+=" -l %d"%(dcid)
	str+=" -t %s"%(TPCW_MIX) 
	str+=" -u %d"%(TPCW_WARMUPTIME)  
	str+=" -d %d"%(TPCW_TEARDOWNTIME) 
	str+=" -i %d"%(TPCW_MEASUREMENTINTERVAL) 
	str+=" -n %d"%(users)
	return str

def get_start_datawriter_command(dcid=0,datawriterid=0,nscratchpads=1):
	# StubStorage config.xml db.xml dcId stroageId threadcount tcnnodelay scratchpadNum
	str=" echo '' > /tmp/storageshim.%d.log && "%(dcid)
	str+=" java -jar %s/storageshim-big.jar "%(TXMUD_BINARIES)
	str+=" %s/%s"%(TPCW_ROOT,TOPOLOGYXMLFILENAME) 
	str+=" %s/%s %d %d 20 true %d &> /tmp/storageshim.%d.log "%(TPCW_ROOT,DATABASEXMLFILENAME,dcid,datawriterid,nscratchpads)
	return str

def get_start_coordinator_command(dcid=0):
#Coordinator config.xml dcId coordinatorId threadCount tokensize tcpnodelay blueTimeOut bluequittime
	str="java -jar %s/coordinator-big.jar"%(TXMUD_BINARIES) 
	str+="%s/%s %d 0 20 1000 true 1000000000 50000000  &> /tmp/coordinator.%d.log"%(TPCW_ROOT,TOPOLOGYXMLFILENAME,dcid,dcid)
	return str

def get_logical_clock():
	LOGICAL_CLOCK="0"
	for i in range(DATACENTERNUM):
		LOGICAL_CLOCK+="-0"
	return LOGICAL_CLOCK

def create_configuration_files():
	file = open(TOPOLOGYXMLFILENAME,'w')

	#DATACENTER TOPOLOGY FILE CONFIGURATION
	file.write("<dataCenters dcNum='%d'>\n"%(DATACENTERNUM))
	for i in range(DATACENTERNUM):
		datacenter=DATACENTERS[i]
		ndatawriters=len(datacenter['datawriter'])
		nproxies=len(datacenter['proxy'])
		
		file.write("\t<dataCenter cdPort='%d' cdIP='%s'>\n" %(cdPort,datacenter['coordinator']))
		#datawriter configuration
		file.write("\t\t<storageShims ssNum='%d'>\n"%ndatawriters)
		for j in range(ndatawriters):
			file.write("\t\t\t<storageShim ssIP='%s' ssPort='%d'/>\n"%(datacenter['datawriter'][j],ssPort)) 		
		file.write("\t\t</storageShims>\n")
		#webproxy configuration		
		file.write("\t\t<webProxies wpNum='%d'>\n"%nproxies)
		for j in range(nproxies):
			file.write("\t\t\t<webproxy wpIP='%s' wpPort='%d'/>\n"%(datacenter['proxy'][j],wpPort)) 		
		file.write("\t\t</webProxies>\n")
		#we're good to go
		file.write("\t</dataCenter>\n")
	file.write("</dataCenters>\n")
	file.close() 

	#DATABASE FILE CONFIGURATION
	file = open(DATABASEXMLFILENAME,'w')
	file.write("<?xml version='1.0' encoding='UTF-8'?>\n")

	for i in range(DATACENTERNUM):
		datacenter=DATACENTERS[i]
		ndatabases=len(datacenter['database'])
		file.write("<databases dbNum='%d'>\n" %(ndatabases))
		
		for j in range(ndatabases):
			file.write("\t <database> \n") 
			file.write("\t\t dcId='%d'  \n"%(i))
			file.write("\t\t dbId='%d'  \n"%(j))
			file.write("\t\t dbHost='%s'\n"%(datacenter['database'][j])) 
			file.write("\t\t dbPort='%d'\n"%(DBPORT))
			file.write("\t\t dbUser='%s'\n"%(DBUSER))
			file.write("\t\t dbPwd='%s' \n"%(DBUSERPW))
			file.write("\t\t dbName='%s'\n"%(DBNAME)) 	
			file.write("\t\t tableList='address,author,cc_xacts,country,customer,item,order_line,orders,shopping_cart,shopping_cart_line'\n") 	
			file.write("\t\t tableLWW='address,author,cc_xacts,country,customer,order_line,shopping_cart_line,shopping_cart'\n")		
			file.write("\t\t talbeOps=''\n")		
			file.write("\t\t redTable='address,author,cc_xacts,country,customer,order_line,shopping_cart_line,shopping_cart'\n") 
			file.write("\t\t blueTable='orders,item'\n")
			file.write("\t\t url_prefix='jdbc:mysql://'\n") 
			file.write("\t </database>\n")
		
		file.write("</databases>\n")
	file.close() 

def install_txmud(ipaddr="127.0.0.1",dcid=0):
	os.system("tpcwtxmud_remote_setup.py -d %s  -u %s -k '%s' --run '%s'"% \
			(ipaddr,USERNAME,CREDENTIAL[dcid],CMD_INSTALL_DEPENDENCIES))
				
#install_database
# arg: host credential
#------------------------------------------------------------------------------
def install_database(ipaddr="127.0.0.1",dcid=0): #host credential
	#download the already populated database from my home directory 
	#pay attention because the database location depends on the username used to compile it
	#we can use a parameter that says the url of the database and another one to check the md5 of it
	os.system("tpcwtxmud_remote_setup.py -d %s  -u %s -k '%s' --run '%s/tpcwtxmud_setup.sh install_database'"%\
			(ipaddr,USERNAME,CREDENTIAL[dcid],TPCW_SCRIPTS))

#------------------------------------------------------------------------------
# install_proxy
# arg: host credential
#------------------------------------------------------------------------------
def install_proxy(ipaddr="127.0.0.1",dcid=0):
	 os.system("tpcwtxmud_remote_setup.py -d %s  -u %s -k '%s' --run '%s/tpcwtxmud_setup.sh install_proxy'"%\
			(ipaddr,USERNAME,CREDENTIAL[dcid],TPCW_SCRIPTS))
			
#------------------------------------------------------------------------------
# install_all
#------------------------------------------------------------------------------
def install_all():
	for i in range(DATACENTERNUM):
		datacenter=DATACENTER[i]
		#install_database ${TXM_DATABASE[$i]} ${CREDENTIAL[$i]}
		install_proxy(datacenter['proxy'], i)
		install_txmud(datacenter['proxy'], i)
		
		install_proxy(datacenter['user'], i)
		install_txmud(datacenter['user'], i)
		
		install_txmud(datacenter['datawriter'], i)
		install_txmud(datacenter['coordinator'], i)
#------------------------------------------------------------------------------
# isrunning check whether a remote process is running or not
# arg: host credential processname
#------------------------------------------------------------------------------	
def isrunning(host="127.0.0.1",credential=CREDENTIAL[0],process="coordinator"):
	
	str=os.popen("tpcwtxmud_remote_setup.py -d %s  -u %s -k '%s' --run 'ps aux | grep %s | grep -v grep'"%\
			(ipaddr,USERNAME,CREDENTIAL[dcid],process))
	res=str.find("Success")
	if res == -1:
		print ("%s is running"%(process))
	else 
		print ("%s is not running"%(process))


#------------------------------------------------------------------------------
# get - download all files from remote servers
# arg: none
#------------------------------------------------------------------------------
def get():
 	d = os.path.dirname("result/%s"%(EXPERIMENT_TAG))
    if not os.path.exists(d):
        os.makedirs(d)

	CMD_SCP="scp -C -r -i "
	
	for i in range(DATACENTERNUM):
		datacenter = DATACENTER[i]
		os.system(("%s %s %s@%s:%s/dist/results/* result/%s")%(CMD_SCP,CREDENTIAL[i],USERNAME,datacenter['user'],TPCW_OUTPUTDIR,EXPERIMENT_TAG))
		os.system(("%s %s %s@%s:/tmp/coordinator.$i.log result/%s")%(CMD_SCP,CREDENTIAL[i],USERNAME,datacenter['coordinator'],TPCW_OUTPUTDIR,EXPERIMENT_TAG))
		for datawriter in datacenter['datawriter']:
		
		os.system(("%s %s %s@%s:/tmp/storageshim.$i.log result/%s")%(CMD_SCP,CREDENTIAL[i],USERNAME,datacenter['datawriter'],TPCW_OUTPUTDIR,EXPERIMENT_TAG))
		os.system(("%s %s %s@%s:/tmp/storageshim.$i.log result/%s")%(CMD_SCP,CREDENTIAL[i],USERNAME,datacenter['proxy'],TPCW_OUTPUTDIR,EXPERIMENT_TAG))
















#------------------------------------------------------------------------------
# configure
# arg: configure all components of all datacenters
#------------------------------------------------------------------------------
configure(){
	if [ $AUTH_MODE = "-k" ]
	then
		CMD_SCP="scp -C -r -i "
	else
		CMD_SCP="scp -C -r "
	fi
	
	echo "deploying configuration files"
	for((i=0;i<$DATACENTERNUM;i++)) do
		
		$CMD_SCP  ${CREDENTIAL[$i]} $TOPOLOGYXMLFILENAME  $DATABASEXMLFILENAME ${USERNAME}"@"${TXM_PROXY[$i]}":"${TPCW_ROOT} 
		#$CMD_SCP  ${CREDENTIAL[$i]} $TOPOLOGYXMLFILENAME  $DATABASEXMLFILENAME ${USERNAME}"@"${TXM_COORDINATOR[$i]}":"${TPCW_ROOT} 
		$CMD_SCP  ${CREDENTIAL[$i]} $TOPOLOGYXMLFILENAME  $DATABASEXMLFILENAME ${USERNAME}"@"${TXM_STORAGESHIM[$i]}":"${TPCW_ROOT}
	done;

	echo "recompile source code"
	for((i=0;i<$DATACENTERNUM;i++)) do
		#python tpcwtxmud_remote_setup.py -d ${TXM_USER[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" --run "$CMD_INSTALL_DEPENDENCIES"
		#python tpcwtxmud_remote_setup.py -d ${TXM_USER[$i]} -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" --run "${CMD_CONFIGURE[$i]}"
		python tpcwtxmud_remote_setup.py -d ${TXM_PROXY[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" --run "$CMD_INSTALL_DEPENDENCIES"
		python tpcwtxmud_remote_setup.py -d ${TXM_PROXY[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" --run "${CMD_CONFIGURE[$i]}"
		#python tpcwtxmud_remote_setup.py -d ${TXM_COORDINATOR[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" --run "$CMD_INSTALL_DEPENDENCIES"
		python tpcwtxmud_remote_setup.py -d ${TXM_STORAGESHIM[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" --run "$CMD_INSTALL_DEPENDENCIES"
	done;
	
}	
populate_remote_database(){
	python tpcwtxmud_remote_setup.py -d ${TXM_DATABASE[0]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[0]}" --run "$CMD_POPULATE_DATABASE"
	#python tpcwtxmud_remote_setup.py -d ${TXM_DATABASE[1]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[1]}" --run "$CMD_POPULATE_DATABASE" 
}		
deploy_tpcw_website(){
	for((i=0;i<$DATACENTERNUM;i++)) do
		#echo "generating images"
		#python tpcwtxmud_remote_setup.py -d ${TXM_PROXY[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" --run "$CMD_BUILD_TPCW_IMAGES"
		echo "=================================================================="
		echo "deploying website with images"
		echo "=================================================================="
		python tpcwtxmud_remote_setup.py -d ${TXM_PROXY[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" --run "$CMD_INSTALL_TPCW_WEBSITE"
	done;

}
reset_logical_clock(){
	for((i=0;i<$DATABASENUM;i++)) do
		echo "========================================================="
		echo "force database to shutdown"
		python tpcwtxmud_remote_setup.py -d ${TXM_DATABASE[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" -r storage --command "reset" 
		echo "========================================================="
		echo "cleanup database "
		python tpcwtxmud_remote_setup.py -d ${TXM_DATABASE[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" --run "$CMD_DBCLEANUP"
		echo "========================================================="
		echo "restore database "
		python tpcwtxmud_remote_setup.py -d ${TXM_DATABASE[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" --run "$CMD_DBDEPLOY"
	done;
	echo "Databases are clean"
}
reset_nodes(){
	for((i=0;i<$DATABASENUM;i++)) do
		echo "========================================================="
		echo "shutown all components of the datacenter $i "
		echo "shutdown storageshim "
		python tpcwtxmud_remote_setup.py -d ${TXM_STORAGESHIM[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" -r mshim --command "reset" 
		echo "shutdown coordinator "
		python tpcwtxmud_remote_setup.py -d ${TXM_COORDINATOR[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" -r coordinator --command "reset" 
		echo "shutdown database "
		python tpcwtxmud_remote_setup.py -d ${TXM_DATABASE[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" -r storage --command "reset" 
		echo "shutdown proxy"
		python tpcwtxmud_remote_setup.py -d ${TXM_PROXY[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" -r proxy --command "reset" 
		echo "shutdown user"
		python tpcwtxmud_remote_setup.py -d ${TXM_USER[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" -r user --command "reset"
	done;
	echo "========================================================="
	echo "all components are shutdown "
	
}

update_src(){
	for((i=0;i<$DATABASENUM;i++)) do
		echo "========================================================="
		echo "update all components of the datacenter $i "
		python tpcwtxmud_remote_setup.py -d ${TXM_STORAGESHIM[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" -r mshim --run "cd $TPCW_ROOT && svn up"
		python tpcwtxmud_remote_setup.py -d ${TXM_COORDINATOR[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" -r coordinator --run "cd $TPCW_ROOT && svn up"
		python tpcwtxmud_remote_setup.py -d ${TXM_PROXY[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" -r proxy --run "cd $TPCW_ROOT && svn up"
		python tpcwtxmud_remote_setup.py -d ${TXM_USER[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" -r user --run "cd $TPCW_ROOT && svn up"
	done;
}
start_users(){
	echo "Starting all users"
	for((i=0;i<$DATABASENUM;i++)) do
			python tpcwtxmud_remote_setup.py -d ${TXM_USER[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" -r user --run "${CMD_START_EXPERIMENT[$i]}" &
	done;
	echo "All users started at "`date +%d/%m/%y_%H:%M:%S`
}
start_tomcat(){
	for((i=0;i<$DATACENTERNUM;i++)) do
		python tpcwtxmud_remote_setup.py -d ${TXM_PROXY[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" -r proxy --run "cd $TPCW_ROOT && ant tomcat-start" 	
	done;
	sleep 10
}	
start_coordinator(){
	for((i=0;i<$DATACENTERNUM;i++)) do	
		python tpcwtxmud_remote_setup.py -d ${TXM_COORDINATOR[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" -r coordinator --runbg "nohup ${CMD_START_COORDINATOR[$i]}  &" 
	done;
}																		
start_storageshim(){
	for((i=0;i<$DATACENTERNUM;i++)) do	
			python tpcwtxmud_remote_setup.py -d ${TXM_STORAGESHIM[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" -r mshim --runbg "nohup ${CMD_START_STORAGESHIM[$i]} &"    
	done;
				
}

wait_for_storageshim(){
	for((i=0;i<$DATACENTERNUM;i++)) do	
			python tpcwtxmud_remote_setup.py -d ${TXM_STORAGESHIM[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" -r mshim --run "${CMD_WAIT_STORAGESHIM[$i]}"    
	done;
}
wait_users_to_finish(){
	for((i=0;i<$DATACENTERNUM;i++)) do	
			python tpcwtxmud_remote_setup.py -d ${TXM_USER[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" -r mshim --run "${CMD_WAIT_USERS[$i]}"    
	done;
}

start_mysql(){
	for((i=0;i<$DATABASENUM;i++)) do
		python tpcwtxmud_remote_setup.py -d ${TXM_DATABASE[$i]}  -u $USERNAME $AUTH_MODE "${CREDENTIAL[$i]}" -r storage --run "cd $TPCW_ROOT && ant mysql-start" 	
	done;				
}
#-----------------------------------------------------------------------------
#MAIN				

case $1 in 
	"install_all") 			install_all;;						
	"install_proxy") 			install_proxy;;
	"install_database") 			install_database;;
	"install_txmud") 			install_txmud;;
	"get")					get;;
	"configure") 			configure;;
	"database") 			populate_remote_database;;	
	"website") 				deploy_tpcw_website;;
	"start_tomcat") 		start_tomcat;;
	"start_mysql") 			start_mysql;;
	"start_user") 			start_users;;	
	"start_coordinator") 	start_coordinator;;	
	"start_storageshim")	start_storageshim;;
	"wait_for_storageshim") wait_for_storageshim;;
	"wait_for_users_to_finish") wait_users_to_finish;;
	"reset_database") 	reset_logical_clock;;
	"shutdown") 				reset_nodes;;
	"update") 				update_src;;
	"isrunning") 			isrunning $2 $3;;
	*) 						help;;
esac
echo "Experiment finished!"

if __name__=="__main__":
	create_configuration_files()





