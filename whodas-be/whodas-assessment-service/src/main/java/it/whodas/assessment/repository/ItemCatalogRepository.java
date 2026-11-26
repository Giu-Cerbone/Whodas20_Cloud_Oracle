package it.whodas.assessment.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import it.whodas.assessment.domain.ItemCatalogEntry;

@Repository
public interface ItemCatalogRepository extends CrudRepository<ItemCatalogEntry, Long> {
    Optional<ItemCatalogEntry> findByItemCode(String itemCode);
    List<ItemCatalogEntry> findByDomainIn(List<String> domains);
}
