package ma.xproce.workflow.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationPriority priority;

    @Column(nullable = false)
    private String recipientEmail;

    private String recipientName;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Builder.Default
    private boolean isRead = false;

    private LocalDateTime readAt;

    @Builder.Default
    private boolean sendEmail = true;

    @Builder.Default
    private boolean sendInApp = true;

    private boolean sendSms = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime sentAt;

    private LocalDateTime scheduledFor; // Pour notifications programmées

    private String createdBy;

    private String actionUrl;

    private String actionLabel;

    @ManyToOne
    private Instance instance;

    @ManyToOne
    private Task task;

    @ManyToOne
    private Workflow workflow;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Destinataire si c'est un utilisateur système

    // === GESTION DES ERREURS ===
    private Integer retryCount = 0;

    @Column(columnDefinition = "TEXT")
    private String errorMessage; // En cas d'échec d'envoi

    private LocalDateTime lastRetryAt;

    // === ÉNUMÉRATIONS ===

    public enum NotificationType {
        TASK_ASSIGNED("Nouvelle tâche assignée"),
        TASK_OVERDUE("Tâche en retard"),
        TASK_COMPLETED("Tâche terminée"),
        WORKFLOW_STARTED("Workflow démarré"),
        WORKFLOW_COMPLETED("Workflow terminé"),
        WORKFLOW_ERROR("Erreur workflow"),
        INSTANCE_STUCK("Instance bloquée"),
        DEADLINE_APPROACHING("Échéance proche"),
        APPROVAL_REQUIRED("Approbation requise"),
        SYSTEM_ALERT("Alerte système"),
        REMINDER("Rappel");

        private final String description;

        NotificationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum NotificationPriority {
        LOW("Faible"),
        NORMAL("Normal"),
        HIGH("Élevée"),
        URGENT("Urgent");

        private final String description;

        NotificationPriority(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum NotificationStatus {
        PENDING("En attente"),
        SENT("Envoyée"),
        DELIVERED("Distribuée"),
        READ("Lue"),
        FAILED("Échec"),
        CANCELLED("Annulée");

        private final String description;

        NotificationStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
    public void markAsRead() {
        this.isRead = true;
        this.readAt = LocalDateTime.now();
        if (this.status == NotificationStatus.DELIVERED || this.status == NotificationStatus.SENT) {
            this.status = NotificationStatus.READ;
        }
    }
    public void markAsSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
    }
    public void markAsFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.retryCount++;
        this.lastRetryAt = LocalDateTime.now();
    }
    public boolean canRetry() {
        return this.retryCount < 3 && this.status == NotificationStatus.FAILED;
    }
    public boolean isScheduled() {
        return this.scheduledFor != null && this.scheduledFor.isAfter(LocalDateTime.now());
    }
}