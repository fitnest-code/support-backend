package az.fitnest.support.service.impl;

import az.fitnest.support.model.entity.Translation;
import az.fitnest.support.repository.TranslationRepository;
import az.fitnest.support.service.TranslationService;
import org.springframework.stereotype.Service;

@Service
public class TranslationServiceImpl implements TranslationService {
    private final TranslationRepository translationRepository;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TranslationServiceImpl.class);

    public TranslationServiceImpl(TranslationRepository translationRepository) {
        this.translationRepository = translationRepository;
    }

    @Override
    public String getTranslatedValue(String entityType, String entityId, String fieldName, String languageCode) {
        if (entityType == null || entityId == null || fieldName == null) {
            return null;
        }
        if (languageCode == null || languageCode.equalsIgnoreCase("AZ")) {
            return null;
        }

        if (entityType != null) {
            String normType = entityType.toUpperCase();
            if (normType.equals("SUPPORT_TICKET_STATUS") || normType.equals("SUPPORTTICKETSTATUS") || normType.equals("TICKET_STATUS") || normType.equals("TICKETSTATUS")) {
                String status = entityId.toUpperCase();
                if (languageCode.equalsIgnoreCase("EN")) {
                    switch (status) {
                        case "OPEN": return "Open";
                        case "RESOLVED": return "Resolved";
                        case "CLOSED": return "Closed";
                        default: return entityId;
                    }
                } else if (languageCode.equalsIgnoreCase("RU")) {
                    switch (status) {
                        case "OPEN": return "Открыт";
                        case "RESOLVED": return "Решено";
                        case "CLOSED": return "Закрыт";
                        default: return entityId;
                    }
                }
                return entityId;
            }
        }

        String existingValue = translationRepository.findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(
                entityType.toUpperCase(),
                entityId,
                languageCode.toUpperCase(),
                fieldName
        )
        .map(Translation::getFieldValue)
        .orElse(null);

        if (existingValue != null) {
            return existingValue;
        }

        // Manual translations only: AZ lives on its own entity table, EN/RU live in
        // the translations table (admin-provided). No machine translation.
        return null;
    }

    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void saveOrUpdateTranslation(String entityType, String entityId, String languageCode, String fieldName, String fieldValue) {
        String normalizedEntityType = entityType.toUpperCase();
        String normalizedLanguageCode = languageCode.toUpperCase();

        log.info("Database Save: entityType={}, entityId={}, languageCode={}, fieldName={}, fieldValue='{}'", 
            normalizedEntityType, entityId, normalizedLanguageCode, fieldName, fieldValue);

        Translation existing = translationRepository.findByEntityTypeAndEntityIdAndLanguageCodeAndFieldName(
                normalizedEntityType, entityId, normalizedLanguageCode, fieldName
        ).orElse(null);

        if (existing != null) {
            log.info("Updating existing translation record ID={}", existing.getId());
            existing.setFieldValue(fieldValue);
            translationRepository.save(existing);
        } else {
            log.info("Creating new translation record");
            Translation translation = Translation.builder()
                    .entityType(normalizedEntityType)
                    .entityId(entityId)
                    .languageCode(normalizedLanguageCode)
                    .fieldName(fieldName)
                    .fieldValue(fieldValue)
                    .build();
            translationRepository.save(translation);
        }
    }
}
