package ma.xproce.workflow.restcontrollers;

import ma.xproce.workflow.entities.Instance;
import ma.xproce.workflow.entities.Transition;
import ma.xproce.workflow.service.TransitionEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/instances/{instanceId}/transitions")
@CrossOrigin("*")
public class TransitionEngineController {

    @Autowired
    private TransitionEngineService transitionEngineService;

    @PostMapping("/{transitionId}/execute")
    public ResponseEntity<Instance> executeTransition(
            @PathVariable Long instanceId,
            @PathVariable Long transitionId,
            @RequestBody String executedBy) {

       // String executedBy = payload.get("executedBy");
        Instance instance = transitionEngineService.executeTransition(instanceId, transitionId, executedBy);
        return ResponseEntity.ok(instance);
    }

    @GetMapping("/available")
    public ResponseEntity<List<Transition>> getAvailableTransitions(@PathVariable Long instanceId) {
        List<Transition> transitions = transitionEngineService.getAvailableTransitions(instanceId);
        return ResponseEntity.ok(transitions);
    }
}