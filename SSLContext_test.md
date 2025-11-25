@Test
    void createRequestFactory_withSSLContext_shouldNotThrowException() {
        // ARRANGE
        // Nous utilisons un mock de SSLContext pour éviter la complexité de l'initialisation réelle.
        SSLContext mockSslContext = mock(SSLContext.class);

        // ACT
        // On vérifie que la construction de la Factory avec SSL est réussie
        ClientHttpRequestFactory factory = RestClientHelper.createRequestFactory(
                mockSslContext,
                CONNECTION_TIMEOUT_MS,
                RESPONSE_TIMEOUT_MS
        );

        // ASSERT
        assertNotNull(factory, "La ClientHttpRequestFactory ne devrait pas être null lors de la configuration SSL.");
    }
