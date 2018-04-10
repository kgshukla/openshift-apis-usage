
# 1 token
# 2 deployment status
# 3 deployment remarks
# 4 cart id
# 5 projectname
# 6 quotaname
# 7 endpoint url

curl -k -X POST -H "Authorization: Bearer $1" -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{ "DEPLOYMENT_STATUS": "'$2'", "DEPLOYMENT_REMARKS": "$3", "CART_ID": "'$4'", "DEPLOYMENT_DATA": {"PROJECT_NAME": "'$5'", "QUOTA_NAME": "'$6'" } }' $7
