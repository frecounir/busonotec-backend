package com.tfm.busonotec_backend.service;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class HttpOpenAiResponsesGateway implements OpenAiResponsesGateway {
  private static final URI RESPONSES_URI = URI.create("https://api.openai.com/v1/responses");

  private final HttpClient httpClient;

  public HttpOpenAiResponsesGateway() {
    this(HttpClient.newHttpClient());
  }

  HttpOpenAiResponsesGateway(HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  @Override
  public OpenAiResponse createResponse(String apiKey, Duration timeout, String requestBody)
      throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder(RESPONSES_URI)
        .timeout(timeout)
        .header("Authorization", "Bearer " + apiKey)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
        .build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return new OpenAiResponse(response.statusCode(), response.body());
  }
}
