package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.dto.AiBusinessSchemaPlan;

public interface GenerativeAgentClient {
  AiBusinessSchemaPlan generateBusinessSchema(String prompt);
}
