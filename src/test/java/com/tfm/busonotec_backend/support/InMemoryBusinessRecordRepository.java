package com.tfm.busonotec_backend.support;

import com.tfm.busonotec_backend.repository.BusinessRecordRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class InMemoryBusinessRecordRepository extends BusinessRecordRepository {
  private final List<Map<String, Object>> records = new ArrayList<>();
  private final List<String> queriedEntityNames = new ArrayList<>();
  private final List<String> createdEntityNames = new ArrayList<>();

  public InMemoryBusinessRecordRepository() {
    super(null);
  }

  public void add(Map<String, Object> record) {
    records.add(record);
  }

  public List<String> queriedEntityNames() {
    return List.copyOf(queriedEntityNames);
  }

  public boolean hasNoQueries() {
    return queriedEntityNames.isEmpty();
  }

  public List<String> createdEntityNames() {
    return List.copyOf(createdEntityNames);
  }

  @Override
  public List<Map<String, Object>> findAllByEntityName(String entityName) {
    queriedEntityNames.add(entityName);
    return List.copyOf(records);
  }

  @Override
  public Map<String, Object> create(String entityName, UUID recordId, Map<String, Object> valuesByColumn) {
    createdEntityNames.add(entityName);
    Map<String, Object> record = new LinkedHashMap<>();
    record.put("id", recordId);
    record.putAll(valuesByColumn);
    records.add(record);
    return record;
  }
}
