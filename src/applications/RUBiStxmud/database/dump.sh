#!/bin/bash

if [ "x$1" ==  x'' ]; then
	echo "please provide a file name"
else
	mysqldump -usa -p101010 --databases rubis > $1
fi

