package com.tfm.busonotec_backend.controller;

import com.tfm.busonotec_backend.domain.EntityModel;
import com.tfm.busonotec_backend.dto.SchemaRequest;
import com.tfm.busonotec_backend.service.DynamicSchemaService;
import com.tfm.busonotec_backend.service.SchemaBuilder;
import com.tfm.busonotec_backend.service.UIConfigService;
import com.tfm.busonotec_backend.service.ModelValidationService;
import com.tfm.busonotec_backend.service.GenericDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;
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

  private final ModelValidationService validator;
  private final SchemaBuilder builder;
  private final DynamicSchemaService schemaService;
  private final UIConfigService uiConfigService;
  private final GenericDataService dataService;

  public GenericDataController(ModelValidationService validator,
                               SchemaBuilder builder,
                               DynamicSchemaService schemaService,
                               UIConfigService uiConfigService,
                               GenericDataService dataService) {
    this.validator = validator;
    this.builder = builder;
    this.schemaService = schemaService;
    this.uiConfigService = uiConfigService;
    this.dataService = dataService;
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
    Map<String, String> sqlMap = builder.buildCreateStatements(request.getEntities());
    schemaService.executeStatements(sqlMap);
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
    try {
      List<Map<String, Object>> rows = dataService.listData(entity);
      return ResponseEntity.ok(rows);
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
  }

  @PostMapping("/data/{entity}")
  @Operation(summary = "Create row", description = "Insert a row into the specified entity table")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Row inserted"),
      @ApiResponse(responseCode = "400", description = "Invalid input or unknown entity"),
      @ApiResponse(responseCode = "500", description = "Server error")
  })
  public ResponseEntity<?> createRow(@PathVariable String entity, @RequestBody Map<String, Object> payload) {
    try {
      dataService.insertRow(entity, payload);
      return ResponseEntity.ok(Map.of("status", "ok"));
    } catch (IllegalArgumentException ex) {
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
  }
}
