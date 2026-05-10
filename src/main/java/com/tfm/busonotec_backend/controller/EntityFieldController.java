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

  @PostMapping({"/api/entity-fields", "/api/fields"})
  public ResponseEntity<EntityFieldResponse> create(@RequestBody EntityFieldRequest req) {
    EntityFieldResponse res = service.create(req);
    return ResponseEntity.ok(res);
  }

  @GetMapping({"/api/entity-fields/{businessEntityId}", "/api/entities/{businessEntityId}/fields"})
  public ResponseEntity<List<EntityFieldResponse>> listByEntity(@PathVariable("businessEntityId") UUID businessEntityId) {
    return ResponseEntity.ok(service.listByEntity(businessEntityId));
  }
}
