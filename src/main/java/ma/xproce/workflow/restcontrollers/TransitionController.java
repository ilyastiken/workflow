package ma.xproce.workflow.restcontrollers;

import ma.xproce.workflow.dtos.ConditionDTO;
import ma.xproce.workflow.dtos.TransitionDTO;
import ma.xproce.workflow.entities.Condition;
import ma.xproce.workflow.entities.Transition;
import ma.xproce.workflow.service.ConditionService;
import ma.xproce.workflow.service.TransitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflows/{workflowId}/transitions")
@CrossOrigin("*")
public class TransitionController {

    @Autowired
    private TransitionService transitionService;
    @Autowired
    private ConditionService conditionService;

    @PostMapping
    public ResponseEntity<List<Transition>> addTransitions(
            @PathVariable Long workflowId,
            @RequestBody List<TransitionDTO> transitionDTOs) {
        return ResponseEntity.ok(transitionService.addTransitionsToWorkflow(workflowId, transitionDTOs));
    }
    @PostMapping("/{transitionId}/conditions")
    public ResponseEntity<List<Condition>> addConditionsToTransition(
            @PathVariable Long transitionId,
            @RequestBody List<ConditionDTO> conditionDTOs) {
        return ResponseEntity.ok(conditionService.addConditionsToTransition(transitionId, conditionDTOs));
    }
}