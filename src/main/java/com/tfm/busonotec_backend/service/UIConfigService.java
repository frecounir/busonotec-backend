package com.tfm.busonotec_backend.service;

import com.tfm.busonotec_backend.domain.EntityModel;
import com.tfm.busonotec_backend.domain.FieldModel;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/*
 Generate a simple server-driven UI JSON config for entities.
*/
@Service
public class UIConfigService {
  private final ObjectMapper mapper = new ObjectMapper();

  public ObjectNode buildUiConfig(List<EntityModel> entities) {
    ObjectNode root = mapper.createObjectNode();
    ArrayNode screens = mapper.createArrayNode();
    for (EntityModel e : entities) {
      ObjectNode screen = mapper.createObjectNode();
      screen.put("entity", e.getName());
      ArrayNode fields = mapper.createArrayNode();
      for (FieldModel f : e.getFields()) {
        ObjectNode field = mapper.createObjectNode();
        field.put("name", f.getName());
        field.put("type", f.getType());
        fields.add(field);
      }
      screen.set("fields", fields);
      screens.add(screen);
    }
    root.set("screens", screens);
    return root;
  }
}
