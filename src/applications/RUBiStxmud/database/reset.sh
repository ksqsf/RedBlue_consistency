#!/bin/bash

if [ "x$1" == x'' ]; then
	echo "please provide a file name"
else

mysql -usa -p101010 <<EOF
drop database rubis;
create database rubis;
connect rubis;
source $1
EOF

fi
