package ma.xproce.workflow.security;

import ma.xproce.workflow.entities.Role;
import ma.xproce.workflow.repositories.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        logger.info("🚀 Initialisation des données pour le moteur de workflow...");

        try {
            initializeRoles();
            logger.info("✅ Initialisation des rôles terminée avec succès !");
        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'initialisation des données", e);
        }
    }

    private void initializeRoles() {
        // Rôles pour ton système de workflow
        createRoleIfNotExists("ADMIN",
                "Administrateur système - Accès complet à toutes les fonctionnalités du workflow");

        createRoleIfNotExists("WORKFLOW_MANAGER",
                "Gestionnaire de workflows - Peut créer, modifier et gérer les workflows et leurs composants");

        createRoleIfNotExists("TASK_ASSIGNEE",
                "Assigné de tâches - Peut exécuter les tâches qui lui sont assignées et faire avancer les instances");

        createRoleIfNotExists("USER",
                "Utilisateur standard - Peut consulter les workflows et créer des instances");

        createRoleIfNotExists("VIEWER",
                "Observateur - Accès en lecture seule pour consulter l'état des workflows et instances");

        // Rôles spécialisés pour ton domaine métier (si nécessaire)
        createRoleIfNotExists("WORKFLOW_DESIGNER",
                "Concepteur de workflows - Spécialisé dans la création et modification des processus");

        createRoleIfNotExists("PROCESS_ANALYST",
                "Analyste de processus - Accès aux rapports et analyses des workflows");
    }

    private void createRoleIfNotExists(String roleName, String description) {
        try {
            if (!roleRepository.existsByName(roleName)) {
                Role role = Role.builder()
                        .name(roleName)
                        .description(description)
                        .build();
                roleRepository.save(role);
                logger.info("✅ Rôle créé: {} - {}", roleName, description);
            } else {
                logger.debug("ℹ️ Rôle déjà existant: {}", roleName);
            }
        } catch (Exception e) {
            logger.error("❌ Erreur lors de la création du rôle {}: {}", roleName, e.getMessage());
        }
    }
}