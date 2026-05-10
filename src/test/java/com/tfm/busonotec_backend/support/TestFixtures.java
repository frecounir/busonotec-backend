package com.tfm.busonotec_backend.support;

import com.tfm.busonotec_backend.domain.BusinessEntity;
import com.tfm.busonotec_backend.domain.EntityField;
import com.tfm.busonotec_backend.dto.BusinessEntityRequest;
import com.tfm.busonotec_backend.dto.EntityFieldRequest;

import java.util.UUID;

public final class TestFixtures {
  public static final String STUDENTS = "Students";
  public static final String STUDENT_DESCRIPTION = "Student records";

  private TestFixtures() {
  }

  public static BusinessEntity businessEntity(UUID id, String name, String description) {
    return new BusinessEntity(id, name, description);
  }

  public static BusinessEntity studentsEntity(UUID id) {
    return businessEntity(id, STUDENTS, STUDENT_DESCRIPTION);
  }

  public static BusinessEntityRequest businessEntityRequest(String name, String description) {
    return new BusinessEntityRequest(name, description);
  }

  public static EntityField entityField(UUID id, UUID entityId, String name, String type) {
    return new EntityField(id, name, type, entityId, null);
  }

  public static EntityFieldRequest entityFieldRequest(UUID entityId, String name, String type) {
    return new EntityFieldRequest(entityId, name, type);
  }

  public static String uniqueEntityName(String prefix) {
    return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
  }
}
