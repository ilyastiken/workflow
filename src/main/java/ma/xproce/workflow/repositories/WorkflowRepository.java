package ma.xproce.workflow.repositories;

import ma.xproce.workflow.entities.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowRepository extends JpaRepository<Workflow,Long> {
    List<Workflow> findByIsActiveTrue();
}
