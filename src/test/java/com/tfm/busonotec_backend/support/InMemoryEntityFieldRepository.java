package com.tfm.busonotec_backend.support;

import com.tfm.busonotec_backend.domain.EntityField;
import com.tfm.busonotec_backend.repository.EntityFieldRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class InMemoryEntityFieldRepository extends EntityFieldRepository {
  private final List<EntityField> fields = new ArrayList<>();
  private final List<EntityField> savedFields = new ArrayList<>();
  private final Set<String> existingNamesByEntity = new HashSet<>();

  public InMemoryEntityFieldRepository() {
    super(null);
  }

  public void add(EntityField field) {
    fields.add(field);
    existingNamesByEntity.add(key(field.getBusinessEntityId(), field.getName()));
  }

  public void markExisting(UUID businessEntityId, String name) {
    existingNamesByEntity.add(key(businessEntityId, name));
  }

  public List<EntityField> savedFields() {
    return List.copyOf(savedFields);
  }

  public boolean hasNoSavedFields() {
    return savedFields.isEmpty();
  }

  @Override
  public void save(EntityField field) {
    savedFields.add(field);
    add(field);
  }

  @Override
  public List<EntityField> findByBusinessEntityId(UUID businessEntityId) {
    return fields.stream()
        .filter(field -> businessEntityId.equals(field.getBusinessEntityId()))
        .sorted(Comparator.comparing(EntityField::getName))
        .toList();
  }

  @Override
  public Optional<EntityField> findById(UUID id) {
    return fields.stream()
        .filter(field -> id.equals(field.getId()))
        .findFirst();
  }

  @Override
  public boolean existsByNameForEntity(UUID businessEntityId, String name) {
    return existingNamesByEntity.contains(key(businessEntityId, name));
  }

  @Override
  public boolean existsByReferencedBusinessEntityId(UUID referencedBusinessEntityId) {
    return fields.stream()
        .anyMatch(field -> referencedBusinessEntityId.equals(field.getReferencedBusinessEntityId()));
  }

  @Override
  public List<EntityField> findByReferencedBusinessEntityId(UUID referencedBusinessEntityId) {
    return fields.stream()
        .filter(field -> referencedBusinessEntityId.equals(field.getReferencedBusinessEntityId()))
        .sorted(Comparator.comparing(EntityField::getBusinessEntityId).thenComparing(EntityField::getName))
        .toList();
  }

  @Override
  public int deleteByBusinessEntityId(UUID businessEntityId) {
    int before = fields.size();
    fields.removeIf(field -> businessEntityId.equals(field.getBusinessEntityId()));
    savedFields.removeIf(field -> businessEntityId.equals(field.getBusinessEntityId()));
    existingNamesByEntity.removeIf(key -> key.startsWith(businessEntityId + ":"));
    return before - fields.size();
  }

  @Override
  public boolean deleteById(UUID id) {
    Optional<EntityField> field = findById(id);
    field.ifPresent(value -> {
      fields.remove(value);
      savedFields.remove(value);
      existingNamesByEntity.remove(key(value.getBusinessEntityId(), value.getName()));
    });
    return field.isPresent();
  }

  private String key(UUID businessEntityId, String name) {
    return businessEntityId + ":" + name;
  }
}
