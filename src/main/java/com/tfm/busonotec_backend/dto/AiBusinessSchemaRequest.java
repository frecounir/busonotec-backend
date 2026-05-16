package com.tfm.busonotec_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Prompt usado por el asistente de IA para crear un esquema de negocio en espanol.")
public record AiBusinessSchemaRequest(
    @Schema(
        description = "Instruccion en lenguaje natural que describe las entidades de negocio y campos a crear.",
        example = "Crea entidades para estudiantes y actividades. Estudiantes necesita nombre, correoElectronico y activo. Actividades necesita titulo, fechaInicio y duracion.",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String prompt
) {
}
