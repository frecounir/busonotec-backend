package com.tfm.busonotec_backend.support;

import com.tfm.busonotec_backend.service.DynamicSchemaService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RecordingDynamicSchemaService extends DynamicSchemaService {
  private final Map<String, String> statementsByEntity = new LinkedHashMap<>();
  private final List<AddedColumn> addedColumns = new ArrayList<>();

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
    return addedColumns.isEmpty();
  }

  public record AddedColumn(String entityName, String fieldName, String logicalType) {
  }
}
