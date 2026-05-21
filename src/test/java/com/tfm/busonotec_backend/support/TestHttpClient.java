package com.tfm.busonotec_backend.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class TestHttpClient {
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private final HttpClient client;
  private final int port;

  public TestHttpClient(int port) {
    this.port = port;
    this.client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();
  }

  public HttpResponse<String> get(String path) throws IOException, InterruptedException {
    return send(HttpRequest.newBuilder(uri(path))
        .timeout(REQUEST_TIMEOUT)
        .GET()
        .build());
  }

  public HttpResponse<String> postJson(String path, Object body) throws IOException, InterruptedException {
    return sendJson("POST", path, body);
  }

  public HttpResponse<String> putJson(String path, Object body) throws IOException, InterruptedException {
    return sendJson("PUT", path, body);
  }

  public HttpResponse<String> patchJson(String path, Object body) throws IOException, InterruptedException {
    return sendJson("PATCH", path, body);
  }

  public HttpResponse<String> delete(String path) throws IOException, InterruptedException {
    return send(HttpRequest.newBuilder(uri(path))
        .timeout(REQUEST_TIMEOUT)
        .DELETE()
        .build());
  }

  public HttpResponse<String> options(String path, Map<String, String> headers)
      throws IOException, InterruptedException {
    HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
        .timeout(REQUEST_TIMEOUT)
        .method("OPTIONS", HttpRequest.BodyPublishers.noBody());
    headers.forEach(builder::header);
    return send(builder.build());
  }

  public <T> T readBody(HttpResponse<String> response, Class<T> responseType) throws JsonProcessingException {
    return objectMapper.readValue(response.body(), responseType);
  }

  public List<String> readTextValues(HttpResponse<String> response, String fieldName) throws JsonProcessingException {
    return objectMapper.readTree(response.body()).findValuesAsText(fieldName);
  }

  public List<Map<String, Object>> readRows(HttpResponse<String> response) throws JsonProcessingException {
    return objectMapper.readValue(response.body(), new TypeReference<>() {
    });
  }

  public Map<String, Object> readRecord(HttpResponse<String> response) throws JsonProcessingException {
    return objectMapper.readValue(response.body(), new TypeReference<>() {
    });
  }

  private HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> sendJson(String method, String path, Object body)
      throws IOException, InterruptedException {
    return send(HttpRequest.newBuilder(uri(path))
        .timeout(REQUEST_TIMEOUT)
        .header("Content-Type", "application/json")
        .method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
        .build());
  }

  private URI uri(String path) {
    return URI.create("http://localhost:" + port + path);
  }
}
