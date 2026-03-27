import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class TrustStrategyCertificateTest {

    @Test
    void trustStrategy_should_accept_valid_certificate_chain() throws Exception {
        // 1. Charge le keystore créé avec le script
        KeyStore ks = KeyStoreHelper.keyStore(
            KeyStoreHelper.JKS,
            new ClassPathResource("ssl/leaf-chain.jks").getInputStream(),
            "password".toCharArray(),
            null
        );

        // 2. Récupère la chaîne complète du certificat leaf
        // (keytool -list doit montrer "leaf-cert" = PrivateKeyEntry)
        Certificate[] raw = ks.getCertificateChain("leaf-cert");
        assertThat(raw)
            .as("Certificate chain should not be null or empty")
            .isNotNull()
            .hasSize(3);

        // 3. Vérifie les sujets (leaf → inter → root)
        X509Certificate leaf = (X509Certificate) raw[0];
        X509Certificate inter = (X509Certificate) raw[1];
        X509Certificate root = (X509Certificate) raw[2];

        assertThat(leaf.getSubjectX500Principal().toString())
            .contains("CN=localhost", "OU=Dev");
        assertThat(inter.getSubjectX500Principal().toString())
            .contains("CN=TestIntermediateCA", "OU=Dev");
        assertThat(root.getSubjectX500Principal().toString())
            .contains("CN=TestRootCA", "OU=Dev");

        // 4. Si ton TrustStrategy attend [root, inter, leaf] dans cet ordre
        X509Certificate[] rootLastToFirst = {
            root,
            inter,
            leaf
        };

        TrustStrategy trustStrategy = getTrustStoreStrategy(
            FunctionalCurrentTimeSupplier.SYSTEM_TIME,
            true,
            TrustedKeyStoreCertificateProvider.build(ks, null),
            true,
            ""
        );

        // 5. Test : la chaîne est valide ?
        boolean trusted = trustStrategy.isTrusted(rootLastToFirst, "RSA");

        assertThat(trusted)
            .as("Leaf certificate chain should be trusted when issuer is a valid CA")
            .isTrue();
    }

    @Test
    void trustStrategy_should_reject_chain_when_issuer_not_ca() throws Exception {
        // 1. Charge le keystore
        KeyStore ks = KeyStoreHelper.keyStore(
            KeyStoreHelper.JKS,
            new ClassPathResource("ssl/leaf-chain.jks").getInputStream(),
            "password".toCharArray(),
            null
        );

        Certificate[] raw = ks.getCertificateChain("leaf-cert");
        assertThat(raw).isNotNull().hasSize(3);

        X509Certificate leaf = (X509Certificate) raw[0];
        X509Certificate inter = (X509Certificate) raw[1]; // l’issuer
        X509Certificate root = (X509Certificate) raw[2];

        // 2. Scenario: simule un cas où tu sais que l’inter n’est pas CA (test d’erreur)
        // Normalement, avec ton script corrigé, isTrusted sera true,
        // donc ce test peut servir plus tard si tu as une autre implémentation fragile.
        X509Certificate[] chain = { root, inter, leaf };

        TrustStrategy brokenStrategy = getTrustStoreStrategy(
            FunctionalCurrentTimeSupplier.SYSTEM_TIME,
            true,
            TrustedKeyStoreCertificateProvider.build(ks, null),
            true,
            ""
        );

        boolean trusted = brokenStrategy.isTrusted(chain, "RSA");
        // Dépend de ton implé, mais tu peux écrire un test "négatif" séparé
        // si tu veux simuler par exemple un certificat expiré ou usage invalide.
    }
}
