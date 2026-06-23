package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.BusinessEntity;
import com.tfm.busonotec_backend.dto.BusinessEntityRequest;
import com.tfm.busonotec_backend.dto.BusinessEntityResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class BusinessEntityMapper {
  public BusinessEntity toDomain(UUID id, BusinessEntityRequest request) {
    return new BusinessEntity(id, request.getName(), request.getDescription());
  }

  public BusinessEntityResponse toResponse(BusinessEntity entity) {
    return new BusinessEntityResponse(entity.getId(), entity.getName(), entity.getDescription());
  }
}
