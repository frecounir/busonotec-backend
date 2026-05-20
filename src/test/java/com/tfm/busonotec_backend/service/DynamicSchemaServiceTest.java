package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.support.RecordingJdbcTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DynamicSchemaServiceTest {
  private static final String CREATE_STUDENTS_TABLE =
      "CREATE TABLE IF NOT EXISTS \"students\" (id UUID PRIMARY KEY)";

  private RecordingJdbcTemplate jdbc;
  private DynamicSchemaService service;

  @BeforeEach
  void setUp() {
    jdbc = new RecordingJdbcTemplate();
    service = new DynamicSchemaService(jdbc);
  }

  @Test
  void executeStatementsIgnoresNullOrEmptyInput() {
    service.executeStatements(null);
    service.executeStatements(Map.of());

    assertThat(jdbc.hasNoExecutedSql()).isTrue();
    assertThat(service.getCreatedEntities()).isEmpty();
  }

  @Test
  void executeStatementsAllowsExpectedCreateTableStatements() {
    Map<String, String> statements = new LinkedHashMap<>();
    statements.put("Students", CREATE_STUDENTS_TABLE);

    service.executeStatements(statements);

    assertThat(jdbc.executedSql())
        .containsExactly(CREATE_STUDENTS_TABLE);
    assertThat(service.getCreatedEntities()).containsExactlyInAnyOrder("students");
  }

  @ParameterizedTest
  @MethodSource("invalidSqlStatements")
  void executeStatementsRejectsUnsafeSql(String sql) {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.executeStatements(Map.of("Students", sql)));

    assertMessageContainsAnyOf(exception,
        "SQL statement is empty",
        "Multiple statements",
        "Dangerous SQL keywords",
        "Only the expected CREATE TABLE");
    assertThat(jdbc.hasNoExecutedSql()).isTrue();
  }

  @Test
  void executeStatementsRejectsNullEntityAndNullSql() {
    assertThrows(NullPointerException.class,
        () -> service.executeStatements(singleEntryMap(null, "CREATE TABLE demo(id UUID)")));
    assertThrows(NullPointerException.class,
        () -> service.executeStatements(singleEntryMap("Demo", null)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "1students", "student-name"})
  void entityExistsReturnsFalseForInvalidNames(String entityName) {
    assertThat(service.entityExists(entityName)).isFalse();
    assertThat(jdbc.hasNoQueries()).isTrue();
  }

  @Test
  void entityExistsReturnsFalseForNullName() {
    assertThat(service.entityExists(null)).isFalse();
    assertThat(jdbc.hasNoQueries()).isTrue();
  }

  @Test
  void entityExistsUsesCacheBeforeDatabaseLookup() {
    registerStudentsTable();

    assertThat(service.entityExists("Students")).isTrue();
    assertThat(jdbc.hasNoQueries()).isTrue();
  }

  @Test
  void entityExistsUsesInformationSchemaWhenEntityIsNotCached() {
    jdbc.tableExists(true);

    assertThat(service.entityExists("Activities")).isTrue();
    assertThat(jdbc.queriedTableNames()).containsExactly("activities");

    jdbc.tableExists(false);

    assertThat(service.entityExists("Teachers")).isFalse();
    assertThat(jdbc.queriedTableNames()).containsExactly("activities", "teachers");
  }

  @ParameterizedTest
  @MethodSource("supportedColumnTypes")
  void addColumnCreatesAlterTableStatementForSupportedTypes(String logicalType, String expectedSqlType) {
    registerStudentsTable();

    service.addColumn("Students", "value_" + logicalType, logicalType);

    assertThat(jdbc.lastExecutedSql())
        .isEqualTo("ALTER TABLE \"students\" ADD COLUMN IF NOT EXISTS \"value_"
            + logicalType + "\" " + expectedSqlType);
  }

  @Test
  void addColumnRejectsInvalidInputs() {
    assertThrows(IllegalArgumentException.class, () -> service.addColumn(null, "score", "number"));
    assertThrows(IllegalArgumentException.class, () -> service.addColumn("1students", "score", "number"));
    assertThrows(IllegalArgumentException.class, () -> service.addColumn("Students", null, "number"));
    assertThrows(IllegalArgumentException.class, () -> service.addColumn("Students", "1score", "number"));
    assertThrows(IllegalArgumentException.class, () -> service.addColumn("Students", "score", "json"));
  }

  @Test
  void addColumnRejectsMissingPhysicalTable() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.addColumn("Students", "score", "number"));

    assertThat(exception).hasMessage("Physical table does not exist for entity: Students");
  }

  @Test
  void dropColumnCreatesAlterTableStatement() {
    registerStudentsTable();

    service.dropColumn("Students", "score");

    assertThat(jdbc.lastExecutedSql())
        .isEqualTo("ALTER TABLE \"students\" DROP COLUMN IF EXISTS \"score\"");
  }

  @Test
  void dropColumnRejectsInvalidInputs() {
    assertThrows(IllegalArgumentException.class, () -> service.dropColumn(null, "score"));
    assertThrows(IllegalArgumentException.class, () -> service.dropColumn("1students", "score"));
    assertThrows(IllegalArgumentException.class, () -> service.dropColumn("Students", null));
    assertThrows(IllegalArgumentException.class, () -> service.dropColumn("Students", "1score"));
    assertThrows(IllegalArgumentException.class, () -> service.dropColumn("Students", "id"));
  }

  @Test
  void dropColumnRejectsMissingPhysicalTable() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> service.dropColumn("Students", "score"));

    assertThat(exception).hasMessage("Physical table does not exist for entity: Students");
  }

  static Stream<String> invalidSqlStatements() {
    return Stream.of(
        "",
        " ",
        "CREATE TABLE demo(id UUID); DROP TABLE demo",
        "DROP TABLE demo",
        "TRUNCATE demo",
        "DELETE FROM demo",
        "ALTER TABLE demo ADD COLUMN name TEXT",
        "INSERT INTO audit_log(id) VALUES ('1')",
        "CREATE TABLE demo(id UUID)",
        "SELECT * FROM demo"
    );
  }

  static Stream<Arguments> supportedColumnTypes() {
    return Stream.of(
        Arguments.of("string", "VARCHAR(255)"),
        Arguments.of("number", "NUMERIC"),
        Arguments.of("boolean", "BOOLEAN"),
        Arguments.of("date", "DATE")
    );
  }

  private void registerStudentsTable() {
    service.executeStatements(Map.of("Students", CREATE_STUDENTS_TABLE));
  }

  private Map<String, String> singleEntryMap(String key, String value) {
    Map<String, String> map = new LinkedHashMap<>();
    map.put(key, value);
    return map;
  }

  private void assertMessageContainsAnyOf(Exception exception, String... expectedMessages) {
    assertThat(expectedMessages)
        .anySatisfy(expectedMessage -> assertThat(exception.getMessage()).contains(expectedMessage));
  }
}
