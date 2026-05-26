package com.tfm.busonotec_backend.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Definicion de entidad de negocio generada por IA lista para convertirse en metadata, tabla, columnas y relaciones normalizadas.")
public record AiBusinessEntityDefinition(
    @Schema(
        description = "Nombre de la entidad de negocio en espanol. Debe iniciar con una letra y puede contener letras, numeros y guiones bajos.",
        example = "Estudiantes",
        pattern = "^[a-zA-Z][a-zA-Z0-9_]{0,62}$",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String name,

    @Schema(
        description = "Descripcion legible de la entidad de negocio.",
        example = "Estudiantes inscritos en actividades academicas",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String description,

    @ArraySchema(
        schema = @Schema(implementation = AiEntityFieldDefinition.class),
        minItems = 0
    )
    List<AiEntityFieldDefinition> fields
) {
}
