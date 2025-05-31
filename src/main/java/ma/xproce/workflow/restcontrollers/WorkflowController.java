package ma.xproce.workflow.restcontrollers;

import ma.xproce.workflow.entities.Workflow;
import ma.xproce.workflow.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
@CrossOrigin("*")
public class WorkflowController {

    @Autowired
    private WorkflowService workflowService;

    @GetMapping
    public List<Workflow> getAllWorkflows() {
        return workflowService.getAllWorkflows();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Workflow> getWorkflowById(@PathVariable Long id) {
        Workflow workflow = workflowService.getWorkflowById(id);
        if (workflow != null) {
            return ResponseEntity.ok(workflow);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public Workflow createWorkflow(@RequestBody Workflow workflow) {
        return workflowService.saveWorkflow(workflow);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Workflow> updateWorkflow(@PathVariable Long id, @RequestBody Workflow workflow) {
        Workflow existingWorkflow = workflowService.getWorkflowById(id);
        if (existingWorkflow != null) {
            workflow.setId(id);
            return ResponseEntity.ok(workflowService.saveWorkflow(workflow));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}