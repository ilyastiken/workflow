package ma.xproce.workflow.service;

import jakarta.transaction.Transactional;
import ma.xproce.workflow.entities.Workflow;
import ma.xproce.workflow.entities.Statut;
import ma.xproce.workflow.entities.Transition;
import ma.xproce.workflow.repositories.StatutRepository;
import ma.xproce.workflow.repositories.WorkflowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
public class WorkflowService {

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private StatutRepository statutRepository;

    public List<Workflow> getAllWorkflows() {
        return workflowRepository.findAll();
    }

    public Workflow getWorkflowById(Long id) {
        return workflowRepository.findById(id).orElse(null);
    }

    @Transactional
    public Workflow saveWorkflow(Workflow workflow) {
        if (workflow.getId() == null) {
            workflow.setCreationDate(LocalDateTime.now());
        }
        workflow.setModificationDate(LocalDateTime.now());

        if (workflow.getStatuts() != null && !workflow.getStatuts().isEmpty()) {
            List<Statut> managedStatuts = new ArrayList<>();

            for (Statut statut : workflow.getStatuts()) {
                if (statut.getId() != null) {
                    Statut existingStatut = statutRepository.findById(statut.getId())
                            .orElseThrow(() -> new RuntimeException("Statut introuvable avec l'ID : " + statut.getId()));

                    existingStatut.setWorkflow(workflow);
                    managedStatuts.add(existingStatut);
                } else {
                    statut.setWorkflow(workflow);
                    managedStatuts.add(statut);
                }
            }
            workflow.setStatuts(managedStatuts);
        }

        if (workflow.getTransitions() != null && !workflow.getTransitions().isEmpty()) {
            List<Transition> managedTransitions = new ArrayList<>();

            for (Transition transition : workflow.getTransitions()) {
                transition.setWorkflow(workflow);

                if (transition.getSourceStatut() != null) {
                    Long sourceStatutId = transition.getSourceStatut().getId();
                    if (sourceStatutId != null) {
                        Statut sourceStatut = statutRepository.findById(sourceStatutId)
                                .orElseThrow(() -> new RuntimeException("Statut source introuvable avec l'ID : " + sourceStatutId));
                        transition.setSourceStatut(sourceStatut);
                    } else {
                        throw new RuntimeException("Le statut source doit avoir un ID");
                    }
                }


                if (transition.getTargetStatut() != null) {
                    Long targetStatutId = transition.getTargetStatut().getId();
                    if (targetStatutId != null) {
                        Statut targetStatut = statutRepository.findById(targetStatutId)
                                .orElseThrow(() -> new RuntimeException("Statut cible introuvable avec l'ID : " + targetStatutId));
                        transition.setTargetStatut(targetStatut);
                    } else {
                        throw new RuntimeException("Le statut cible doit avoir un ID");
                    }
                }

                managedTransitions.add(transition);
            }

            workflow.setTransitions(managedTransitions);
        }

        return workflowRepository.save(workflow);
    }


}