package ma.xproce.workflow.restcontrollers;

import ma.xproce.workflow.entities.Instance;
import ma.xproce.workflow.entities.Transition;
import ma.xproce.workflow.service.WorkflowEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workflow-engine")
@CrossOrigin("*")
public class WorkflowEngineController {

    @Autowired
    private WorkflowEngineService workflowEngineService;

    // Récupérer toutes les instances
    @GetMapping
    public List<Instance> getAllInstances() {
        return workflowEngineService.getAllInstances();
    }

    // Récupérer une instance par ID
    @GetMapping("/{id}")
    public ResponseEntity<Instance> getInstanceById(@PathVariable Long id) {
        Instance instance = workflowEngineService.getInstanceById(id);
        if (instance != null) {
            return ResponseEntity.ok(instance);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // Démarrer une nouvelle instance
    @PostMapping("/start")
    public ResponseEntity<Instance> startWorkflow(@RequestBody Map<String, Object> request) {
        Long workflowId = Long.valueOf(request.get("workflowId").toString());
        String businessKey = (String) request.get("businessKey");
        String createdBy = (String) request.get("createdBy");

        Instance instance = workflowEngineService.startWorkflow(workflowId, businessKey, createdBy);

        if (instance != null) {
            return ResponseEntity.ok(instance);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    // Récupérer les transitions disponibles pour une instance
    @GetMapping("/{id}/transitions")
    public List<Transition> getAvailableTransitions(@PathVariable Long id) {
        return workflowEngineService.getAvailableTransitions(id);
    }

    // Effectuer une transition
    @PostMapping("/{id}/transition")
    public ResponseEntity<Instance> performTransition(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        Long transitionId = Long.valueOf(request.get("transitionId").toString());
        String executedBy = (String) request.get("executedBy");

        Instance instance = workflowEngineService.performTransition(id, transitionId, executedBy);

        if (instance != null) {
            return ResponseEntity.ok(instance);
        } else {
            return ResponseEntity.badRequest().build();
        }
    }
}