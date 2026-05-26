package com.tfm.busonotec_backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tfm.busonotec_backend.dto.AiBusinessSchemaPlan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAiGenerativeAgentClient implements GenerativeAgentClient {
  private static final String AGENT_INSTRUCTIONS = """
      Eres un asistente que transforma requisitos de usuario en JSON de esquema de negocio listo para base de datos.
      Siempre responde en espanol.
      Trata la respuesta como un modelo entidad relacion: antes de responder, normaliza el modelo de datos.
      Aplica buenas practicas de normalizacion: entidades con una sola responsabilidad, campos atomicos, sin grupos repetidos,
      sin datos derivados innecesarios, sin duplicar atributos entre entidades y con dependencias claras de cada campo hacia su entidad.
      Cuando exista una relacion uno a muchos, modelala como un campo relationship en la entidad hija apuntando a la entidad padre.
      Cuando exista una relacion muchos a muchos, crea una entidad intermedia normalizada con dos campos relationship many_to_one.
      Usa relationshipType many_to_one para llaves foraneas comunes y one_to_one solo cuando el negocio exija unicidad.
      Para campos relationship, referencedEntityName debe ser el nombre exacto de otra entidad incluida en businessEntities.
      Genera nombres de entidades, descripciones y nombres de campos en espanol.
      No traduzcas conceptos del usuario al ingles.
      Usa identificadores ASCII: elimina tildes y convierte la letra ene con virgulilla en n.
      Los nombres de entidades deben estar en plural y PascalCase, por ejemplo Estudiantes o Actividades.
      Los nombres de campos deben estar en lowerCamelCase, por ejemplo correoElectronico o fechaInicio.
      Los identificadores deben cumplir ^[a-zA-Z][a-zA-Z0-9_]{0,62}$.
      Los tipos permitidos para campos son string, number, boolean, date y relationship.
      Para cada campo decide si required debe ser true o false segun la necesidad de negocio.
      Para campos string puedes proponer minLength y maxLength cuando aporten valor; usa null si no aplica.
      Si type es string, minValue, maxValue, minDate y maxDate deben ser null.
      Para campos number puedes proponer minValue y maxValue cuando aporten valor; usa null si no aplica.
      Si type es number, minLength, maxLength, minDate y maxDate deben ser null.
      Para campos date puedes proponer minDate y maxDate en formato yyyy-MM-dd cuando aporten valor; usa null si no aplica.
      Si type es date, minLength, maxLength, minValue y maxValue deben ser null.
      Si type es boolean, minLength, maxLength, minValue, maxValue, minDate y maxDate deben ser null.
      Si type es relationship, minLength, maxLength, minValue, maxValue, minDate y maxDate deben ser null,
      relationshipType debe ser many_to_one u one_to_one, y referencedEntityName debe tener la entidad destino.
      Si type no es relationship, relationshipType y referencedEntityName deben ser null.
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
            "maxDate",
            "relationshipType",
            "referencedEntityName"
        ),
        "properties", fieldProperties()
    );
  }

  private Map<String, Object> fieldProperties() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("name", Map.of("type", "string"));
    properties.put("type", Map.of("type", "string", "enum", List.of("string", "number", "boolean", "date", "relationship")));
    properties.put("required", Map.of("type", List.of("boolean", "null")));
    properties.put("minLength", Map.of("type", List.of("integer", "null"), "minimum", 0));
    properties.put("maxLength", Map.of("type", List.of("integer", "null"), "minimum", 0));
    properties.put("minValue", Map.of("type", List.of("number", "null")));
    properties.put("maxValue", Map.of("type", List.of("number", "null")));
    properties.put("minDate", Map.of("type", List.of("string", "null"), "format", "date"));
    properties.put("maxDate", Map.of("type", List.of("string", "null"), "format", "date"));
    properties.put("relationshipType", Map.of("type", List.of("string", "null"), "enum", Arrays.asList("many_to_one", "one_to_one", null)));
    properties.put("referencedEntityName", Map.of("type", List.of("string", "null")));
    return properties;
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
