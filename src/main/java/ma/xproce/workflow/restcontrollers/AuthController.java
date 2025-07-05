package ma.xproce.workflow.restcontrollers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    /**
     * ✅ ENDPOINT DE SANTÉ - PUBLIC
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "🚀 API d'authentification workflow opérationnelle !");
        response.put("timestamp", System.currentTimeMillis());
        response.put("keycloak", "Intégré");
        response.put("security", "Activée");
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ INFORMATIONS UTILISATEUR CONNECTÉ
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) auth.getPrincipal();

                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("username", jwt.getClaimAsString("preferred_username"));
                userInfo.put("email", jwt.getClaimAsString("email"));
                userInfo.put("firstName", jwt.getClaimAsString("given_name"));
                userInfo.put("lastName", jwt.getClaimAsString("family_name"));
                userInfo.put("subject", jwt.getSubject());

                // Extraire les rôles
                Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                if (realmAccess != null && realmAccess.containsKey("roles")) {
                    userInfo.put("roles", realmAccess.get("roles"));
                } else {
                    userInfo.put("roles", List.of());
                }

                userInfo.put("authenticated", true);
                return ResponseEntity.ok(userInfo);
            }

            return ResponseEntity.status(401)
                    .body(Map.of("error", "Utilisateur non authentifié", "authenticated", false));

        } catch (Exception e) {
            logger.error("Erreur dans getCurrentUser", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erreur serveur: " + e.getMessage()));
        }
    }

    /**
     * ✅ DEBUG TOKEN JWT
     */
    @GetMapping("/token-info")
    public ResponseEntity<Map<String, Object>> getTokenInfo() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) auth.getPrincipal();

                Map<String, Object> tokenInfo = new HashMap<>();
                tokenInfo.put("subject", jwt.getSubject());
                tokenInfo.put("username", jwt.getClaimAsString("preferred_username"));
                tokenInfo.put("email", jwt.getClaimAsString("email"));
                tokenInfo.put("firstName", jwt.getClaimAsString("given_name"));
                tokenInfo.put("lastName", jwt.getClaimAsString("family_name"));
                tokenInfo.put("roles", jwt.getClaimAsMap("realm_access"));
                tokenInfo.put("issuedAt", jwt.getIssuedAt());
                tokenInfo.put("expiresAt", jwt.getExpiresAt());
                tokenInfo.put("audience", jwt.getAudience());
                tokenInfo.put("issuer", jwt.getIssuer());
                tokenInfo.put("allClaims", jwt.getClaims());

                return ResponseEntity.ok(tokenInfo);
            }

            return ResponseEntity.status(401)
                    .body(Map.of("error", "Token JWT non trouvé"));
        } catch (Exception e) {
            logger.error("Erreur dans getTokenInfo", e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erreur: " + e.getMessage()));
        }
    }

    /**
     * ✅ VÉRIFICATION DE RÔLE
     */
    @GetMapping("/has-role/{roleName}")
    public ResponseEntity<Map<String, Object>> hasRole(@PathVariable String roleName) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean hasRole = false;
            String currentUser = "non authentifié";

            if (auth != null && auth.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) auth.getPrincipal();
                currentUser = jwt.getClaimAsString("preferred_username");

                // Vérifier le rôle
                hasRole = auth.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + roleName.toUpperCase()));
            }

            return ResponseEntity.ok(Map.of(
                    "role", roleName,
                    "hasRole", hasRole,
                    "user", currentUser
            ));
        } catch (Exception e) {
            logger.error("Erreur dans hasRole", e);
            return ResponseEntity.ok(Map.of(
                    "role", roleName,
                    "hasRole", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * ✅ LISTE DES RÔLES
     */
    @GetMapping("/roles")
    public ResponseEntity<Map<String, Object>> getCurrentUserRoles() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) auth.getPrincipal();
                String currentUser = jwt.getClaimAsString("preferred_username");

                List<String> roles = auth.getAuthorities().stream()
                        .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                        .toList();

                return ResponseEntity.ok(Map.of(
                        "roles", roles,
                        "user", currentUser,
                        "count", roles.size()
                ));
            }

            return ResponseEntity.status(401)
                    .body(Map.of("error", "Non authentifié"));
        } catch (Exception e) {
            logger.error("Erreur dans getCurrentUserRoles", e);
            return ResponseEntity.ok(Map.of(
                    "roles", List.of(),
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * ✅ TEST ACCÈS ADMIN
     */
    @GetMapping("/admin/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testAdminAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = "unknown";

        if (auth != null && auth.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) auth.getPrincipal();
            currentUser = jwt.getClaimAsString("preferred_username");
        }

        return ResponseEntity.ok(Map.of(
                "message", "🎉 Accès ADMIN confirmé !",
                "user", currentUser,
                "timestamp", System.currentTimeMillis(),
                "accessLevel", "ADMIN"
        ));
    }

    /**
     * ✅ TEST ACCÈS WORKFLOW MANAGER
     */
    @GetMapping("/workflow-manager/test")
    @PreAuthorize("hasRole('WORKFLOW_MANAGER')")
    public ResponseEntity<Map<String, Object>> testWorkflowManagerAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = "unknown";

        if (auth != null && auth.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) auth.getPrincipal();
            currentUser = jwt.getClaimAsString("preferred_username");
        }

        return ResponseEntity.ok(Map.of(
                "message", "✅ Accès WORKFLOW_MANAGER confirmé !",
                "user", currentUser,
                "timestamp", System.currentTimeMillis(),
                "accessLevel", "WORKFLOW_MANAGER"
        ));
    }

    /**
     * ✅ TEST ACCÈS TASK_ASSIGNEE
     */
    @GetMapping("/task-assignee/test")
    @PreAuthorize("hasRole('TASK_ASSIGNEE')")
    public ResponseEntity<Map<String, Object>> testTaskAssigneeAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUser = "unknown";

        if (auth != null && auth.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) auth.getPrincipal();
            currentUser = jwt.getClaimAsString("preferred_username");
        }

        return ResponseEntity.ok(Map.of(
                "message", "👤 Accès TASK_ASSIGNEE confirmé !",
                "user", currentUser,
                "timestamp", System.currentTimeMillis(),
                "accessLevel", "TASK_ASSIGNEE"
        ));
    }

    /**
     * ✅ LOGOUT INFO
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout() {
        return ResponseEntity.ok(Map.of(
                "message", "Logout successful",
                "keycloakLogoutUrl", "http://localhost:8080/realms/workflow-realm/protocol/openid-connect/logout?redirect_uri=http://localhost:8081",
                "instructions", "Supprimez le token côté client et redirigez vers keycloakLogoutUrl"
        ));
    }
}