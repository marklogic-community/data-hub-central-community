#!/bin/bash

curl -X POST -d "" http://localhost:8001/admin/v1/init
sleep 10
curl -i -X POST  \
	--data "admin-username=admin&admin-password=admin&wallet-password=admin&realm=public" \
	"http://localhost:8001/admin/v1/instance-admin"
sleep 10

