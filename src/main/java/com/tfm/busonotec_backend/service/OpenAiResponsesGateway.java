package com.tfm.busonotec_backend.service;

import java.io.IOException;
import java.time.Duration;

public interface OpenAiResponsesGateway {
  OpenAiResponse createResponse(String apiKey, Duration timeout, String requestBody)
      throws IOException, InterruptedException;
}
