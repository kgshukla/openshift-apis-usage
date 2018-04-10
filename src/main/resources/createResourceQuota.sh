# 1 token
# 2 quota name
# 3 namespace name
# 4 cpu limit
# 5 memory limit
# 6 endpoint url

curl -k -X POST -H "Authorization: Bearer $1" -H 'Accept: application/json' -H 'Content-Type: application/json' -d '{ "apiVersion": "v1", "kind": "ResourceQuota", "metadata": { "name": "'$2'", "namespace": "'$3'" }, "spec": { "hard": { "cpu": "'$4'", "memory": "'$5'" } } }' $6/api/v1/namespaces/$3/resourcequotas
