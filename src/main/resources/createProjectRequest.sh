#!/bin/sh

# 1 token
# 2 project name
# 3 endpoint url

curl -k -X POST -H "Authorization: Bearer $1" -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{ "apiVersion": "v1", "kind": "ProjectRequest", "metadata": { "name": "'$2'" } }' $3/oapi/v1/projectrequests
