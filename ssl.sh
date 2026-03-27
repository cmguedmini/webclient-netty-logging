`error 79 at 1 depth lookup: invalid CA certificate` indique que **l’intermédiaire (`inter.crt`) n’est pas reconnu comme une CA valide** par OpenSSL, même si tu as signé le leaf avec lui. [stackoverflow](https://stackoverflow.com/questions/53881437/invalid-ca-certificate-with-self-signed-certificate-chain)

À la profondeur `1` (l’intermédiaire), OpenSSL considère que ce certificat **n’est pas un CA autorisé à signer** → c’est la même raison que ton `issuer can't sign key` en Java.

***

## 1. Ce que signale `verify` ici

- `leaf.crt` = certificat de leaf, `depth 0`.  
- `inter.crt` = certificat de l’AC qui signe leaf, `depth 1`.  
- `error 79 at 1 depth lookup: invalid CA certificate` →  
  soit `Basic Constraints` n’est pas `CA:true`, soit `Key Usage` ne permet pas `keyCertSign`, soit l’extension est absente/malformée. [github](https://github.com/openssl/openssl/issues/16664)

***

## 2. Vérifie explicitement `inter.crt`

Lance dans Git Bash :

```bash
openssl x509 -in inter.crt -text -noout | grep -i "CA\|Basic Constraints\|Key Usage"
```

Tu dois voir au minimum :

```text
X509v3 Basic Constraints:
    CA:TRUE
X509v3 Key Usage:
    Digital Signature, Key Cert Sign, CRL Sign
```

Si tu vois :
- `CA:FALSE`, ou  
- `Basic Constraints` absent, ou  
- `Key Usage` sans `Certificate Sign` / `keyCertSign`  

→ ton intermédiaire n’est pas une CA valide, d’où l’erreur `invalid CA certificate`. [docs.openssl](https://docs.openssl.org/3.1/man1/openssl-verification-options/)

***

## 3. Script de certificat intermédiaire qui doit marcher

Voici un script **minimal et propre** pour générer une intermédiaire correctement marquée comme CA :

### 1) Fichier `openssl_inter.conf`

Crée le fichier dans le même répertoire :

```ini
[ v3_intermediate_ca ]
basicConstraints = critical, CA:true, pathlen:1
keyUsage = critical, keyCertSign, cRLSign
```

### 2) Génération de l’intermédiaire avec ces extensions

```bash
# ROOT (pas de changement important pour ce test)
openssl genrsa -out root.key 2048
openssl req -x509 -new -nodes -key root.key -days 3650 \
  -subj "/C=FR/ST=IDF/L=Paris/O=TestRoot/OU=Dev/CN=TestRootCA" \
  -out root.crt

# INTERMEDIATE
openssl genrsa -out inter.key 2048
openssl req -new -key inter.key \
  -subj "/C=FR/ST=IDF/L=Paris/O=TestInter/OU=Dev/CN=TestIntermediateCA" \
  -out inter.csr

openssl x509 -req -in inter.csr -CA root.crt -CAkey root.key -CAcreateserial \
  -extfile openssl_inter.conf -extensions v3_intermediate_ca \
  -out inter.crt -days 1825 -sha256

# LEAF
openssl genrsa -out leaf.key 2048
openssl req -new -key leaf.key \
  -subj "/C=FR/ST=IDF/L=Paris/O=TestLeaf/OU=Dev/CN=localhost" \
  -out leaf.csr

openssl x509 -req -in leaf.csr -CA inter.crt -CAkey inter.key -CAcreateserial \
  -out leaf.crt -days 825 -sha256

# Vérifie que tout est OK
openssl verify -CAfile root.crt -untrusted inter.crt leaf.crt
```

À la fin, `openssl verify` doit afficher `leaf.crt: OK` (ou juste `OK`). [docs.openssl](https://docs.openssl.org/3.0/man1/openssl-verify/)

***

## 4. Vérifie ensuite dans Java

Une fois que `openssl verify` accepte `leaf.crt`, tu peux être sûr que la chaîne est correcte côté OpenSSL, et que le problème éventuel vient de ton `TrustStrategy` ou du truststore.

Dans ton test Java, garde :

- le **root** comme AC de confiance,  
- la **chaîne leaf + intermédiaire** passée à `isTrusted` ou à un client HTTP,  
- et assure‑toi que ton `TrustStrategy` n’attend pas autre chose que ce que tu as généré (bad‑order, alias manquant, etc.).

***

Si tu veux que je t’aide à fermer la boucle Java/OpenSSL, colle :

1) La sortie exacte de `openssl x509 -in inter.crt -text -noout` (ou le bloc `Basic Constraints` + `Key Usage`).  
2) La ligne de `getTrustStoreStrategy(...)` et de l’appel `trustStrategy.isTrusted(...)`.

et je te propose un code de test unitaire **garanti valide** et prêt à coller.
