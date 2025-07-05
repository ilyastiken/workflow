package ma.xproce.workflow.service;

import ma.xproce.workflow.entities.*;
import ma.xproce.workflow.repositories.WorkflowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BpmnGeneratorService {
    @Autowired
    private WorkflowRepository workflowRepository;

    private Map<String, ElementPosition> elementPositions = new HashMap<>();

    private List<FlowInfo> generatedFlows = new ArrayList<>();

    // ✅ NOUVEAU : Classe interne pour stocker les informations de flux
    private static class FlowInfo {
        String id;
        String sourceRef;
        String targetRef;

        FlowInfo(String id, String sourceRef, String targetRef) {
            this.id = id;
            this.sourceRef = sourceRef;
            this.targetRef = targetRef;
        }
    }

    public String generateBpmn(Workflow workflow) {
        if (workflow == null) {
            throw new IllegalArgumentException("Le workflow ne peut pas être null.");
        }


        elementPositions.clear();

        String processName = workflow.getName();
        String processId = "Process_" + processName.replace(" ", "_");


        // Récupération et tri des statuts
        List<Statut> statuts = workflow.getStatuts();
        if (statuts == null) {
            statuts = new ArrayList<>();
        }
        statuts.sort(Comparator.comparing(s -> s.getPosition() != null ? s.getPosition() : 0));

        // Récupération des transitions
        List<Transition> transitions = workflow.getTransitions();
        if (transitions == null) {
            transitions = new ArrayList<>();
        }
        List<Passerelle> passerelles = workflow.getPasserelles();

        // ✅ DEBUG CORRIGÉ
        System.err.println("=== DEBUG GÉNÉRATEUR BPMN ===");
        System.err.println("Workflow ID: " + workflow.getId());
        System.err.println("Nombre de statuts: " + statuts.size());
        System.err.println("Nombre de transitions: " + transitions.size());
        System.err.println("Nombre de passerelles: " + (passerelles != null ? passerelles.size() : "NULL"));

        if (passerelles != null && !passerelles.isEmpty()) {
            for (Passerelle p : passerelles) {
                System.err.println("  - Passerelle: " + p.getName() + " (position: " + p.getPosition() + ")");
            }
        } else {
            System.err.println("❌ AUCUNE PASSERELLE TROUVÉE !");
        }
        System.err.println("=============================");


        calculateElementPositions(statuts,passerelles);

        StringBuilder bpmn = new StringBuilder();

        // En-tête BPMN
        appendBpmnHeader(bpmn);

        // Processus principal
        bpmn.append("  <bpmn:process id=\"").append(processId).append("\" ");
        bpmn.append("name=\"").append(escapeXml(processName)).append("\" isExecutable=\"true\">\n");

        // Événement de début
        bpmn.append("    <bpmn:startEvent id=\"start\" name=\"Start\" />\n");

        // Génération des tâches pour chaque statut (sauf INITIAL et FINAL)
        debugTaskGeneration(bpmn, statuts);


        generatePasserelleElements(bpmn, passerelles);

        // Événement de fin
        bpmn.append("    <bpmn:endEvent id=\"end\" name=\"End\" />\n");

        // Génération des flux de séquence
        bpmn.append(generateSequenceFlows(statuts, transitions, passerelles));

        bpmn.append("  </bpmn:process>\n");

        // Génération du diagramme visuel
        bpmn.append(generateDiagram(processId, statuts, passerelles));

        bpmn.append("</bpmn:definitions>");
        workflow.setBpmn(bpmn.toString());
        workflowRepository.save(workflow);

        return bpmn.toString();
    }

    private void appendBpmnHeader(StringBuilder bpmn) {
        bpmn.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        bpmn.append("<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" ");
        bpmn.append("xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" ");
        bpmn.append("xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" ");
        bpmn.append("xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" ");
        bpmn.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        bpmn.append("id=\"Definitions_1\" ");
        bpmn.append("targetNamespace=\"http://bpmn.io/schema/bpmn\">\n");
    }

    private void generateStatutTasks(StringBuilder bpmn, List<Statut> statuts) {
        for (Statut statut : statuts) {
            if (!"INITIAL".equals(statut.getStatutType()) && !"FINAL".equals(statut.getStatutType())) {
                bpmn.append("    <bpmn:task id=\"task_").append(statut.getId())
                        .append("\" name=\"").append(escapeXml(statut.getName())).append("\" />\n");
            }
        }
    }
    private void generatePasserelleElements(StringBuilder bpmn, List<Passerelle> passerelles) {
        for (Passerelle p : passerelles) {
            bpmn.append("    <bpmn:exclusiveGateway id=\"gateway_").append(p.getId())
                    .append("\" name=\"").append(escapeXml(p.getName())).append("\" />\n");
        }
    }

    private String generateSequenceFlows(List<Statut> statuts, List<Transition> transitions, List<Passerelle> passerelles) {
        StringBuilder flows = new StringBuilder();

        // ✅ NOUVEAU : Vider la liste des flux au début
        generatedFlows.clear();

        if (statuts.isEmpty()) {
            flows.append("    <bpmn:sequenceFlow id=\"flow_0\" sourceRef=\"start\" targetRef=\"end\" />\n");
            // ✅ NOUVEAU : Enregistrer ce flux
            generatedFlows.add(new FlowInfo("flow_0", "start", "end"));
            return flows.toString();
        }

        if (!transitions.isEmpty()) {
            generateTransitionBasedFlows(flows, transitions);
        } else {
            generateDefaultSequentialFlows(flows, statuts, passerelles);
        }

        return flows.toString();
    }

    // 3. Remplacez generateTransitionBasedFlows par cette version :

    private void generateTransitionBasedFlows(StringBuilder flows, List<Transition> transitions) {
        System.err.println("🔍 GÉNÉRATION BASÉE SUR TRANSITIONS");
        System.err.println("Nombre de transitions: " + transitions.size());

        int flowIndex = 0;
        Set<String> createdConnections = new HashSet<>();

        for (Transition transition : transitions) {
            String sourceRef = determineSourceRef(transition);
            String targetRef = determineTargetRef(transition);
            String connectionKey = sourceRef + "->" + targetRef;

            if (!createdConnections.contains(connectionKey)) {
                String flowId = "flow_" + flowIndex++;

                flows.append("    <bpmn:sequenceFlow id=\"").append(flowId)
                        .append("\" sourceRef=\"").append(sourceRef)
                        .append("\" targetRef=\"").append(targetRef);

                // Ajout du nom si présent
                if (transition.getName() != null && !transition.getName().isEmpty()) {
                    flows.append("\" name=\"").append(escapeXml(transition.getName()));
                }

                // Ajout des conditions
                if (hasConditions(transition)) {
                    flows.append("\">\n");
                    generateConditionExpression(flows, transition);
                    flows.append("    </bpmn:sequenceFlow>\n");
                } else {
                    flows.append("\" />\n");
                }

                // ✅ NOUVEAU : Enregistrer le flux créé
                generatedFlows.add(new FlowInfo(flowId, sourceRef, targetRef));
                createdConnections.add(connectionKey);

                // ✅ NOUVEAU : Debug
                System.err.println("  ✅ Flux créé: " + flowId + " (" + sourceRef + " → " + targetRef + ")");
            }
        }
    }

    private String determineSourceRef(Transition transition) {
        if (transition.getSourceStatut() != null) {
            if ("INITIAL".equals(transition.getSourceStatut().getStatutType())) {
                return "start";
            } else {
                return "task_" + transition.getSourceStatut().getId();
            }
        }
        return "start";
    }

    private String determineTargetRef(Transition transition) {
        if (transition.getTargetStatut() != null) {
            if ("FINAL".equals(transition.getTargetStatut().getStatutType())) {
                return "end";
            } else {
                return "task_" + transition.getTargetStatut().getId();
            }
        }
        return "end";
    }

    private boolean hasConditions(Transition transition) {
        return (transition.getConditionExpression() != null && !transition.getConditionExpression().isEmpty()) ||
                (transition.getConditions() != null && !transition.getConditions().isEmpty());
    }

    private void generateConditionExpression(StringBuilder flows, Transition transition) {
        String condition = null;

        // Priorité à conditionExpression (legacy)
        if (transition.getConditionExpression() != null && !transition.getConditionExpression().isEmpty()) {
            condition = transition.getConditionExpression();
        }
        // Sinon, utiliser les conditions liées
        else if (transition.getConditions() != null && !transition.getConditions().isEmpty()) {
            List<String> expressions = transition.getConditions().stream()
                    .filter(c -> c.isActive() && c.getExpression() != null)
                    .map(Condition::getExpression)
                    .toList();

            if (!expressions.isEmpty()) {
                condition = String.join(" AND ", expressions);
            }
        }

        if (condition != null) {
            flows.append("      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">")
                    .append(escapeXml(condition)).append("</bpmn:conditionExpression>\n");
        }
    }

    // ✅ CORRECTION PRINCIPALE: generateDefaultSequentialFlows
    // 4. Modifiez generateDefaultSequentialFlows pour enregistrer les flux :

    private void generateDefaultSequentialFlows(StringBuilder flows, List<Statut> statuts, List<Passerelle> passerelles) {
        List<Statut> normalStatuts = statuts.stream()
                .filter(s -> !"INITIAL".equals(s.getStatutType()) && !"FINAL".equals(s.getStatutType()))
                .sorted(Comparator.comparing(s -> s.getPosition() != null ? s.getPosition() : 0))
                .toList();

        if (normalStatuts.isEmpty()) return;

        Map<Integer, Object> elementsByPosition = new TreeMap<>();

        // Ajouter les statuts normaux
        for (Statut statut : normalStatuts) {
            if (statut.getPosition() != null) {
                elementsByPosition.put(statut.getPosition(), statut);
            }
        }

        // Ajouter les passerelles
        if (passerelles != null) {
            for (Passerelle passerelle : passerelles) {
                if (passerelle.getPosition() != null) {
                    elementsByPosition.put(passerelle.getPosition(), passerelle);
                }
            }
        }

        // Générer les flux dans l'ordre logique des positions
        int flowIndex = 0;
        String currentSourceRef = "start";

        for (Map.Entry<Integer, Object> entry : elementsByPosition.entrySet()) {
            Object element = entry.getValue();
            String targetRef;

            if (element instanceof Statut) {
                Statut statut = (Statut) element;
                targetRef = "task_" + statut.getId();
            } else if (element instanceof Passerelle) {
                Passerelle passerelle = (Passerelle) element;
                targetRef = "gateway_" + passerelle.getId();
            } else {
                continue;
            }

            // Créer le flux
            String flowId = "flow_" + flowIndex++;
            flows.append("    <bpmn:sequenceFlow id=\"").append(flowId)
                    .append("\" sourceRef=\"").append(currentSourceRef)
                    .append("\" targetRef=\"").append(targetRef).append("\" />\n");

            // ✅ NOUVEAU : Enregistrer le flux
            generatedFlows.add(new FlowInfo(flowId, currentSourceRef, targetRef));

            // L'élément actuel devient la source pour le prochain flux
            currentSourceRef = targetRef;
        }

        // Flux final vers END
        String finalFlowId = "flow_" + flowIndex;
        flows.append("    <bpmn:sequenceFlow id=\"").append(finalFlowId)
                .append("\" sourceRef=\"").append(currentSourceRef)
                .append("\" targetRef=\"end\" />\n");

        // ✅ NOUVEAU : Enregistrer le flux final
        generatedFlows.add(new FlowInfo(finalFlowId, currentSourceRef, "end"));
    }

    private String generateDiagram(String processId, List<Statut> statuts,List<Passerelle> passerelles) {
        StringBuilder diagram = new StringBuilder();

        diagram.append("  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\n");
        diagram.append("    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"").append(processId).append("\">\n");

        generateShapes(diagram, statuts, passerelles);
        generateEdges(diagram, statuts, passerelles);

        diagram.append("    </bpmndi:BPMNPlane>\n");
        diagram.append("  </bpmndi:BPMNDiagram>\n");

        return diagram.toString();
    }

    private void generateShapes(StringBuilder diagram, List<Statut> statuts, List<Passerelle> passerelles) {
        System.err.println("\nGÉNÉRATION DES FORMES VISUELLES:");

        // Start event
        ElementPosition startPos = elementPositions.get("start");
        if (startPos != null) {
            System.err.println("   Forme Start créée");
            diagram.append("      <bpmndi:BPMNShape id=\"start_di\" bpmnElement=\"start\">\n");
            diagram.append("        <dc:Bounds x=\"").append(startPos.x).append("\" y=\"")
                    .append(startPos.y).append("\" width=\"36\" height=\"36\" />\n");
            diagram.append("      </bpmndi:BPMNShape>\n");
        } else {
            System.err.println("   Position Start manquante!");
        }

        // ✅ CORRECTION PRINCIPALE : Tasks avec debug détaillé
        System.err.println("  ️ Génération formes des tâches:");
        for (Statut statut : statuts) {
            String taskId = "task_" + statut.getId();

            // ✅ CRÉER LA FORME POUR TOUS LES STATUTS (PAS SEULEMENT NORMAL)
            // Le problème était ici : on ignorait les statuts INITIAL et FINAL
            System.err.println("    Statut " + statut.getId() + " (" + statut.getName() + ") - Type: " + statut.getStatutType());

            ElementPosition pos = elementPositions.get(taskId);
            if (pos != null) {
                System.err.println("       Forme créée pour " + taskId + " à position (" + pos.x + ", " + pos.y + ")");
                diagram.append("      <bpmndi:BPMNShape id=\"").append(taskId).append("_di\" bpmnElement=\"").append(taskId).append("\">\n");
                diagram.append("        <dc:Bounds x=\"").append(pos.x).append("\" y=\"")
                        .append(pos.y).append("\" width=\"100\" height=\"80\" />\n");
                diagram.append("      </bpmndi:BPMNShape>\n");
            } else {
                System.err.println("       Position manquante pour " + taskId + " !");

                // ✅ CRÉER UNE POSITION PAR DÉFAUT SI MANQUANTE
                int defaultX = 300;
                int defaultY = 80;
                System.err.println("       Création position par défaut: (" + defaultX + ", " + defaultY + ")");
                diagram.append("      <bpmndi:BPMNShape id=\"").append(taskId).append("_di\" bpmnElement=\"").append(taskId).append("\">\n");
                diagram.append("        <dc:Bounds x=\"").append(defaultX).append("\" y=\"")
                        .append(defaultY).append("\" width=\"100\" height=\"80\" />\n");
                diagram.append("      </bpmndi:BPMNShape>\n");
            }
        }

        // Passerelles
        if (passerelles != null) {
            System.err.println("   Génération formes des passerelles:");
            for (Passerelle passerelle : passerelles) {
                String gatewayId = "gateway_" + passerelle.getId();
                ElementPosition pos = elementPositions.get(gatewayId);
                System.err.println("     Passerelle " + passerelle.getId() + " (" + passerelle.getName() + ")");

                if (pos != null) {
                    System.err.println("       Forme créée pour " + gatewayId + " à position (" + pos.x + ", " + pos.y + ")");
                    diagram.append("      <bpmndi:BPMNShape id=\"").append(gatewayId).append("_di\" bpmnElement=\"").append(gatewayId).append("\">\n");
                    diagram.append("        <dc:Bounds x=\"").append(pos.x).append("\" y=\"")
                            .append(pos.y).append("\" width=\"").append(pos.width).append("\" height=\"").append(pos.height).append("\" />\n");
                    diagram.append("      </bpmndi:BPMNShape>\n");
                } else {
                    System.err.println("      Position manquante pour " + gatewayId + " !");
                }
            }
        }

        // End event
        ElementPosition endPos = elementPositions.get("end");
        if (endPos != null) {
            System.err.println("   Forme End créée");
            diagram.append("      <bpmndi:BPMNShape id=\"end_di\" bpmnElement=\"end\">\n");
            diagram.append("        <dc:Bounds x=\"").append(endPos.x).append("\" y=\"")
                    .append(endPos.y).append("\" width=\"36\" height=\"36\" />\n");
            diagram.append("      </bpmndi:BPMNShape>\n");
        } else {
            System.err.println("   Position End manquante!");
        }

        System.err.println(" === FIN GÉNÉRATION FORMES ===\n");
    }
    private void debugTaskGeneration(StringBuilder bpmn, List<Statut> statuts) {
        System.err.println("GÉNÉRATION DES TÂCHES:");
        for (Statut statut : statuts) {
            String taskId = "task_" + statut.getId();
            System.err.println("   Tâche créée: " + taskId + " (" + statut.getName() + ") [Type: " + statut.getStatutType() + "]");
            bpmn.append("    <bpmn:task id=\"").append(taskId)
                    .append("\" name=\"").append(escapeXml(statut.getName())).append("\" />\n");
        }
    }
    // ✅ CORRECTION PRINCIPALE: generateEdges
    // 5. Remplacez COMPLÈTEMENT la méthode generateEdges par celle-ci :

    private void generateEdges(StringBuilder diagram, List<Statut> statuts, List<Passerelle> passerelles) {
        System.err.println("\nGÉNÉRATION DES EDGES VISUELS:");
        System.err.println("Nombre de flux à visualiser: " + generatedFlows.size());

        // ✅ NOUVELLE APPROCHE : Utiliser les flux enregistrés
        for (FlowInfo flow : generatedFlows) {
            ElementPosition sourcePos = elementPositions.get(flow.sourceRef);
            ElementPosition targetPos = elementPositions.get(flow.targetRef);

            if (sourcePos == null || targetPos == null) {
                System.err.println("   Positions manquantes pour edge: " + flow.id +
                        " (" + flow.sourceRef + " → " + flow.targetRef + ")");
                continue;
            }

            System.err.println("   Edge créé: " + flow.id + " (" +
                    flow.sourceRef + " → " + flow.targetRef + ")");

            diagram.append("      <bpmndi:BPMNEdge id=\"").append(flow.id)
                    .append("_di\" bpmnElement=\"").append(flow.id).append("\">\n");

            // Calculer les waypoints selon le type d'élément
            int sourceWidth = getElementWidth(flow.sourceRef);
            int sourceX = sourcePos.x + sourceWidth;
            int sourceY = sourcePos.y + (sourcePos.height / 2);
            int targetX = targetPos.x;
            int targetY = targetPos.y + (targetPos.height / 2);

            diagram.append("        <di:waypoint x=\"").append(sourceX)
                    .append("\" y=\"").append(sourceY).append("\" />\n");
            diagram.append("        <di:waypoint x=\"").append(targetX)
                    .append("\" y=\"").append(targetY).append("\" />\n");

            diagram.append("      </bpmndi:BPMNEdge>\n");
        }
    }

    // ✅ NOUVELLE MÉTHODE : Ajouter cette méthode utilitaire
    private int getElementWidth(String elementRef) {
        if (elementRef.equals("start") || elementRef.equals("end")) {
            return 36;  // Largeur des événements
        } else if (elementRef.startsWith("gateway_")) {
            return 50;  // Largeur des passerelles
        } else if (elementRef.startsWith("task_")) {
            return 100; // Largeur des tâches
        }
        return 0;
    }


    private void calculateElementPositions(List<Statut> statuts, List<Passerelle> passerelles) {
        System.err.println("\n CALCUL DES POSITIONS:");

        int currentX = 150;
        int standardY = 100;
        int spacing = 150;

        // Position start
        elementPositions.put("start", new ElementPosition(currentX, standardY, 36, 36));
        System.err.println("   start -> (" + currentX + ", " + standardY + ")");
        currentX += spacing;

        // ✅ CORRECTION : INCLURE TOUS LES STATUTS POUR LES POSITIONS
        // Pas seulement les "normaux" - tous les statuts peuvent avoir des tâches référencées
        System.err.println("   Positionnement des statuts:");
        for (Statut statut : statuts) {
            String taskId = "task_" + statut.getId();
            ElementPosition pos = new ElementPosition(currentX, standardY - 20, 100, 80);
            elementPositions.put(taskId, pos);

            System.err.println("     " + taskId + " (" + statut.getName() + ") -> (" + currentX + ", " + (standardY - 20) + ")");
            currentX += spacing;
        }

        // Passerelles
        if (passerelles != null) {
            System.err.println("   Positionnement des passerelles:");
            for (Passerelle passerelle : passerelles) {
                String elementId = "gateway_" + passerelle.getId();
                ElementPosition pos = new ElementPosition(currentX, standardY - 10, 50, 50);
                elementPositions.put(elementId, pos);

                System.err.println("     " + elementId + " (" + passerelle.getName() + ") -> (" + currentX + ", " + (standardY - 10) + ")");
                currentX += spacing;
            }
        }

        // Position end
        elementPositions.put("end", new ElementPosition(currentX, standardY, 36, 36));
        System.err.println("   end -> (" + currentX + ", " + standardY + ")");

        System.err.println(" === FIN CALCUL POSITIONS ===\n");
    }
    private String escapeXml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static class ElementPosition {
        int x, y, width, height;

        ElementPosition(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}