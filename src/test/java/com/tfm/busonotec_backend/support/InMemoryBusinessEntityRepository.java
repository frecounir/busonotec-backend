package com.tfm.busonotec_backend.support;

import com.tfm.busonotec_backend.domain.BusinessEntity;
import com.tfm.busonotec_backend.repository.BusinessEntityRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryBusinessEntityRepository extends BusinessEntityRepository {
  private final Map<UUID, BusinessEntity> entitiesById = new LinkedHashMap<>();
  private final Map<String, BusinessEntity> entitiesByName = new LinkedHashMap<>();
  private final List<BusinessEntity> savedEntities = new ArrayList<>();

  public InMemoryBusinessEntityRepository() {
    super(null);
  }

  public void add(BusinessEntity entity) {
    entitiesById.put(entity.getId(), entity);
    entitiesByName.put(entity.getName(), entity);
  }

  public List<BusinessEntity> savedEntities() {
    return List.copyOf(savedEntities);
  }

  public boolean hasNoSavedEntities() {
    return savedEntities.isEmpty();
  }

  @Override
  public void save(BusinessEntity entity) {
    savedEntities.add(entity);
    add(entity);
  }

  @Override
  public List<BusinessEntity> findAll() {
    return entitiesById.values().stream()
        .sorted(Comparator.comparing(BusinessEntity::getName))
        .toList();
  }

  @Override
  public Optional<BusinessEntity> findById(UUID id) {
    return Optional.ofNullable(entitiesById.get(id));
  }

  @Override
  public Optional<BusinessEntity> findByName(String name) {
    return Optional.ofNullable(entitiesByName.get(name));
  }

  @Override
  public boolean existsByName(String name) {
    return entitiesByName.containsKey(name);
  }
}
