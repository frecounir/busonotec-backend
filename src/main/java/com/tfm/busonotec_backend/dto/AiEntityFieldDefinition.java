package com.tfm.busonotec_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Definicion de campo generada por IA lista para convertirse en columna fisica.")
public record AiEntityFieldDefinition(
    @Schema(
        description = "Nombre del campo en espanol. Debe iniciar con una letra y puede contener letras, numeros y guiones bajos.",
        example = "puntaje",
        pattern = "^[a-zA-Z][a-zA-Z0-9_]{0,62}$",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String name,

    @Schema(
        description = "Tipo logico del campo mapeado a un tipo de columna en base de datos.",
        example = "number",
        allowableValues = {"string", "number", "boolean", "date"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String type
) {
}
