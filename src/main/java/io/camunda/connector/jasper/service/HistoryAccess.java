package io.camunda.connector.jasper.service;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.response.ElementInstance;
import io.camunda.client.api.search.response.Incident;
import io.camunda.client.api.search.response.UserTask;
import io.camunda.client.api.search.response.Variable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

/**
 * Fetches the full execution history of a Camunda process instance via CamundaClient search APIs.
 * All methods return List&lt;Map&lt;String, Object&gt;&gt; so the data can be passed directly to JasperReports
 * as a JRBeanCollectionDataSource or as a plain parameter map.
 */

public class HistoryAccess {

    private static final Logger logger = LoggerFactory.getLogger(HistoryAccess.class);

    private final CamundaClient camundaClient;

    public HistoryAccess(CamundaClient camundaClient) {
        this.camundaClient = camundaClient;
    }

    /**
     * Returns all executed elements (tasks, events, gateways, sub-processes…) for the given
     * process instance, sorted by start date ascending.
     *
     * Each map contains:
     *   elementKey, elementId, elementName, type, state,
     *   startDate, endDate, durationMs, durationFormatted,
     *   hasIncident, incidentKey
     */
    public List<Map<String, Object>> getHistory(Long processInstanceKey) {
        logger.info("Fetching element instance history for processInstanceKey={}", processInstanceKey);
        List<Map<String, Object>> history = new ArrayList<>();
        try {
            // Build a map of elementInstanceKey -> assignee for completed user tasks
            Map<Long, String> assigneeByElementInstanceKey = getUserTaskAssignees(processInstanceKey);

            var response = camundaClient.newElementInstanceSearchRequest()
                    .filter(f -> f.processInstanceKey(processInstanceKey))
                    .page(p -> p.limit(1000))
                    .send()
                    .join();

            for (ElementInstance ei : response.items()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("elementKey",         ei.getElementInstanceKey());
                row.put("elementId",          ei.getElementId());
                row.put("elementName",        ei.getElementName());
                row.put("type",               ei.getType()  != null ? ei.getType().name()  : null);
                row.put("state",              ei.getState() != null ? ei.getState().name() : null);
                row.put("startDate",          ei.getStartDate() != null ? ei.getStartDate().toString() : null);
                row.put("endDate",            ei.getEndDate()   != null ? ei.getEndDate().toString()   : null);
                row.put("hasIncident",        Boolean.TRUE.equals(ei.getIncident()));
                row.put("incidentKey",        ei.getIncidentKey());
                row.put("assignee",           assigneeByElementInstanceKey.get(ei.getElementInstanceKey()));

                if (ei.getStartDate() != null && ei.getEndDate() != null) {
                    Duration d = Duration.between(ei.getStartDate(), ei.getEndDate());
                    row.put("durationMs",        d.toMillis());
                    row.put("durationFormatted", formatDuration(d));
                } else {
                    row.put("durationMs",        null);
                    row.put("durationFormatted", null);
                }
                history.add(row);
            }
        } catch (Exception e) {
            logger.error("Error fetching element instances for processInstanceKey={}: {}", processInstanceKey, e.getMessage());
        }

        history.sort(Comparator.comparing(
                m -> (String) m.get("startDate"),
                Comparator.nullsLast(Comparator.naturalOrder())));

        return history;
    }

    private Map<Long, String> getUserTaskAssignees(Long processInstanceKey) {
        Map<Long, String> assignees = new HashMap<>();
        try {
            var response = camundaClient.newUserTaskSearchRequest()
                    .filter(f -> f.processInstanceKey(processInstanceKey))
                    .page(p -> p.limit(1000))
                    .send()
                    .join();
            for (UserTask ut : response.items()) {
                if (ut.getElementInstanceKey() != null && ut.getAssignee() != null) {
                    assignees.put(ut.getElementInstanceKey(), ut.getAssignee());
                }
            }
        } catch (Exception e) {
            logger.warn("Could not fetch user task assignees for processInstanceKey={}: {}", processInstanceKey, e.getMessage());
        }
        return assignees;
    }

