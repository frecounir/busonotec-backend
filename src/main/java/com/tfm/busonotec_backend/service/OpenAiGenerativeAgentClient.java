package com.tfm.busonotec_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tfm.busonotec_backend.dto.AiBusinessSchemaPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiGenerativeAgentClient implements GenerativeAgentClient {
  private static final String AGENT_INSTRUCTIONS = """
      Eres un asistente que transforma requisitos de usuario en JSON de esquema de negocio listo para base de datos.
      Siempre responde en espanol.
      Genera nombres de entidades, descripciones y nombres de campos en espanol.
      No traduzcas conceptos del usuario al ingles.
      Usa identificadores ASCII: elimina tildes y convierte la letra ene con virgulilla en n.
      Los nombres de entidades deben estar en plural y PascalCase, por ejemplo Estudiantes o Actividades.
      Los nombres de campos deben estar en lowerCamelCase, por ejemplo correoElectronico o fechaInicio.
      Los identificadores deben cumplir ^[a-zA-Z][a-zA-Z0-9_]{0,62}$.
      Los tipos permitidos para campos son string, number, boolean y date.
      Para cada campo decide si required debe ser true o false segun la necesidad de negocio.
      Para campos string puedes proponer minLength y maxLength cuando aporten valor; usa null si no aplica.
      Si type es string, minValue, maxValue, minDate y maxDate deben ser null.
      Para campos number puedes proponer minValue y maxValue cuando aporten valor; usa null si no aplica.
      Si type es number, minLength, maxLength, minDate y maxDate deben ser null.
      Para campos date puedes proponer minDate y maxDate en formato yyyy-MM-dd cuando aporten valor; usa null si no aplica.
      Si type es date, minLength, maxLength, minValue y maxValue deben ser null.
      Si type es boolean, minLength, maxLength, minValue, maxValue, minDate y maxDate deben ser null.
      No agregues validaciones que no correspondan al tipo del campo.
      Nunca incluyas un campo id, porque el backend lo crea automaticamente.
      Manten las descripciones concisas y practicas.
      """;

  private final ObjectMapper objectMapper;
  private final OpenAiResponsesGateway gateway;
  private final String apiKey;
  private final String model;
  private final Duration timeout;

  @Autowired
  public OpenAiGenerativeAgentClient(
      OpenAiResponsesGateway gateway,
      @Value("${ai.openai.api-key:}") String apiKey,
      @Value("${ai.openai.model:gpt-5.2}") String model,
      @Value("${ai.openai.timeout-seconds:30}") long timeoutSeconds
  ) {
    this(new ObjectMapper().findAndRegisterModules(), gateway, apiKey, model, timeoutSeconds);
  }

  OpenAiGenerativeAgentClient(
      ObjectMapper objectMapper,
      OpenAiResponsesGateway gateway,
      String apiKey,
      String model,
      long timeoutSeconds
  ) {
    this.objectMapper = objectMapper;
    this.gateway = gateway;
    this.apiKey = apiKey;
    this.model = model;
    this.timeout = Duration.ofSeconds(timeoutSeconds);
  }

  @Override
  public AiBusinessSchemaPlan generateBusinessSchema(String prompt) {
    validateConfiguration();
    try {
      String requestBody = objectMapper.writeValueAsString(requestBody(prompt));
      OpenAiResponse response = gateway.createResponse(apiKey, timeout, requestBody);
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new IllegalStateException("OpenAI request failed with status " + response.statusCode() + ": " + response.body());
      }
      String outputText = extractOutputText(response.body());
      return objectMapper.readValue(outputText, AiBusinessSchemaPlan.class);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("OpenAI request was interrupted", e);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to generate business schema with OpenAI", e);
    }
  }

  private void validateConfiguration() {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("OPENAI_API_KEY must be configured to use the AI schema assistant");
    }
    if (model == null || model.isBlank()) {
      throw new IllegalStateException("OpenAI model must be configured to use the AI schema assistant");
    }
  }

  private Map<String, Object> requestBody(String prompt) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", model);
    body.put("instructions", AGENT_INSTRUCTIONS);
    body.put("input", prompt);
    body.put("text", Map.of("format", jsonSchemaFormat()));
    return body;
  }

  private Map<String, Object> jsonSchemaFormat() {
    Map<String, Object> format = new LinkedHashMap<>();
    format.put("type", "json_schema");
    format.put("name", "business_schema_plan");
    format.put("description", "Business entities and fields that the backend can create safely.");
    format.put("strict", true);
    format.put("schema", outputSchema());
    return format;
  }

  private Map<String, Object> outputSchema() {
    return Map.of(
        "type", "object",
        "additionalProperties", false,
        "required", List.of("businessEntities"),
        "properties", Map.of(
            "businessEntities", Map.of(
                "type", "array",
                "items", entitySchema()
            )
        )
    );
  }

  private Map<String, Object> entitySchema() {
    return Map.of(
        "type", "object",
        "additionalProperties", false,
        "required", List.of("name", "description", "fields"),
        "properties", Map.of(
            "name", Map.of("type", "string"),
            "description", Map.of("type", "string"),
            "fields", Map.of(
                "type", "array",
                "items", fieldSchema()
            )
        )
    );
  }

  private Map<String, Object> fieldSchema() {
    return Map.of(
        "type", "object",
        "additionalProperties", false,
        "required", List.of(
            "name",
            "type",
            "required",
            "minLength",
            "maxLength",
            "minValue",
            "maxValue",
            "minDate",
            "maxDate"
        ),
        "properties", Map.of(
            "name", Map.of("type", "string"),
            "type", Map.of("type", "string", "enum", List.of("string", "number", "boolean", "date")),
            "required", Map.of("type", List.of("boolean", "null")),
            "minLength", Map.of("type", List.of("integer", "null"), "minimum", 0),
            "maxLength", Map.of("type", List.of("integer", "null"), "minimum", 0),
            "minValue", Map.of("type", List.of("number", "null")),
            "maxValue", Map.of("type", List.of("number", "null")),
            "minDate", Map.of("type", List.of("string", "null"), "format", "date"),
            "maxDate", Map.of("type", List.of("string", "null"), "format", "date")
        )
    );
  }

  private String extractOutputText(String responseBody) throws IOException {
    JsonNode root = objectMapper.readTree(responseBody);
    String outputText = root.path("output_text").asText(null);
    if (outputText != null && !outputText.isBlank()) {
      return outputText;
    }

    for (JsonNode outputItem : root.path("output")) {
      for (JsonNode contentItem : outputItem.path("content")) {
        if ("output_text".equals(contentItem.path("type").asText())) {
          String text = contentItem.path("text").asText(null);
          if (text != null && !text.isBlank()) {
            return text;
          }
        }
      }
    }

    throw new IllegalStateException("OpenAI response did not include structured output text");
  }
}
