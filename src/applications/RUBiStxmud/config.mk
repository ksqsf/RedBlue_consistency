##############################
#    Environment variables   #
##############################
JAVA_HOME=/usr/lib/jvm/java-6-sun
JAVA  = $(JAVA_HOME)/bin/java
JAVAC = $(JAVA_HOME)/bin/javac
#JAVAC = /usr/bin/jikes
JAVACOPTS =
# +E -deprecation
JAVACC = $(JAVAC) $(JAVACOPTS)
RMIC = $(JAVA_HOME)/bin/rmic
RMIREGISTRY= $(JAVA_HOME)/bin/rmiregistry
CLASSPATH = .:$(JAVA_HOME)/jre/lib/rt.jar:/var/tmp/dcfp/txmud/tomcat6/lib/servlet-api.jar:/var/tmp/dcfp/txmud/tomcat6/lib/mysql-connector-java-5.1.18-bin.jar:$(PWD)
JAVADOC = $(JAVA_HOME)/bin/javadoc
JAR = $(JAVA_HOME)/bin/jar

GENIC = ${JONAS_ROOT}/bin/unix/GenIC

MAKE = make
CP = /bin/cp
RM = /bin/rm
MKDIR = /bin/mkdir


# EJB server: supported values are jonas or jboss
EJB_SERVER = jonas

# DB server: supported values are MySQL or PostgreSQL
DB_SERVER = MySQL

%.class: %.java
	${JAVACC} -classpath ${CLASSPATH} $<

