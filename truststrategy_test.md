## Étapes concrètes (solution 1 – chaîne de test complète)

Idée :  
- Root CA self‑signed (AC racine).  
- Intermediate CA signée par la root.  
- Leaf (ton « certificat serveur ») signé par l’intermediate.  
- Tu importes la **chaîne complète** dans un keystore JKS/PKCS12, ou tu mets le root dans un truststore et la chaîne leaf+intermédiaire dans un keystore séparé, suivant ce que consomme ton `TrustedKeyStoreCertificateProvider`. [docs.oracle](https://docs.oracle.com/en/java/javase/17/docs/specs/man/keytool.html)

En pratique tu peux faire ça avec `openssl` + `keytool` (une fois, puis versionner les fichiers dans `src/test/resources`) :

Voici un script complet prêt à coller dans **Git Bash** (Windows) pour créer une chaîne réelle :  
`root CA` → `intermediate CA` → `leaf cert` (CN=localhost), avec `MSYS_NO_PATHCONV=1` pour éviter les warnings et problèmes de parsing.

***

## Script complet (Git Bash)

Tu peux tout copier dans un fichier `.sh` ou dans le terminal, dans un répertoire propre (ex. `certs/`).

```bash
#!/bin/bash

# Pour éviter les problèmes de parsing de /C=... sous Git Bash
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
  -out inter.crt -days 1825 -sha256


# 3. LEAF CERT (server)
echo "3. Génération leaf cert (localhost)"
openssl genrsa -out leaf.key 2048

openssl req -new -key leaf.key \
  -subj "/C=FR/ST=IDF/L=Paris/O=TestLeaf/OU=Dev/CN=localhost" \
  -out leaf.csr

# Chaîne complète leaf → inter → root
cat leaf.crt inter.crt root.crt > chain.pem

openssl x509 -req -in leaf.csr \
  -CA inter.crt -CAkey inter.key -CAcreateserial \
  -out leaf.crt -days 825 -sha256

cat leaf.crt inter.crt root.crt > chain.pem


# 4. Création PKCS12 (clé + cert + chaîne inter + root)
echo "4. Export PKCS12 leaf + chain"
openssl pkcs12 -export \
  -inkey leaf.key \
  -in leaf.crt \
  -certfile inter.crt \
  -name leaf-cert \
  -out leaf-chain.p12 \
  -passout pass:password


# 5. Conversion PKCS12 -> JKS (si tu veux JKS pour ton test)
echo "5. Conversion PKCS12 -> JKS"
keytool -importkeystore \
  -srckeystore leaf-chain.p12 \
  -srcstoretype PKCS12 \
  -srcstorepass password \
  -destkeystore leaf-chain.jks \
  -deststoretype JKS \
  -deststorepass password
```

***

## Utilisation dans ton test unitaire Java

Mets `leaf-chain.jks` dans `src/test/resources/ssl/leaf-chain.jks`, puis dans ton test :

```java
KeyStore ks = KeyStoreHelper.keyStore(
    KeyStoreHelper.JKS,
    new ClassPathResource("ssl/leaf-chain.jks").getInputStream(),
    "password".toCharArray(),
    null
);

X509Certificate leafCert = (X509Certificate) ks.getCertificate("leaf-cert");
X509Certificate[] chain = ks.getCertificateChain("leaf-cert")
    .stream()
    .map(c -> (X509Certificate) c)
    .toArray(X509Certificate[]::new);

TrustStrategy trustStrategy = getTrustStoreStrategy(
    FunctionalCurrentTimeSupplier.SYSTEM_TIME,
    true,
    TrustedKeyStoreCertificateProvider.build(ks, null),
    true,
    ""
);

boolean trusted = trustStrategy.isTrusted(chain, "RSA");
assertThat(trusted).isTrue();
```

Avec ce script, ton leaf cert est bien signé par l’intermédiaire, qui lui‑même est signé par le root, et ta chaîne est « non self‑signed » au niveau du leaf, tout en restant gérée par ton propre root CA. [docs.opensearch](https://docs.opensearch.org/latest/security/configuration/generate-certificates/)
X509Certificate root = (X509Certificate) ks.getCertificate("root-cert");   // ou autre alias
X509Certificate inter = (X509Certificate) ks.getCertificate("inter-cert"); // si alias distinct
X509Certificate leaf = (X509Certificate) ks.getCertificate("leaf-cert");

X509Certificate[] chain = {
    root,            // certificates[0] → root
    inter,           // certificates[1] → inter
    leaf             // certificates[2] → leaf
};

boolean trusted = trustStrategy.isTrusted(chain, "RSA");
assertThat(trusted).isTrue();

Si tu veux, je peux aussi te proposer une version minifiée avec uniquement `C=FR,CN=localhost` si tu veux une chose ultra simple.  
