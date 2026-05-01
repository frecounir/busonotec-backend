package com.tfm.busonotec_backend.controller;

import com.tfm.busonotec_backend.domain.EntityModel;
import com.tfm.busonotec_backend.dto.SchemaRequest;
import com.tfm.busonotec_backend.service.DynamicSchemaService;
import com.tfm.busonotec_backend.service.SchemaBuilder;
import com.tfm.busonotec_backend.service.UIConfigService;
import com.tfm.busonotec_backend.service.ModelValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

/*
 Exposes endpoints:
 POST /api/schema -> create tables
 GET /api/data/{entity} -> list rows
 POST /api/data/{entity} -> insert a row
*/
@RestController
@RequestMapping("/api")
public class GenericDataController {

  private final ModelValidator validator;
  private final SchemaBuilder builder;
  private final DynamicSchemaService schemaService;
  private final UIConfigService uiConfigService;
  private final JdbcTemplate jdbc;

  public GenericDataController(ModelValidator validator,
                               SchemaBuilder builder,
                               DynamicSchemaService schemaService,
                               UIConfigService uiConfigService,
                               JdbcTemplate jdbc) {
    this.validator = validator;
    this.builder = builder;
    this.schemaService = schemaService;
    this.uiConfigService = uiConfigService;
    this.jdbc = jdbc;
  }

  @PostMapping("/schema")
  @Operation(summary = "Create schema", description = "Create database tables from provided entity models and return UI configuration")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Schema created and UI config returned"),
      @ApiResponse(responseCode = "400", description = "Validation failed"),
      @ApiResponse(responseCode = "500", description = "Server error")
  })
  public ResponseEntity<?> createSchema(@RequestBody SchemaRequest request) {
    validator.validateEntities(request.getEntities());
    List<String> sql = builder.buildCreateStatements(request.getEntities());
    schemaService.executeStatements(sql);
    List<String> created = request.getEntities().stream().map(EntityModel::getName).collect(Collectors.toList());
    return ResponseEntity.ok(Map.of(
        "created", created,
        "ui", uiConfigService.buildUiConfig(request.getEntities())
    ));
  }

  @GetMapping("/data/{entity}")
  @Operation(summary = "List data", description = "List rows for an entity (max 100)")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "List of rows"),
      @ApiResponse(responseCode = "400", description = "Unknown entity"),
      @ApiResponse(responseCode = "500", description = "Server error")
  })
  public ResponseEntity<?> listData(@PathVariable String entity) {
    if (!schemaService.entityExists(entity)) {
      return ResponseEntity.badRequest().body(Map.of("error", "Unknown entity"));
    }
    // Basic SELECT - limit results. Field names are taken from DB driver.
    String safe = "\"" + entity.toLowerCase() + "\"";
    List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM " + safe + " LIMIT 100");
    return ResponseEntity.ok(rows);
  }

  @PostMapping("/data/{entity}")
  @Operation(summary = "Create row", description = "Insert a row into the specified entity table")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Row inserted"),
      @ApiResponse(responseCode = "400", description = "Invalid input or unknown entity"),
      @ApiResponse(responseCode = "500", description = "Server error")
  })
  public ResponseEntity<?> createRow(@PathVariable String entity, @RequestBody Map<String, Object> payload) {
    if (!schemaService.entityExists(entity)) {
      return ResponseEntity.badRequest().body(Map.of("error", "Unknown entity"));
    }
    String table = "\"" + entity.toLowerCase() + "\"";
    // Validate keys look like identifiers
    for (String k : payload.keySet()) {
      if (!k.matches("^[a-zA-Z][a-zA-Z0-9_]{0,62}$")) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid column name: " + k));
      }
    }
    // Build insert with prepared params
    List<String> keys = new ArrayList<>(payload.keySet());
    String cols = keys.stream().map(s -> "\"" + s.toLowerCase() + "\"").collect(Collectors.joining(", "));
    String placeholders = keys.stream().map(k -> "?").collect(Collectors.joining(", "));
    Object[] values = keys.stream().map(payload::get).toArray();
    String sql = "INSERT INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")";
    jdbc.update(sql, values);
    return ResponseEntity.ok(Map.of("status", "ok"));
  }
}
