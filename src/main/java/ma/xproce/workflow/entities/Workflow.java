package ma.xproce.workflow.entities;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Workflow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private String version;
    private boolean isActive;
    private String createdBy;
    private LocalDateTime creationDate;
    private LocalDateTime modificationDate;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String bpmn;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("workflow-statuts")
    @Builder.Default
    private List<Statut> statuts = new ArrayList<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("workflow-transitions")
    @Builder.Default
    private List<Transition> transitions = new ArrayList<>();

    @OneToMany(mappedBy = "workflow")
    @JsonIgnore
    private List<Instance> instances = new ArrayList<>();

    @Getter
    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Passerelle> passerelles;

}