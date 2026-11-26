package it.whodas.questionnaire.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import it.whodas.questionnaire.domain.QuestionnaireItem;

@Repository
public interface QuestionnaireItemRepository extends JpaRepository<QuestionnaireItem, Long>, JpaSpecificationExecutor<QuestionnaireItem> {
    QuestionnaireItem findByItemCode(String itemCode);
    java.util.List<QuestionnaireItem> findByDomain(String domain);
    java.util.List<QuestionnaireItem> findByDomainIn(java.util.List<String> domains);
    java.util.List<QuestionnaireItem> findByItemGroup(String itemGroup);
    long countByDomain(String domain);
}