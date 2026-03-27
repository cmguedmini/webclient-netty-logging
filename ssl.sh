#!/bin/bash

export MSYS_NO_PATHCONV=1


# 1. ROOT CA
echo "1. Génération root CA"
openssl genrsa -out root.key 2048
openssl req -x509 -new -nodes -key root.key \
  -sha256 -days 3650 \
  -subj "/C=FR/ST=IDF/L=Paris/O=TestRoot/OU=Dev/CN=TestRootCA" \
  -out root.crt


# 2. INTERMEDIATE CA
echo "2. Génération intermediate CA"
openssl genrsa -out inter.key 2048
openssl req -new -key inter.key \
  -subj "/C=FR/ST=IDF/L=Paris/O=TestInter/OU=Dev/CN=TestIntermediateCA" \
  -out inter.csr

openssl x509 -req -in inter.csr \
  -CA root.crt -CAkey root.key -CAcreateserial \
  -extfile openssl_inter.conf -extensions v3_req \
  -out inter.crt -days 1825 -sha256


# 3. LEAF CERT (server)
echo "3. Génération leaf cert (localhost)"
openssl genrsa -out leaf.key 2048
openssl req -new -key leaf.key \
  -subj "/C=FR/ST=IDF/L=Paris/O=TestLeaf/OU=Dev/CN=localhost" \
  -out leaf.csr

openssl x509 -req -in leaf.csr \
  -CA inter.crt -CAkey inter.key -CAcreateserial \
  -out leaf.crt -days 825 -sha256


# 4. Création PKCS12 avec leaf + inter + root
echo "4. Export PKCS12 leaf + inter + root"
cat leaf.crt inter.crt root.crt > chain.pem

openssl pkcs12 -export \
  -inkey leaf.key \
  -in leaf.crt \
  -certfile chain.pem \
  -name leaf-cert \
  -out leaf-chain.p12 \
  -passout pass:password


# 5. Conversion PKCS12 -> JKS
echo "5. Conversion PKCS12 -> JKS"
keytool -importkeystore \
  -srckeystore leaf-chain.p12 \
  -srcstoretype PKCS12 \
  -srcstorepass password \
  -destkeystore leaf-chain.jks \
  -deststoretype JKS \
  -deststorepass password
