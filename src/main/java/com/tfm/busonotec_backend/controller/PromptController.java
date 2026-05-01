package com.tfm.busonotec_backend.controller;

import com.tfm.busonotec_backend.domain.EntityModel;
import com.tfm.busonotec_backend.dto.PromptRequest;
import com.tfm.busonotec_backend.service.ModelValidator;
import com.tfm.busonotec_backend.service.ai.AIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/prompts")
public class PromptController {

  private final AIService ai;
  private final ModelValidator validator;

  public PromptController(AIService ai, ModelValidator validator) {
    this.ai = ai;
    this.validator = validator;
  }

  @PostMapping
  @Operation(summary = "Handle AI prompt", description = "Infer entity model from a text prompt using AI")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Inferred entity model list"),
      @ApiResponse(responseCode = "400", description = "Invalid request"),
      @ApiResponse(responseCode = "500", description = "Server error")
  })
  public ResponseEntity<List<EntityModel>> handlePrompt(@RequestBody PromptRequest req) throws Exception {
    List<EntityModel> model = ai.inferModel(req.getPrompt());
    // validate AI output
    validator.validateEntities(model);
    return ResponseEntity.ok(model);
  }
}