    /**
     * Returns all incidents raised during the process instance.
     *
     * Each map contains:
     *   incidentKey, elementId, elementInstanceKey, errorType, errorMessage,
     *   state, creationTime, jobKey
     */
    public List<Map<String, Object>> getIncidents(Long processInstanceKey) {
        logger.info("Fetching incidents for processInstanceKey={}", processInstanceKey);
        List<Map<String, Object>> incidents = new ArrayList<>();
        try {
            var response = camundaClient.newIncidentsByProcessInstanceSearchRequest(processInstanceKey)
                    .send()
                    .join();

            for (Incident inc : response.items()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("incidentKey",        inc.getIncidentKey());
                row.put("elementId",          inc.getElementId());
                row.put("elementInstanceKey", inc.getElementInstanceKey());
                row.put("errorType",          inc.getErrorType()  != null ? inc.getErrorType().name()  : null);
                row.put("errorMessage",       inc.getErrorMessage());
                row.put("state",              inc.getState() != null ? inc.getState().name() : null);
                row.put("creationTime",       inc.getCreationTime() != null ? inc.getCreationTime().toString() : null);
                row.put("jobKey",             inc.getJobKey());
                incidents.add(row);
            }
        } catch (Exception e) {
            logger.error("Error fetching incidents for processInstanceKey={}: {}", processInstanceKey, e.getMessage());
        }
        return incidents;
    }

    /**
     * Returns all process-instance-scope variables (latest value per variable name).
     *
     * Each map contains:
     *   name, value (JSON string), scopeKey, truncated
     */
    public List<Map<String, Object>> getVariables(Long processInstanceKey) {
        logger.info("Fetching variables for processInstanceKey={}", processInstanceKey);
        List<Map<String, Object>> vars = new ArrayList<>();
        try {
            var response = camundaClient.newVariableSearchRequest()
                    .filter(f -> f.processInstanceKey(processInstanceKey))
                    .page(p -> p.limit(1000))
                    .send()
                    .join();

            for (Variable v : response.items()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("name",      v.getName());
                row.put("value",     v.getValue());
                row.put("scopeKey",  v.getScopeKey());
                row.put("truncated", Boolean.TRUE.equals(v.isTruncated()));
                vars.add(row);
            }
        } catch (Exception e) {
            logger.error("Error fetching variables for processInstanceKey={}: {}", processInstanceKey, e.getMessage());
        }
        vars.sort(Comparator.comparing(
                m -> (String) m.getOrDefault("name", ""),
                Comparator.nullsLast(Comparator.naturalOrder())));
        return vars;
    }

    /**
     * Convenience method that collects everything into a single map suitable for use as the
     * historyData parameter of JasperGeneration.generate().
     *
     * Keys:  "elementInstances"  → List&lt;Map&gt;  (from getHistory)
     *        "incidents"         → List&lt;Map&gt;  (from getIncidents)
     *        "variables"         → List&lt;Map&gt;  (from getVariables)
     */
    public Map<String, Object> getAllHistory(Long processInstanceKey) {
        Map<String, Object> all = new LinkedHashMap<>();
        all.put("elementInstances", getHistory(processInstanceKey));
        all.put("incidents",        getIncidents(processInstanceKey));
        all.put("variables",        getVariables(processInstanceKey));
        return all;
    }

    // -------------------------------------------------------------------------

    private String formatDuration(Duration d) {
        long hours   = d.toHours();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();
        long millis  = d.toMillisPart();
        if (hours   > 0) return String.format("%dh %dm %ds",   hours, minutes, seconds);
        if (minutes > 0) return String.format("%dm %ds",       minutes, seconds);
        if (seconds > 0) return String.format("%d.%03ds",      seconds, millis);
        return String.format("%dms", millis);
    }
}
