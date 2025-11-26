public final class QualityProfileRulesRequest {
    private String qualityProfileId;
    private QualityProfileBookmark bookmark;
}

public final class QualityProfileBookmark {
    private @Min(0L) long startIndex;
    private @Min(1L) int pageSize;
    private @Valid @NotEmpty List<SortPropertyDto> sortProperties;
}

public final class SortPropertyDto {
    private String property;
    private SortDirection direction;
}

// Méthode utilitaire dans votre service
public <P extends Enum<P>> RsaBookmark<P> toRsaBookmark(
        QualityProfileBookmark bookmark, 
        Class<P> enumClass) {
    
    List<SortProperty<P>> sortProps = bookmark.getSortProperties().stream()
        .map(sp -> new SortProperty<>(
            Enum.valueOf(enumClass, sp.getProperty()),
            sp.getDirection()
        ))
        .toList();
        
    return new RsaBookmark<>(
        bookmark.getStartIndex(),
        bookmark.getPageSize(),
        sortProps
    );
}
