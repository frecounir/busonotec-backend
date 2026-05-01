package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.EntityModel;
import com.tfm.busonotec_backend.domain.FieldModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/*
 Build safe CREATE TABLE statements from validated models.
*/
@Service
public class SchemaBuilder {

  public List<String> buildCreateStatements(List<EntityModel> entities) {
    List<String> statements = new ArrayList<>();
    for (EntityModel e : entities) {
      statements.add(createTableFor(e));
    }
    return statements;
  }

  private String createTableFor(EntityModel e) {
    StringBuilder sb = new StringBuilder();
    String table = sanitizeIdentifier(e.getName());
    sb.append("CREATE TABLE IF NOT EXISTS ").append(table).append(" (\n");
    // simple id primary key if not present
    boolean hasId = e.getFields().stream().anyMatch(f -> "id".equalsIgnoreCase(f.getName()));
    if (!hasId) {
      sb.append("  id SERIAL PRIMARY KEY,\n");
    }
    for (int i = 0; i < e.getFields().size(); i++) {
      FieldModel f = e.getFields().get(i);
      sb.append("  ").append(sanitizeIdentifier(f.getName())).append(" ").append(mapType(f.getType()));
      if ("id".equalsIgnoreCase(f.getName())) {
        sb.append(" PRIMARY KEY");
      }
      if (i < e.getFields().size() - 1) sb.append(",\n");
      else sb.append("\n");
    }
    sb.append(");");
    return sb.toString();
  }

  private String mapType(String logical) {
    return switch (logical) {
      case "STRING" -> "VARCHAR(255)";
      case "INTEGER" -> "INTEGER";
      case "BOOLEAN" -> "BOOLEAN";
      case "TEXT" -> "TEXT";
      case "DATE" -> "DATE";
      case "TIMESTAMP" -> "TIMESTAMP";
      default -> "TEXT";
    };
  }

  private String sanitizeIdentifier(String id) {
    // identifier already validated by ModelValidator; defensively quote if needed
    return "\"" + id.toLowerCase() + "\"";
  }
}
