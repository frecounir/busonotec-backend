package com.tfm.busonotec_backend.controller;

import com.tfm.busonotec_backend.service.BusinessRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class BusinessRecordController {
  private final BusinessRecordService service;

  public BusinessRecordController(BusinessRecordService service) {
    this.service = service;
  }

  @GetMapping({"/api/business-entities/{businessEntityId}/records", "/api/entities/{businessEntityId}/records"})
  public ResponseEntity<List<Map<String, Object>>> listByBusinessEntity(
      @PathVariable("businessEntityId") UUID businessEntityId
  ) {
    return ResponseEntity.ok(service.listByBusinessEntity(businessEntityId));
  }

  @PostMapping({"/api/business-entities/{businessEntityId}/records", "/api/entities/{businessEntityId}/records"})
  public ResponseEntity<Map<String, Object>> create(
      @PathVariable("businessEntityId") UUID businessEntityId,
      @RequestBody Map<String, Object> record
  ) {
    return ResponseEntity.ok(service.create(businessEntityId, record));
  }
}
