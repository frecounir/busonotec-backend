package com.tfm.busonotec_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

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
        allowableValues = {"string", "number", "boolean", "date", "relationship"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String type,

    @Schema(description = "Indica si el campo debe ser obligatorio para crear records.", example = "true")
    Boolean required,

    @Schema(description = "Longitud minima para campos string. Debe ser null para otros tipos.", example = "3")
    Integer minLength,

    @Schema(description = "Longitud maxima para campos string. Debe ser null para otros tipos.", example = "120")
    Integer maxLength,

    @Schema(description = "Valor minimo para campos number. Debe ser null para otros tipos.", example = "0")
    BigDecimal minValue,

    @Schema(description = "Valor maximo para campos number. Debe ser null para otros tipos.", example = "100")
    BigDecimal maxValue,

    @Schema(description = "Fecha minima para campos date. Debe ser null para otros tipos.", example = "2026-01-01", format = "date")
    LocalDate minDate,

    @Schema(description = "Fecha maxima para campos date. Debe ser null para otros tipos.", example = "2026-12-31", format = "date")
    LocalDate maxDate,

    @Schema(
        description = "Cardinalidad de la relacion cuando type es relationship. Debe ser null para otros tipos.",
        example = "many_to_one",
        allowableValues = {"many_to_one", "one_to_one"}
    )
    String relationshipType,

    @Schema(
        description = "Nombre de la entidad de negocio destino cuando type es relationship. Debe existir en businessEntities y ser null para otros tipos.",
        example = "Actividades"
    )
    String referencedEntityName
) {
  public AiEntityFieldDefinition(String name, String type) {
    this(name, type, null, null, null, null, null, null, null, null, null);
  }

  public AiEntityFieldDefinition(String name,
                                 String type,
                                 Boolean required,
                                 Integer minLength,
                                 Integer maxLength,
                                 BigDecimal minValue,
                                 BigDecimal maxValue,
                                 LocalDate minDate,
                                 LocalDate maxDate) {
    this(name, type, required, minLength, maxLength, minValue, maxValue, minDate, maxDate, null, null);
  }
}
