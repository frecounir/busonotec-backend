package com.tfm.busonotec_backend.service.ai;

import com.tfm.busonotec_backend.domain.EntityModel;

import java.util.List;

public interface AIService {
  // Given a free-text prompt, return a list of entity models inferred from the description.
  List<EntityModel> inferModel(String prompt) throws Exception;
}
