package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.EntityModel;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 Service class for validation. Delegates to domain.ModelValidator (framework-free).
 Use this instead of service.ModelValidator to avoid name clashes with domain.
 */
@Service
public class ModelValidationService {
  private final com.tfm.busonotec_backend.domain.ModelValidator delegate = new com.tfm.busonotec_backend.domain.ModelValidator();

  public void validateEntities(List<EntityModel> entities) {
    delegate.validateEntities(entities);
  }

  public void validateEntity(EntityModel e) {
    delegate.validateEntity(e);
  }
}
