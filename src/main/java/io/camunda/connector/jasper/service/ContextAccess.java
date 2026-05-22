package io.camunda.connector.jasper.service;

import io.camunda.client.CamundaClient;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

public class ContextAccess {
    private static final Logger logger = LoggerFactory.getLogger(ContextAccess.class);

    private final CamundaClient camundaClient;

    public ContextAccess(CamundaClient camundaClient) {
        this.camundaClient = camundaClient;
    }

    public Map<String, Object> getContext(OutboundConnectorContext outboundConnectorContext) {
        var job = outboundConnectorContext.getJobContext();

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("processInstanceKey",      job.getProcessInstanceKey());
        context.put("tenantId",                job.getTenantId());

        long processDefinitionKey = job.getProcessDefinitionKey();
        context.put("processDefinitionKey",    processDefinitionKey);
        context.put("processDefinitionVersion", job.getProcessDefinitionVersion());
        context.put("processDefinitionName",   fetchProcessDefinitionName(processDefinitionKey));

        long elementInstanceKey = job.getElementInstanceKey();
        context.put("activityKey",  elementInstanceKey);
        context.put("activityId",   job.getElementId());
        context.put("activityName", fetchActivityName(elementInstanceKey));

        fetchProcessInstanceDetails(job.getProcessInstanceKey(), context);

        return context;
    }

    private void fetchProcessInstanceDetails(long processInstanceKey, Map<String, Object> context) {
        try {
            var result = camundaClient.newProcessInstanceSearchRequest()
                    .filter(f -> f.processInstanceKey(processInstanceKey))
                    .page(p -> p.limit(1))
                    .send()
                    .join();
            if (!result.items().isEmpty()) {
                var instance = result.items().get(0);
                context.put("processInstanceRootKey",   instance.getRootProcessInstanceKey()==null? processInstanceKey: instance.getRootProcessInstanceKey());
                context.put("processInstanceParentKey", instance.getParentProcessInstanceKey()==null?processInstanceKey: instance.getParentProcessInstanceKey());
                context.put("processDefinitionId",      instance.getProcessDefinitionId());
                context.put("processStartDate",         instance.getStartDate());
            }
        } catch (Exception e) {
            logger.warn("Could not fetch process instance details for key={}: {}", processInstanceKey, e.getMessage());
        }
    }

    private String fetchProcessDefinitionName(long processDefinitionKey) {
        try {
            var result = camundaClient.newProcessDefinitionSearchRequest()
                    .filter(f -> f.processDefinitionKey(processDefinitionKey))
                    .page(p -> p.limit(1))
                    .send()
                    .join();
            if (!result.items().isEmpty()) {
                return result.items().get(0).getName();
            }
        } catch (Exception e) {
            logger.warn("Could not fetch process definition name for key={}: {}", processDefinitionKey, e.getMessage());
        }
        return null;
    }

    private String fetchActivityName(long elementInstanceKey) {
        try {
            var result = camundaClient.newElementInstanceSearchRequest()
                    .filter(f -> f.elementInstanceKey(elementInstanceKey))
                    .page(p -> p.limit(1))
                    .send()
                    .join();
            if (!result.items().isEmpty()) {
                return result.items().get(0).getElementName();
            }
        } catch (Exception e) {
            logger.warn("Could not fetch activity name for elementInstanceKey={}: {}", elementInstanceKey, e.getMessage());
        }
        return null;
    }
}
