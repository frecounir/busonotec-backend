package com.tfm.busonotec_backend.controller;

import com.tfm.busonotec_backend.dto.EntityFieldRequest;
import com.tfm.busonotec_backend.dto.EntityFieldResponse;
import com.tfm.busonotec_backend.service.EntityFieldService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
public class EntityFieldController {
  private final EntityFieldService service;

  public EntityFieldController(EntityFieldService service) { this.service = service; }

  @PostMapping("/api/fields")
  public ResponseEntity<EntityFieldResponse> create(@RequestBody EntityFieldRequest req) {
    EntityFieldResponse res = service.create(req);
    return ResponseEntity.ok(res);
  }

  @GetMapping("/api/entities/{id}/fields")
  public ResponseEntity<List<EntityFieldResponse>> listByEntity(@PathVariable UUID id) {
    return ResponseEntity.ok(service.listByEntity(id));
  }
}
