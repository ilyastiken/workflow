package ma.xproce.workflow.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz

                        // ====================================
                        // ENDPOINTS PUBLICS (pas de token nécessaire)
                        // ====================================
                        .requestMatchers("/api/workflows", "/api/instances").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/*/test").permitAll()  // Tous les endpoints /test
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // ====================================
                        // MONITORING - Accès ADMIN uniquement
                        // ====================================
                        .requestMatchers("/api/monitoring/**").hasRole("ADMIN")

                        // ====================================
                        // GESTION DES WORKFLOWS - ADMIN et WORKFLOW_MANAGER
                        // ====================================
                        .requestMatchers("/api/workflows/**").hasAnyRole("ADMIN", "WORKFLOW_MANAGER")
                        .requestMatchers("/api/bpmn/**").hasAnyRole("ADMIN", "WORKFLOW_MANAGER")

                        // ====================================
                        // GESTION DES STATUTS ET TRANSITIONS - ADMIN et WORKFLOW_MANAGER
                        // ====================================
                        .requestMatchers("/api/workflows/*/statuts/**").hasAnyRole("ADMIN", "WORKFLOW_MANAGER")
                        .requestMatchers("/api/workflows/*/transitions/**").hasAnyRole("ADMIN", "WORKFLOW_MANAGER")
                        .requestMatchers("/api/workflows/*/passerelles/**").hasAnyRole("ADMIN", "WORKFLOW_MANAGER")

                        // ====================================
                        // GESTION DES INSTANCES - Différents niveaux d'accès
                        // ====================================
                        .requestMatchers("GET", "/api/instances/**").hasAnyRole("ADMIN", "WORKFLOW_MANAGER", "USER")
                        .requestMatchers("POST", "/api/instances").hasAnyRole("ADMIN", "WORKFLOW_MANAGER", "USER")
                        .requestMatchers("POST", "/api/instances/*/advance").hasAnyRole("ADMIN", "WORKFLOW_MANAGER", "TASK_ASSIGNEE")

                        // ====================================
                        // GESTION DES TÂCHES - TASK_ASSIGNEE et plus
                        // ====================================
                        .requestMatchers("/api/tasks/my-tasks").hasAnyRole("ADMIN", "WORKFLOW_MANAGER", "TASK_ASSIGNEE", "USER")
                        .requestMatchers("/api/tasks/*/complete-and-advance").hasAnyRole("ADMIN", "WORKFLOW_MANAGER", "TASK_ASSIGNEE")
                        .requestMatchers("/api/tasks/all").hasAnyRole("ADMIN", "WORKFLOW_MANAGER")  // Toutes les tâches = Admin seulement

                        // ====================================
                        // MOTEUR DE WORKFLOW - Accès contrôlé
                        // ====================================
                        .requestMatchers("GET", "/api/workflow-engine/**").hasAnyRole("ADMIN", "WORKFLOW_MANAGER", "USER")
                        .requestMatchers("POST", "/api/workflow-engine/**").hasAnyRole("ADMIN", "WORKFLOW_MANAGER")

                        // ====================================
                        // CONDITIONS - ADMIN et WORKFLOW_MANAGER
                        // ====================================
                        .requestMatchers("/api/conditions/**").hasAnyRole("ADMIN", "WORKFLOW_MANAGER")

                        // ====================================
                        // MOTEUR DE TRANSITION - Accès contrôlé
                        // ====================================
                        .requestMatchers("/api/instances/*/transitions/**").hasAnyRole("ADMIN", "WORKFLOW_MANAGER", "TASK_ASSIGNEE")

                        // ====================================
                        // TOUS LES AUTRES ENDPOINTS nécessitent une authentification
                        // ====================================
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            // Récupère les rôles Keycloak
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                Collection<String> roles = (Collection<String>) realmAccess.get("roles");
                return roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                        .collect(Collectors.toList());
            }
            return List.of();
        });

        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}