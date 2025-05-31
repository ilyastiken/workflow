package ma.xproce.workflow.restcontrollers;

import ma.xproce.workflow.entities.Workflow;
import ma.xproce.workflow.service.BpmnGeneratorService;
import ma.xproce.workflow.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bpmn")
@CrossOrigin("*")
public class BpmnController {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private BpmnGeneratorService bpmnGeneratorService; // Injection du service

    @PostMapping("/generate")
    @CrossOrigin(origins = "http://localhost:3000")
    public ResponseEntity<String> generateBpmn(@RequestBody Workflow workflow) {
        try {
            Workflow savedWorkflow = workflowService.saveWorkflow(workflow);

            // Utilisation correcte du service injecté
            String bpmnXml = bpmnGeneratorService.generateBpmn(savedWorkflow);
            System.out.println("BPMN généré : \n" + bpmnXml);

            return ResponseEntity.ok().body(bpmnXml);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Erreur lors de la génération BPMN: " + e.getMessage());
        }
    }

    @GetMapping("/generate/{workflowId}") // Correction du mapping
    public ResponseEntity<String> generateBpmnById(@PathVariable Long workflowId) {
        try {
            Workflow workflow = workflowService.getWorkflowById(workflowId);
            String bpmnXml = bpmnGeneratorService.generateBpmn(workflow);
            return ResponseEntity.ok(bpmnXml);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Workflow non trouvé: " + e.getMessage());
        }
    }
}