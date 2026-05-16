package com.tfm.busonotec_backend.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Plan de esquema de negocio generado por IA y recursos creados a partir de el.")
public record AiBusinessSchemaResponse(
    @Schema(implementation = AiBusinessSchemaPlan.class)
    AiBusinessSchemaPlan plan,

    @ArraySchema(schema = @Schema(implementation = CreatedBusinessEntityResponse.class))
    List<CreatedBusinessEntityResponse> createdBusinessEntities
) {
}
