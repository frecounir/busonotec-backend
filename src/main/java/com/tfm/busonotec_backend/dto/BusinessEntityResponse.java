package com.tfm.busonotec_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * DTO returned after creating or listing BusinessEntity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Business entity metadata returned by the API.")
public class BusinessEntityResponse {
  @Schema(
      description = "Business entity UUID.",
      example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      format = "uuid",
      accessMode = Schema.AccessMode.READ_ONLY
  )
  private UUID id;

  @Schema(description = "Business entity name.", example = "Students")
  private String name;

  @Schema(description = "Business entity description.", example = "Students enrolled in academic activities")
  private String description;
}
