@Service
@RequiredArgsConstructor
public class RefActionCacheService {

    private final RefActionRepository refActionRepository;

    @Cacheable(value = "refActionCache")
    public RefActionRac getRefFromCodeAndOrigin(String code, String origin) {

        List<RefActionRac> actionsList =
                refActionRepository.findByCodeAndOrigin(code, origin);

        if (actionsList.isEmpty()) {
            return null;
        }

        return actionsList.getFirst();
    }
}
------------
@Service
@RequiredArgsConstructor
@Slf4j
public class ActionReferenceService {

    private final RefActionCacheService cacheService;

    public String getActionFromCodeAndOrigin(String code, String origin) {

        return Optional.ofNullable(
                cacheService.getRefFromCodeAndOrigin(code, origin)
        ).map(RefActionRac::getPiuValue)
         .orElse(null);
    }
}
``
