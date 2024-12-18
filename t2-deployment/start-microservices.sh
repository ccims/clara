#!/bin/bash

# Documentation: https://t2-documentation.readthedocs.io/en/latest/microservices/deploy.html

MY_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

# If an argument is given, use it as the namespace
if [ $# -gt 0 ]; then
    NAMESPACE=$1
else
    NAMESPACE="default"
fi

if [ $NAMESPACE != "default" ]; then
  kubectl create namespace $NAMESPACE --dry-run=client -o yaml | kubectl apply -f -
fi

helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update
helm install mongo-cart -f $MY_DIR/mongodb/mongo-values.yaml bitnami/mongodb -n $NAMESPACE
helm install mongo-order -f $MY_DIR/mongodb/mongo-values.yaml bitnami/mongodb -n $NAMESPACE
helm install kafka bitnami/kafka --version 18.5.0 --set replicaCount=1 -n $NAMESPACE

kubectl apply -k $MY_DIR/t2-microservices/ -n $NAMESPACE
