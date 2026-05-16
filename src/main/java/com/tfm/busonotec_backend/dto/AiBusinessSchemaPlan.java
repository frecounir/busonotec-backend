package com.tfm.busonotec_backend.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Plan JSON estructurado generado por el asistente de IA en espanol.")
public record AiBusinessSchemaPlan(
    @ArraySchema(
        schema = @Schema(implementation = AiBusinessEntityDefinition.class),
        minItems = 1
    )
    List<AiBusinessEntityDefinition> businessEntities
) {
}
