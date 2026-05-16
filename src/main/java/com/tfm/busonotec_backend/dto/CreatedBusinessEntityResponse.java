package com.tfm.busonotec_backend.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Entidad de negocio y campos creados desde un plan generado por IA.")
public record CreatedBusinessEntityResponse(
    @Schema(implementation = BusinessEntityResponse.class)
    BusinessEntityResponse businessEntity,

    @ArraySchema(schema = @Schema(implementation = EntityFieldResponse.class))
    List<EntityFieldResponse> fields
) {
}
