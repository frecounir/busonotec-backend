package com.tfm.busonotec_backend.controller;

import com.tfm.busonotec_backend.dto.EntityFieldRequest;
import com.tfm.busonotec_backend.dto.EntityFieldResponse;
import com.tfm.busonotec_backend.service.EntityFieldService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(
    name = "Entity Fields",
    description = "Manage dynamic fields for business entities and synchronize them with physical table columns."
)
public class EntityFieldController {
  private final EntityFieldService service;

  public EntityFieldController(EntityFieldService service) { this.service = service; }

  @Operation(
      summary = "Create an entity field",
      description = "Registers a field for a business entity and adds the matching column to the physical table."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Entity field created.",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = EntityFieldResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "id": "c5ad7a80-63bb-4b0f-9679-2c0b1f5fcf9d",
                    "businessEntityId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                    "name": "score",
                    "type": "number"
                  }
                  """)
          )
      ),
      @ApiResponse(responseCode = "400", description = "Invalid field name, unsupported type, duplicated field, or missing entity.", content = @Content)
  })
  @PostMapping({"/api/entity-fields", "/api/fields"})
  public ResponseEntity<EntityFieldResponse> create(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          description = "Entity field metadata.",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = EntityFieldRequest.class),
              examples = @ExampleObject(value = """
                  {
                    "businessEntityId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                    "name": "score",
                    "type": "number"
                  }
                  """)
          )
      )
      @RequestBody EntityFieldRequest req
  ) {
    EntityFieldResponse res = service.create(req);
    return ResponseEntity.ok(res);
  }

  @Operation(
      summary = "List fields for a business entity",
      description = "Returns all dynamic fields registered for the provided business entity."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Entity fields returned.",
          content = @Content(
              mediaType = "application/json",
              array = @ArraySchema(schema = @Schema(implementation = EntityFieldResponse.class))
          )
      ),
      @ApiResponse(responseCode = "400", description = "Invalid business entity UUID.", content = @Content)
  })
  @GetMapping({"/api/entity-fields/{businessEntityId}", "/api/entities/{businessEntityId}/fields"})
  public ResponseEntity<List<EntityFieldResponse>> listByEntity(
      @Parameter(
          description = "Business entity UUID.",
          example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
          required = true
      )
      @PathVariable("businessEntityId") UUID businessEntityId
  ) {
    return ResponseEntity.ok(service.listByEntity(businessEntityId));
  }
}
