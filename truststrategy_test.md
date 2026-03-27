## Étapes concrètes (solution 1 – chaîne de test complète)

Idée :  
- Root CA self‑signed (AC racine).  
- Intermediate CA signée par la root.  
- Leaf (ton « certificat serveur ») signé par l’intermediate.  
- Tu importes la **chaîne complète** dans un keystore JKS/PKCS12, ou tu mets le root dans un truststore et la chaîne leaf+intermédiaire dans un keystore séparé, suivant ce que consomme ton `TrustedKeyStoreCertificateProvider`. [docs.oracle](https://docs.oracle.com/en/java/javase/17/docs/specs/man/keytool.html)

En pratique tu peux faire ça avec `openssl` + `keytool` (une fois, puis versionner les fichiers dans `src/test/resources`) :

### 1. Générer root, intermédiaire et leaf avec openssl

Exemple simplifié (script shell) inspiré des guides classiques : [docs.opensearch](https://docs.opensearch.org/latest/security/configuration/generate-certificates/)

```bash
# 1. Root CA
openssl genrsa -out root.key 2048
openssl req -x509 -new -nodes -key root.key \
  -sha256 -days 3650 \
  -subj "/C=FR/ST=IDF/L=Paris/O=TestRoot/OU=Dev/CN=Test Root CA" \
  -out root.crt

# 2. Intermediate CA
openssl genrsa -out inter.key 2048
openssl req -new -key inter.key \
  -subj "/C=FR/ST=IDF/L=Paris/O=TestInter/OU=Dev/CN=Test Intermediate CA" \
  -out inter.csr

openssl x509 -req -in inter.csr -CA root.crt -CAkey root.key -CAcreateserial \
  -out inter.crt -days 1825 -sha256

# 3. Leaf (certificat serveur)
openssl genrsa -out leaf.key 2048
openssl req -new -key leaf.key \
  -subj "/C=FR/ST=IDF/L=Paris/O=TestLeaf/OU=Dev/CN=localhost" \
  -out leaf.csr

openssl x509 -req -in leaf.csr -CA inter.crt -CAkey inter.key -CAcreateserial \
  -out leaf.crt -days 825 -sha256
```

Maintenant tu as une vraie **chaîne** : `leaf.crt` → `inter.crt` → `root.crt`. [stackoverflow](https://stackoverflow.com/questions/10175812/how-can-i-generate-a-self-signed-ssl-certificate-using-openssl)

### 2. Construire un PKCS12/JKS contenant la chaîne

On met la clé privée du leaf + toute la chaîne dans un PKCS12, puis on importe en JKS si tu veux absolument JKS. [johnghawi](http://www.johnghawi.com/2015/12/openssl-certificates-to-java-keystore.html)

```bash
# Chaîne complète pour le leaf
cat leaf.crt inter.crt root.crt > chain.pem

# PKCS12 avec clé privée leaf + chaîne
openssl pkcs12 -export \
  -inkey leaf.key \
  -in leaf.crt \
  -certfile inter.crt \
  -name "leaf-cert" \
  -out leaf-chain.p12 \
  -passout pass:password
```

Puis, si tu veux un JKS :

```bash
keytool -importkeystore \
  -srckeystore leaf-chain.p12 -srcstoretype PKCS12 -srcstorepass password \
  -destkeystore leaf-chain.jks -deststoretype JKS -deststorepass password
```

Tu mets `leaf-chain.jks` dans `src/test/resources/ssl/`. [johnghawi](http://www.johnghawi.com/2015/12/openssl-certificates-to-java-keystore.html)

***

## Utilisation dans ton test

Si ton `TrustedKeyStoreCertificateProvider.build(ks, null)` s’attend à un keystore qui contient la chaîne, ton test ressemble à ceci :

```java
KeyStore ks = KeyStoreHelper.keyStore(
    KeyStoreHelper.JKS,
    new ClassPathResource("ssl/leaf-chain.jks").getInputStream(),
    "password".toCharArray(),
    null
);

TrustStrategy trustStrategy = getTrustStoreStrategy(
    FunctionalCurrentTimeSupplier.SYSTEM_TIME,
    true,
    TrustedKeyStoreCertificateProvider.build(ks, null),
    true,
    ""
);

// Récupère la chaîne complète depuis le keystore
Certificate[] chain = ks.getCertificateChain("leaf-cert");
X509Certificate[] x509Chain = Arrays.stream(chain)
    .map(c -> (X509Certificate) c)
    .toArray(X509Certificate[]::new);

boolean trusted = trustStrategy.isTrusted(x509Chain, "RSA");
assertThat(trusted).isTrue();
```

Dans ce scénario, le root du keystore joue le rôle d’AC racine de confiance dans ton test (même si ce n’est pas un root « public »), et la leaf n’est pas self‑signed : elle est signée par l’intermédiaire qui lui‑même est signé par la root. [stackoverflow](https://stackoverflow.com/questions/29950950/validating-certificate-chain-in-java-from-truststore)

***
