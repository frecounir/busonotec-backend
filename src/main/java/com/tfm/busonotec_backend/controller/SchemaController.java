package com.tfm.busonotec_backend.controller;

import com.tfm.busonotec_backend.dto.ColumnResponse;
import com.tfm.busonotec_backend.service.SchemaMetadataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schema")
public class SchemaController {

    private final SchemaMetadataService schemaService;

    public SchemaController(SchemaMetadataService schemaService) {
        this.schemaService = schemaService;
    }

    /**
     * GET /api/schema/tables
     */
    @GetMapping("/tables")
    public ResponseEntity<List<String>> listTables() {
        List<String> tables = schemaService.listTables();
        return ResponseEntity.ok(tables);
    }

    /**
     * GET /api/schema/{table}/columns
     */
    @GetMapping("/{table}/columns")
    public ResponseEntity<?> getColumns(@PathVariable("table") String table) {
        try {
            List<ColumnResponse> cols = schemaService.getColumns(table);
            return ResponseEntity.ok(cols);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
}
