package com.tfm.busonotec_backend.controller;

import com.tfm.busonotec_backend.dto.AiBusinessSchemaRequest;
import com.tfm.busonotec_backend.dto.AiBusinessSchemaResponse;
import com.tfm.busonotec_backend.service.AiBusinessSchemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(
    name = "AI Business Schema",
    description = "Genera y crea entidades de negocio en espanol usando un asistente de IA generativa."
)
public class AiBusinessSchemaController {
  private final AiBusinessSchemaService service;

  public AiBusinessSchemaController(AiBusinessSchemaService service) {
    this.service = service;
  }

  @Operation(
      summary = "Crear esquema de negocio desde un prompt",
      description = "Envia un prompt en lenguaje natural al asistente de IA generativa. El asistente responde un plan JSON estructurado en espanol, y el backend usa los servicios existentes de entidades y campos para crear metadata, tablas fisicas y columnas."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Esquema de negocio generado y creado.",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AiBusinessSchemaResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "plan": {
                      "businessEntities": [
                        {
                          "name": "Estudiantes",
                          "description": "Estudiantes inscritos en actividades academicas",
                          "fields": [
                            { "name": "nombre", "type": "string" },
                            { "name": "correoElectronico", "type": "string" },
                            { "name": "activo", "type": "boolean" }
                          ]
                        }
                      ]
                    },
                    "createdBusinessEntities": [
                      {
                        "businessEntity": {
                          "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                          "name": "Estudiantes",
                          "description": "Estudiantes inscritos en actividades academicas"
                        },
                        "fields": [
                          {
                            "id": "58f2a6a4-8e72-4d4f-bb13-ecf5f1f2c7c1",
                            "businessEntityId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                            "name": "nombre",
                            "type": "string"
                          }
                        ]
                      }
                    ]
                  }
                  """)
          )
      ),
      @ApiResponse(responseCode = "400", description = "Prompt invalido o esquema generado por IA invalido.", content = @Content),
      @ApiResponse(responseCode = "500", description = "Error de configuracion o comunicacion con el proveedor de IA.", content = @Content)
  })
  @PostMapping("/api/ai/business-schema")
  public ResponseEntity<AiBusinessSchemaResponse> createFromPrompt(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          description = "Prompt que describe las entidades de negocio y campos a crear.",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = AiBusinessSchemaRequest.class),
              examples = @ExampleObject(value = """
                  {
                    "prompt": "Crea entidades para estudiantes y actividades. Estudiantes necesita nombre, correoElectronico y activo. Actividades necesita titulo, fechaInicio y duracion."
                  }
                  """)
          )
      )
      @RequestBody AiBusinessSchemaRequest request
  ) {
    return ResponseEntity.ok(service.createFromPrompt(request));
  }
}
