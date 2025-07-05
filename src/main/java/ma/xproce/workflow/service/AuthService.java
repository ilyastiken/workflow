package ma.xproce.workflow.service;

import ma.xproce.workflow.entities.User;
import ma.xproce.workflow.entities.Role;
import ma.xproce.workflow.entities.UserRole;
import ma.xproce.workflow.repositories.UserRepository;
import ma.xproce.workflow.repositories.RoleRepository;
import ma.xproce.workflow.repositories.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    /**
     * Récupère l'utilisateur actuellement connecté
     */
    public User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) auth.getPrincipal();
                String username = jwt.getClaimAsString("preferred_username");
                String email = jwt.getClaimAsString("email");

                return syncUserWithKeycloak(username, email, jwt);
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de l'utilisateur actuel", e);
        }
        return null;
    }

    /**
     * Synchronise l'utilisateur Keycloak avec la base de données locale
     */
    public User syncUserWithKeycloak(String username, String email, Jwt jwt) {
        try {
            Optional<User> existingUser = userRepository.findByUsername(username);

            User user;
            if (existingUser.isPresent()) {
                user = existingUser.get();
                // Met à jour les informations si nécessaires
                user.setEmail(email);
                user.setFirstName(jwt.getClaimAsString("given_name"));
                user.setLastName(jwt.getClaimAsString("family_name"));
            } else {
                // Crée un nouvel utilisateur
                user = User.builder()
                        .username(username)
                        .email(email)
                        .firstName(jwt.getClaimAsString("given_name"))
                        .lastName(jwt.getClaimAsString("family_name"))
                        .isActive(true)
                        .build();
            }

            user = userRepository.save(user);

            // Synchronise les rôles
            syncUserRoles(user, jwt);

            return user;
        } catch (Exception e) {
            logger.error("Erreur lors de la synchronisation avec Keycloak", e);
            return null;
        }
    }

    /**
     * Synchronise les rôles entre Keycloak et la base locale
     */
    private void syncUserRoles(User user, Jwt jwt) {
        try {
            // Récupère les rôles depuis Keycloak
            List<String> keycloakRoles = extractKeycloakRoles(jwt);

            // Supprime les anciens rôles
            userRoleRepository.deleteByUser(user);

            // Ajoute les nouveaux rôles
            for (String roleName : keycloakRoles) {
                Optional<Role> role = roleRepository.findByName(roleName);
                if (role.isPresent()) {
                    UserRole userRole = UserRole.builder()
                            .user(user)
                            .role(role.get())
                            .build();
                    userRoleRepository.save(userRole);
                } else {
                    logger.warn("Rôle '{}' non trouvé dans la base locale", roleName);
                }
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la synchronisation des rôles", e);
        }
    }

    /**
     * Extrait les rôles depuis le JWT Keycloak
     */
    private List<String> extractKeycloakRoles(Jwt jwt) {
        try {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                return (List<String>) realmAccess.get("roles");
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'extraction des rôles", e);
        }
        return List.of();
    }

    /**
     * Vérifie si l'utilisateur actuel a un rôle spécifique
     */
    public boolean hasRole(String roleName) {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) return false;

            return currentUser.getUserRoles().stream()
                    .anyMatch(userRole -> userRole.getRole().getName().equals(roleName));
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification du rôle", e);
            return false;
        }
    }

    /**
     * Récupère tous les rôles de l'utilisateur actuel
     */
    public List<String> getCurrentUserRoles() {
        try {
            User currentUser = getCurrentUser();
            if (currentUser == null) return List.of();

            return currentUser.getUserRoles().stream()
                    .map(userRole -> userRole.getRole().getName())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des rôles", e);
            return List.of();
        }
    }

    /**
     * Récupère le nom d'utilisateur actuel
     */
    public String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) auth.getPrincipal();
                return jwt.getClaimAsString("preferred_username");
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du nom d'utilisateur", e);
        }
        return "SYSTEM";  // Valeur par défaut pour éviter les erreurs
    }

    /**
     * Méthode utilitaire pour tes contrôleurs existants
     * Remplace les "current_user" par le vrai utilisateur connecté
     */
    public String getCurrentUserForTasks() {
        String username = getCurrentUsername();
        return username != null ? username : "current_user";
    }

    /**
     * Vérifie si l'utilisateur peut gérer les workflows
     */
    public boolean canManageWorkflows() {
        return hasRole("ADMIN") || hasRole("WORKFLOW_MANAGER");
    }

    /**
     * Vérifie si l'utilisateur peut gérer les tâches
     */
    public boolean canManageTasks() {
        return hasRole("ADMIN") || hasRole("WORKFLOW_MANAGER") || hasRole("TASK_ASSIGNEE");
    }

    /**
     * Vérifie si l'utilisateur est administrateur
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }
}