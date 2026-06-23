package com.tfm.busonotec_backend.controller;

import com.tfm.busonotec_backend.dto.BusinessEntityRequest;
import com.tfm.busonotec_backend.dto.BusinessEntityResponse;
import com.tfm.busonotec_backend.service.BusinessEntityService;
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
@RequestMapping("/api/business-entities")
@Tag(
    name = "Business Entities",
    description = "Manage logical business entities and their physical database tables."
)
public class BusinessEntityController {
  private final BusinessEntityService service;

  public BusinessEntityController(BusinessEntityService service) {
      this.service = service;
  }

  @Operation(
      summary = "Create a business entity",
      description = "Registers a business entity and creates its physical table with an auto-generated UUID primary key."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Business entity created.",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = BusinessEntityResponse.class),
              examples = @ExampleObject(value = """
                  {
                    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                    "name": "Students",
                    "description": "Students enrolled in academic activities"
                  }
                  """)
          )
      ),
      @ApiResponse(responseCode = "400", description = "Invalid entity name or duplicated entity.", content = @Content)
  })
  @PostMapping
  public ResponseEntity<BusinessEntityResponse> create(
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          description = "Business entity metadata.",
          content = @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = BusinessEntityRequest.class),
              examples = @ExampleObject(value = """
                  {
                    "name": "Students",
                    "description": "Students enrolled in academic activities"
                  }
                  """)
          )
      )
      @RequestBody BusinessEntityRequest req
  ) {
    BusinessEntityResponse res = service.create(req);
    return ResponseEntity.ok(res);
  }

  @Operation(
      summary = "List business entities",
      description = "Returns all registered business entities ordered by name."
  )
  @ApiResponse(
      responseCode = "200",
      description = "Business entities returned.",
      content = @Content(
          mediaType = "application/json",
          array = @ArraySchema(schema = @Schema(implementation = BusinessEntityResponse.class))
      )
  )
  @GetMapping
  public ResponseEntity<List<BusinessEntityResponse>> list() {
    return ResponseEntity.ok(service.list());
  }

  @Operation(
      summary = "Get a business entity by UUID",
      description = "Returns a single business entity using its metadata UUID."
  )
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "Business entity returned.",
          content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusinessEntityResponse.class))
      ),
      @ApiResponse(responseCode = "400", description = "Invalid UUID.", content = @Content),
      @ApiResponse(responseCode = "404", description = "Business entity not found.", content = @Content)
  })
  @GetMapping("/{id}")
  public ResponseEntity<BusinessEntityResponse> findById(
      @Parameter(
          description = "Business entity UUID.",
          example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
          required = true
      )
      @PathVariable UUID id
  ) {
    return ResponseEntity.ok(service.findById(id));
  }

  @Operation(
      summary = "Delete a business entity",
      description = "Deletes the business entity metadata, its registered fields, and its physical database table."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Business entity deleted.", content = @Content),
      @ApiResponse(responseCode = "400", description = "Invalid UUID.", content = @Content),
      @ApiResponse(responseCode = "404", description = "Business entity not found.", content = @Content)
  })
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      @Parameter(
          description = "Business entity UUID.",
          example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
          required = true
      )
      @PathVariable UUID id
  ) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
