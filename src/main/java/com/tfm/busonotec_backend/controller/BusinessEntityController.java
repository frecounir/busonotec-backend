package com.tfm.busonotec_backend.controller;

import com.tfm.busonotec_backend.dto.BusinessEntityRequest;
import com.tfm.busonotec_backend.dto.BusinessEntityResponse;
import com.tfm.busonotec_backend.service.BusinessEntityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entities")
public class BusinessEntityController {
  private final BusinessEntityService service;

  public BusinessEntityController(BusinessEntityService service) { this.service = service; }

  @PostMapping
  public ResponseEntity<BusinessEntityResponse> create(@RequestBody BusinessEntityRequest req) {
    BusinessEntityResponse res = service.create(req);
    return ResponseEntity.ok(res);
  }

  @GetMapping
  public ResponseEntity<List<BusinessEntityResponse>> list() {
    return ResponseEntity.ok(service.list());
  }
}
