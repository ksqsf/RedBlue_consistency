#!/bin/bash

make clean && ant mksrc && make -j12 && sudo cp -a edu/rice/rubis/servlets/*.class /var/lib/tomcat8/webapps/rubis_servlets/WEB-INF/classes/edu/rice/rubis/servlets/ && sudo systemctl restart tomcat8
