Parfait, cela signifie que **ta root** et **ton intermédiaire** sont bien marqués comme AC (`CA:true`). Cela règle une grosse partie du problème côté OpenSSL/chaîne. [docs.openssl](https://docs.openssl.org/3.0/man1/openssl-verify/)

Maintenant le bloc `isTrustedAt` doit être **homogène** avec la façon dont tu passes les certificats :  
- soit tu passes **une seule certificat leaf** (1 cert, completé par le truststore),  
- soit tu passes la **chaîne complète leaf → inter → root** (3 certificats).

***

## 1. Ce que CA:true implique dans ton cas

- `root.crt` (AC racine) :  
  - `CA:true` + `isSelfSigned` (normal) → OK.  
- `inter.crt` (AC intermédiaire) :  
  - `CA:true` + **non self‑signed** (signé par la racine) → OK. [docs.openssl](https://docs.openssl.org/3.1/man1/openssl-verification-options/)

Donc `openssl verify` doit accepter la chaîne :

```bash
openssl verify -CAfile root.crt -untrusted inter.crt leaf.crt
# → leaf.crt: OK
```

***

## 2. Ce que tu dois archiver dans Java

Avec ton `isTrustedAt` tel que corrigé précédemment, il faut :

- Que la **racine** soit présente en dernier (`certificates[certificates.length - 1]`) et **auto‑signée** → l’erreur `root certificate is not trusted` ne doit plus apparaître.  
- Que l’**intermédiaire** soit **non auto‑signé** et que son issuer corresponde au cert suivant (`i+1`) → l’erreur `intermediate certificate is self signed` ne doit plus apparaître.

***

## 3. Voici les deux cas pour lesquels je te propose de vérifier

### Cas 1 : Certificat simple (1 cert = leaf)

```java
@Test
void trustStrategy_should_accept_single_leaf_cert() throws Exception {
    KeyStore ks = KeyStoreHelper.keyStore(
        KeyStoreHelper.JKS,
        new ClassPathResource("ssl/leaf-chain.jks").getInputStream(),
        "password".toCharArray(),
        null
    );

    X509Certificate leaf = (X509Certificate) ks.getCertificate("leaf-cert");

    TrustStrategy trustStrategy = getTrustStoreStrategy(
        FunctionalCurrentTimeSupplier.SYSTEM_TIME,
        true,
        TrustedKeyStoreCertificateProvider.build(ks, null),
        true,
        ""
    );

    boolean trusted = trustStrategy.isTrusted(new X509Certificate[]{ leaf }, "RSA");

    assertThat(trusted)
        .as("Single leaf cert should be trusted")
        .isTrue();
}
```

- Lorsque ce cas est exécuté, la méthode `isTrustedAt` reçoit un tableau de **1 certificat**.  
- `isLeaf = true`, `isRoot = true` → le bloc `isRoot` s’exécute et vérifie:
  - `leaf.isSelfSigned()` → doit être **false** (leaf signé par intermédiaire),  
  - `truststore.isTrustedChain(leaf, null)` → retourne `true` si ton truststore connaît la chaîne complète.

Si `leaf` est **auto‑signé** dans ce cas, tu prends `root certificate is not trusted` (ce qui est normal, car tu n’as pas de vraie AC dans le tableau).

→ Assure‑toi que pour ce cas, le truststore **complète bien** la chaîne (`leaf` → `inter` → `root`).

***

### Cas 2 : Chaîne complète `leaf → inter → root`

```java
@Test
void trustStrategy_should_accept_full_chain() throws Exception {
    KeyStore ks = KeyStoreHelper.keyStore(
        KeyStoreHelper.JKS,
        new ClassPathResource("ssl/leaf-chain.jks").getInputStream(),
        "password".toCharArray(),
        null
    );

    Certificate[] raw = ks.getCertificateChain("leaf-cert");
    X509Certificate[] chain = Arrays.stream(raw)
        .map(c -> (X509Certificate) c)
        .toArray(X509Certificate[]::new);

    assertThat(chain).hasSize(3)
        .as("Full chain must have 3 certificates (leaf, inter, root)");

    TrustStrategy trustStrategy = getTrustStoreStrategy(
        FunctionalCurrentTimeSupplier.SYSTEM_TIME,
        true,
        TrustedKeyStoreCertificateProvider.build(ks, null),
        true,
        ""
    );

    boolean trusted = trustStrategy.isTrusted(chain, "RSA");

    assertThat(trusted)
        .as("Full chain leaf→inter→root should be trusted")
        .isTrue();
}
```

- `chain[0] = leaf` → `isLeaf = true`, `isRoot = false`  
- `chain [docs.openssl](https://docs.openssl.org/3.0/man1/openssl-verify/) = inter` → `isLeaf = false`, `isRoot = false`  
- `chain [docs.openssl](https://docs.openssl.org/3.1/man1/openssl-verification-options/) = root` → `isLeaf = false`, `isRoot = true`  

Dans ce cas :
- `leaf` ne doit pas être auto‑signé → OK car signé par `inter`.  
- `inter` ne doit pas être auto‑signé → OK car signé par `root`.  
- `root` doit être auto‑signé → OK.

***

## 4. Point critique à vérifier dans `isTrustedAt`

Assure‑toi dans `isTrustedAt` que :

```java
if (!c.isSelfSigned() && !truststore.isTrustedChain(c, null)) {
    log.warn("root certificate is not trusted");
    return false;
}
```

ne s’exécute **que pour la racine** (i.e. `isRoot = true`).  
Si tu passes `leaf` en `i = 0` dans un tableau de taille 1, tu peux recevoir ce message même si c’est un **leaf**, pas une racine.

Pour éviter ça, tu peux ajuster la logique :

```java
if (isRoot) {
    if (!c.isSelfSigned() && !truststore.isTrustedChain(c, null)) {
        log.warn("root certificate is not trusted");
        return false;
    }
} else if (c.isSelfSigned()) {
    // Un certificat non racine ne doit pas être auto-signé
    log.warn("intermediate certificate is self signed ! dn={}", dn);
    return false;
}
```

***

## 5. Recommandation finale

- Vérifie que `openssl verify` passe sur `leaf.crt` (comme vous l’avez déjà fait).  
- Vérifie que le truststore connaît bien `TestRootCA` (ou `TestIntermediateCA`) pour `isTrustedChain(c, null)`.

Avec ces deux points, **les deux cas** (cert simple et chaîne complète) devraient passer sans problème.

***

Si tu veux que je t’aide à finaliser complètement le test, colle la sortie de `openssl verify` et le code de `isTrusted` et `isCertificateChecked` et je t’ajustes tout pour que les deux cas soient solides.
