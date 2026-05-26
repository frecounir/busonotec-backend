package com.tfm.busonotec_backend.support;

import com.tfm.busonotec_backend.service.DynamicSchemaService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class RecordingDynamicSchemaService extends DynamicSchemaService {
  private final Map<String, String> statementsByEntity = new LinkedHashMap<>();
  private final List<AddedColumn> addedColumns = new ArrayList<>();
  private final List<AddedRelationshipColumn> addedRelationshipColumns = new ArrayList<>();
  private final List<DroppedColumn> droppedColumns = new ArrayList<>();
  private final List<String> droppedEntities = new ArrayList<>();
  private final Set<String> existingEntities = new HashSet<>();

  public RecordingDynamicSchemaService() {
    super(null);
  }

  @Override
  public void executeStatements(Map<String, String> statementsByEntity) {
    if (statementsByEntity != null) {
      this.statementsByEntity.putAll(statementsByEntity);
    }
  }

  @Override
  public void addColumn(String entityName, String fieldName, String logicalType) {
    addedColumns.add(new AddedColumn(entityName, fieldName, logicalType));
  }

  @Override
  public void addRelationshipColumn(String entityName, String fieldName, String referencedEntityName, String relationshipType) {
    addedRelationshipColumns.add(new AddedRelationshipColumn(entityName, fieldName, referencedEntityName, relationshipType));
  }

  @Override
  public void dropColumn(String entityName, String fieldName) {
    droppedColumns.add(new DroppedColumn(entityName, fieldName));
  }

  @Override
  public boolean entityExists(String entityName) {
    return existingEntities.contains(entityName);
  }

  @Override
  public void dropEntityTable(String entityName) {
    droppedEntities.add(entityName);
    existingEntities.remove(entityName);
  }

  public void markEntityAsExisting(String entityName) {
    existingEntities.add(entityName);
  }

  public Optional<String> statementFor(String entityName) {
    return Optional.ofNullable(statementsByEntity.get(entityName));
  }

  public List<AddedColumn> addedColumns() {
    return List.copyOf(addedColumns);
  }

  public boolean hasNoStatements() {
    return statementsByEntity.isEmpty();
  }

  public boolean hasNoAddedColumns() {
    return addedColumns.isEmpty() && addedRelationshipColumns.isEmpty();
  }

  public List<AddedRelationshipColumn> addedRelationshipColumns() {
    return List.copyOf(addedRelationshipColumns);
  }

  public List<DroppedColumn> droppedColumns() {
    return List.copyOf(droppedColumns);
  }

  public boolean hasNoDroppedColumns() {
    return droppedColumns.isEmpty();
  }

  public List<String> droppedEntities() {
    return List.copyOf(droppedEntities);
  }

  public record AddedColumn(String entityName, String fieldName, String logicalType) {
  }

  public record AddedRelationshipColumn(String entityName, String fieldName, String referencedEntityName, String relationshipType) {
  }

  public record DroppedColumn(String entityName, String fieldName) {
  }
}
