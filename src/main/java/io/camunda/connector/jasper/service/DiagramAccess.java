package io.camunda.connector.jasper.service;

import io.camunda.client.CamundaClient;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.jasper.JasperError;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Collections;

public class DiagramAccess {

    private static final Logger logger = LoggerFactory.getLogger(DiagramAccess.class);

    private final CamundaClient camundaClient;

    public DiagramAccess(CamundaClient camundaClient) {
        this.camundaClient = camundaClient;
    }

    /**
     * Returns the BPMN diagram for the given process definition as a PNG byte array.
     * Returns null on any failure so the caller degrades gracefully.
     */
    public byte[] getDiagramAsPng(long processDefinitionKey) {
        try {
            long begnTime = System.currentTimeMillis();
            logger.info("Start generating diagram...");
            String bpmnXml = fetchBpmnXml(processDefinitionKey);
            if (bpmnXml == null || bpmnXml.isBlank()) {
                logger.info("Can't fetch BpmnXml");
                return null;
            }
            byte[] image = renderToPng(bpmnXml);
            logger.info("Finished generating diagram in {} ms", System.currentTimeMillis() - begnTime);
            return image;
        } catch (Exception e) {
            logger.error("Failed to generate diagram for processDefinitionKey={}: {}", processDefinitionKey, e.getMessage());
            return null;
        }
    }

    private String fetchBpmnXml(long processDefinitionKey) {
        try {
            return camundaClient.newProcessDefinitionGetXmlRequest(processDefinitionKey)
                    .send().join();
        } catch (Exception e) {
            logger.error("Failed to fetch BPMN XML for processDefinitionKey={}: {}", processDefinitionKey, e.getMessage());
            return null;
        }
    }

    private byte[] renderToPng(String bpmnXml) throws Exception {
        XMLInputFactory xif = XMLInputFactory.newInstance();
        // Disable external entity processing to avoid XXE
        xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        XMLStreamReader xtr = xif.createXMLStreamReader(new StringReader(bpmnXml));

        BpmnModel bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);

        ProcessDiagramGenerator generator = new DefaultProcessDiagramGenerator();
        try (InputStream pngStream = generator.generateDiagram(
                bpmnModel,
                Collections.emptyList(),   // no highlighted activities
                Collections.emptyList())) { // no highlighted flows
            return pngStream.readAllBytes();
        } catch (Exception e) {
            logger.error("Exception generating Drawing image: {}", e.getMessage());
            throw new ConnectorException(JasperError.GENERATING_DIAGRAM, "Error " + e.getMessage());
        } catch (Error e) {
            logger.error("Error generating Drawing image: {}", e);
            throw new ConnectorException(JasperError.GENERATING_DIAGRAM, "Error " + e.getMessage());
        }
    }
}
