Vous avez tout à fait raison ! J'ai fait une erreur
Le problème est que l'annotation @Cacheable sur la méthode hostname() qui retourne Optional<String> va quand même essayer de mettre en cache l'Optional directement, pas le CacheableOptional.
Le problème est qu'une fois que Spring a déballé le Optional<String> et a mis le String simple dans le cache, si une autre partie du code ou une désérialisation incorrecte (souvent le cas avec Ehcache/JCache) se produit, le String est récupéré à la place du Optional<String> attendu. Le mécanisme de Spring n'arrive pas à re-wrapper correctement le String dans un Optional au moment du retour.

// Méthode interne AVEC @Cacheable qui retourne CacheableOptional
@Cacheable("dnsHostName")
protected CacheableOptional<String> hostnameInternal(final String address) {
    Optional<String> result = IpHelper.getHostName(executor, dnsTimeout, address);
    return CacheableOptional.of(result);
}

@Cacheable(value = "dnsHostName", 
           key = "#address",
           unless = "#result == null || !#result.isPresent()")
public Optional<String> hostname(final String address) {
    return IpHelper.getHostName(executor, dnsTimeout, address);
}

@Cacheable(value = "dnsHostName", 
           key = "#address",
           unless = "#result == null || !#result.isPresent()")
public Optional<String> hostname(final String address) {
    return IpHelper.getHostName(executor, dnsTimeout, address);
}
