#!/bin/bash

MY_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

if [ $# -gt 0 ]; then
    NAMESPACE=$1
else
    NAMESPACE="default"
fi

kubectl delete -k $MY_DIR/t2-microservices/base/ -n $NAMESPACE

helm uninstall mongo-cart -n $NAMESPACE
helm uninstall mongo-order -n $NAMESPACE
helm uninstall kafka -n $NAMESPACE

if [ $NAMESPACE != "default" ]; then
  kubectl delete namespace $NAMESPACE
fi
