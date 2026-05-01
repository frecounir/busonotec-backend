package com.tfm.busonotec_backend.service.ai;

import com.tfm.busonotec_backend.domain.EntityModel;
import com.tfm.busonotec_backend.domain.FieldModel;
import com.tfm.busonotec_backend.domain.RelationshipModel;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/*
 Simple stub AI service for development. Replace with OpenAI/Spring AI integration.
*/
@Service
public class MockAIService implements AIService {
  @Override
  public List<EntityModel> inferModel(String prompt) {
    // TODO: replace with real AI integration that returns parsed entities.
    EntityModel user = new EntityModel("user",
        Arrays.asList(
            new FieldModel("id", "INTEGER"),
            new FieldModel("username", "STRING"),
            new FieldModel("email", "STRING"),
            new FieldModel("bio", "TEXT")
        ),
        null);

    EntityModel post = new EntityModel("post",
        Arrays.asList(
            new FieldModel("id", "INTEGER"),
            new FieldModel("user_id", "INTEGER"),
            new FieldModel("title", "STRING"),
            new FieldModel("content", "TEXT"),
            new FieldModel("created_at", "TIMESTAMP")
        ),
        Arrays.asList(new RelationshipModel("post", "user", "MANY_TO_ONE"))
    );

    return Arrays.asList(user, post);
  }
}
