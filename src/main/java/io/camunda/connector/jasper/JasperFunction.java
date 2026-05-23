package io.camunda.connector.jasper;

/* ******************************************************************** */
/*                                                                      */
/*  JasperFunction                                                         */
/*                                                                      */
/*  This connector is the main connector, doing the distribution on     */
/*  specific function                                                   */
/* ******************************************************************** */

import io.camunda.client.CamundaClient;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.cherrytemplate.CherryConnector;
import io.camunda.connector.jasper.report.JasperGeneration;
import io.camunda.connector.jasper.service.ContextAccess;
import io.camunda.connector.jasper.service.DiagramAccess;
import io.camunda.connector.jasper.service.HistoryAccess;
import io.camunda.filestorage.FileRepoFactory;
import io.camunda.filestorage.FileVariable;
import io.camunda.filestorage.FileVariableReference;
import io.camunda.filestorage.storage.StorageDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@OutboundConnector(name = "JasperFunction", inputVariables = {
        JasperInput.JASPER_REPORT,
        JasperInput.FORMAT_EXPORT,
        JasperInput.DATA,
        JasperInput.DESTINATION_FILE_NAME,
        JasperInput.DESTINATION_JSONSTORAGEDEFINITION,
        JasperInput.INCLUDE_DIAGRAM_IMAGE,
        JasperInput.INCLUDE_PROCESS_HISTORY,
        JasperInput.INCLUDE_CONTEXT}, type = "c-jasper-function")

public class JasperFunction implements OutboundConnectorFunction, CherryConnector {

    private static final String WORKER_LOGO =
            "data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHhtbG5zOnhsaW5rPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5L3hsaW5rIiB3aWR0aD0iMjc4LjI5MiIgaGVpZ2h0PSI0OS45MDYiIHZpZXdCb3g9IjAgMCAyNzguMjkyIDQ5LjkwNiI+PGRlZnM+PGNsaXBQYXRoIGlkPSJhIj48cmVjdCB3aWR0aD0iMjc4LjI5MiIgaGVpZ2h0PSI0OS45MDYiIGZpbGw9Im5vbmUiLz48L2NsaXBQYXRoPjwvZGVmcz48ZyB0cmFuc2Zvcm09InRyYW5zbGF0ZSgwIDApIiBjbGlwLXBhdGg9InVybCgjYSkiPjxwYXRoIGQ9Ik0yNy42NTcsMy42NzZWMTguNGExNS40NjksMTUuNDY5LDAsMCwxLS4zNjksMy45MzksNS45MjksNS45MjksMCwwLDEtMS4yNDYsMi40OTIsNS42ODIsNS42ODIsMCwwLDEtNC41NjIsMi4wMjIsNy42NTMsNy42NTMsMCwwLDEtNC4wMjEtMS4xMjdsMS42NzUtMi45NDdhNC4yNzIsNC4yNzIsMCwwLDAsMi4zNDYuODM5QTIuMTY1LDIuMTY1LDAsMCwwLDIzLjYsMjIuNDU0YTkuNDU0LDkuNDU0LDAsMCwwLC42MTUtNC4wNjZWMy42NzZaIiB0cmFuc2Zvcm09InRyYW5zbGF0ZSg0Ny42NjcgMTAuMDM3KSIgZmlsbD0iIzAwNzZhZCIvPjxwYXRoIGQ9Ik0zNi4yNjgsMjAuMTk1SDI3LjkxNmwtMi4xNjcsNC43NDlIMjIuNTc0bDkuNjUtMjAuNzE3LDkuMzA3LDIwLjcxN0gzOC4zMTJabS0xLjItMi43NjRMMzIuMTcyLDEwLjhsLTMuMDI5LDYuNjI5WiIgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoNjEuNjMyIDExLjUzOCkiIGZpbGw9IiMwMDc2YWQiLz48cGF0aCBkPSJNNDEuNDE4LDcuNzIxLDM5LjAzLDkuMTM4QTQuMTI3LDQuMTI3LDAsMCwwLDM3Ljc1NSw3LjYyYTMuNCwzLjQsMCwwLDAtMy42NjMuMjkxLDIuMTY0LDIuMTY0LDAsMCwwLS44MDksMS43MmMwLC45NTUuNzA5LDEuNzIsMi4xMjYsMi4zbDEuOTQ3LjhhOC4yMDYsOC4yMDYsMCwwLDEsMy40OCwyLjM0Niw1LjI0Myw1LjI0MywwLDAsMSwxLjEsMy4zOTQsNS45ODcsNS45ODcsMCwwLDEtMS43OSw0LjQ1NEE2LjE2MSw2LjE2MSwwLDAsMSwzNS42NjYsMjQuN2E2LjAwOSw2LjAwOSwwLDAsMS00LjItMS41MDcsNi45NTMsNi45NTMsMCwwLDEtMi4wMjktNC4yMzdsMi45OC0uNjUzYTUuMDgsNS4wOCwwLDAsMCwuNzA1LDIuMzcyLDMuMzU2LDMuMzU2LDAsMCwwLDQuOTM5LjMzOSwzLjE4LDMuMTgsMCwwLDAsLjkxLTIuMzM1LDMuMjQ0LDMuMjQ0LDAsMCwwLS4xNi0xLjA0NCwyLjY4MSwyLjY4MSwwLDAsMC0uNDkyLS44NzMsNC4xMjcsNC4xMjcsMCwwLDAtLjg2NS0uNzQyLDguMzE0LDguMzE0LDAsMCwwLTEuMjY4LS42NkwzNC4zLDE0LjU2OXEtNC4wMTctMS42OS00LjAxNy00Ljk1YTQuNzExLDQuNzExLDAsMCwxLDEuNjgyLTMuNjc4LDYuMDkxLDYuMDkxLDAsMCwxLDQuMTg1LTEuNDg4LDUuODM3LDUuODM3LDAsMCwxLDUuMjY3LDMuMjY4IiB0cmFuc2Zvcm09InRyYW5zbGF0ZSg4MC4zNzcgMTIuMTU4KSIgZmlsbD0iIzAwNzZhZCIvPjxwYXRoIGQ9Ik0zOC4wNjQsMTYuMTQxdjcuOTE1SDM1LjEzMlY0LjU1MWgzLjMyN2ExNC45NDcsMTQuOTQ3LDAsMCwxLDMuNjkzLjM0Myw0Ljk1OCw0Ljk1OCwwLDAsMSwyLjIxOSwxLjI5MSw1LjU2Myw1LjU2MywwLDAsMSwxLjY3NSw0LjE0NCw1LjM3Niw1LjM3NiwwLDAsMS0xLjc5MSw0LjI0OSw3LjEsNy4xLDAsMCwxLTQuODMxLDEuNTYzWm0wLTIuNzY0aDEuMXE0LjA1MSwwLDQuMDUxLTMuMSwwLTIuOTkzLTQuMTc4LTNoLS45NzRaIiB0cmFuc2Zvcm09InRyYW5zbGF0ZSg5NS45MTggMTIuNDI1KSIgZmlsbD0iIzAwNzZhZCIvPjxwYXRoIGQ9Ik01MS4wMTUsNy4zMTJINDMuMlYxMmg3LjU4N3YyLjc2NEg0My4ydjYuNTMyaDcuODE5djIuNzY0SDQwLjI2NVY0LjU1MmgxMC43NVoiIHRyYW5zZm9ybT0idHJhbnNsYXRlKDEwOS45MzEgMTIuNDI3KSIgZmlsbD0iIzAwNzZhZCIvPjxwYXRoIGQ9Ik01Mi4xNjQsMTUuNzQzLDU4LjIsMjQuMDU3SDU0LjZsLTUuNTY5LTcuOTgzSDQ4LjV2Ny45ODNINDUuNTczVjQuNTUyaDMuNDQzYzIuNTc4LDAsNC40MzEuNDg5LDUuNTc3LDEuNDU1YTUuMzM3LDUuMzM3LDAsMCwxLDEuODkxLDQuMjY3LDUuNTI2LDUuNTI2LDAsMCwxLTEuMTksMy41NjIsNS4yNTgsNS4yNTgsMCwwLDEtMy4xMywxLjkwNk00OC41LDEzLjVoLjkzM3E0LjE1MiwwLDQuMTUyLTMuMmMwLTEuOTkyLTEuMzQ3LTMtNC4wNC0zSDQ4LjVaIiB0cmFuc2Zvcm09InRyYW5zbGF0ZSgxMjQuNDI0IDEyLjQyOCkiIGZpbGw9IiMwMDc2YWQiLz48cGF0aCBkPSJNNTkuNTA1LDIxLjAxMWEzLjIsMy4yLDAsMCwwLC45MS0yLjMzNSwzLjMzNCwzLjMzNCwwLDAsMC0uMTU3LTEuMDQ0LDIuNzUxLDIuNzUxLDAsMCwwLS40OTItLjg3Myw0LjA4Nyw0LjA4NywwLDAsMC0uODczLS43NDIsNy42MzIsNy42MzIsMCwwLDAtMS4yNjUtLjY2bC0xLjg4Ny0uNzg3Yy0yLjY3OC0xLjEyNy00LjAxNy0yLjc4My00LjAxNy00Ljk1YTQuNzExLDQuNzExLDAsMCwxLDEuNjgyLTMuNjc4LDYuMDYsNi4wNiwwLDAsMSw0LjE4NS0xLjQ4OCw1LjgzNCw1LjgzNCwwLDAsMSw1LjI2NywzLjI2OEw2MC40NjgsOS4xMzRBNC4wMDYsNC4wMDYsMCwwLDAsNTkuMTkyLDcuNjJhMi45MzcsMi45MzcsMCwwLDAtMS42MjYtLjQsMi45ODksMi45ODksMCwwLDAtMi4wMzMuNjk0LDIuMTU3LDIuMTU3LDAsMCwwLS44MTMsMS43MmMwLC45NTUuNzEyLDEuNzIsMi4xMywyLjNsMS45NDcuOGE4LjIyLDguMjIsMCwwLDEsMy40OCwyLjM0Niw1LjI4Myw1LjI4MywwLDAsMSwxLjEsMy4zOTQsNS45NjIsNS45NjIsMCwwLDEtMS43OTQsNC40NTRBNi4xNCw2LjE0LDAsMCwxLDU3LjEsMjQuN2E1Ljk2OSw1Ljk2OSwwLDAsMS00LjE4OS0xLjUxMSw2LjksNi45LDAsMCwxLTIuMDMzLTQuMjM0bDIuOTc3LS42NTNhNS4wODMsNS4wODMsMCwwLDAsLjcxMiwyLjM3MiwzLjM1MiwzLjM1MiwwLDAsMCw0LjkzNS4zMzkiIHRyYW5zZm9ybT0idHJhbnNsYXRlKDEzOC45MTYgMTIuMTU4KSIgZmlsbD0iIzAwNzZhZCIvPjxwYXRoIGQ9Ik01Ni4yOTIsMTQuNDkxYTkuNTU5LDkuNTU5LDAsMCwxLDMuMDE0LTcuMDgsOS45Myw5LjkzLDAsMCwxLDcuMjIyLTIuOTU4LDkuNzQ3LDkuNzQ3LDAsMCwxLDcuMTQ3LDIuOTg0LDkuNzg0LDkuNzg0LDAsMCwxLDIuOTkyLDcuMTY5LDkuNiw5LjYsMCwwLDEtMyw3LjE0MywxMC4yOTUsMTAuMjk1LDAsMCwxLTE0LjA2Ny4zMzIsOS42LDkuNiwwLDAsMS0zLjMtNy41OTFtMi45NTguMDM0YTcuMDY5LDcuMDY5LDAsMCwwLDIuMTc1LDUuMzI3QTcuMTk1LDcuMTk1LDAsMCwwLDcxLjYsMTkuODEsNy4yMTEsNy4yMTEsMCwwLDAsNzMuNywxNC41NzcsNy4xNTIsNy4xNTIsMCwwLDAsNzEuNjE2LDkuMzRhNy4yNjgsNy4yNjgsMCwwLDAtMTAuMjY2LDAsNy4wNDksNy4wNDksMCwwLDAtMi4xLDUuMTg1IiB0cmFuc2Zvcm09InRyYW5zbGF0ZSgxNTMuNjg5IDEyLjE1OCkiIGZpbGw9IiMwMDc2YWQiLz48cGF0aCBkPSJNNzMuODYyLDcuMzEyaC02LjhWMTJoNi41NTh2Mi43NjRINjcuMDY1djkuM0g2NC4xMzdWNC41NTJoOS43MjVaIiB0cmFuc2Zvcm09InRyYW5zbGF0ZSgxNzUuMTA4IDEyLjQyNykiIGZpbGw9IiMwMDc2YWQiLz48cGF0aCBkPSJNNzUuOTE1LDcuMzEyVjI0LjA1N0g3Mi45ODNWNy4zMTJINjguNVY0LjU1Mkg4MC4zODR2Mi43NloiIHRyYW5zZm9ybT0idHJhbnNsYXRlKDE4Ny4wMDkgMTIuNDI3KSIgZmlsbD0iIzAwNzZhZCIvPjxwYXRoIGQ9Ik0xNC41LDcuMDM3QzI1LjEtLjQ4NywzNi43ODItMi4yNCw0MC41NzksMy4xMTdjMS41NDQsMi4xODIsMS41MzMsNS4yMjIuMjUsOC41NjVhMy4xNjUsMy4xNjUsMCwwLDAtLjUxMS0xLjMxM2MtMi4zNjktMy4zNDYtMTEuMjc2LTEuMDkzLTE5LjksNS4wMjFTNi43MzEsMjkuMTY4LDkuMSwzMi41MTFhMy4xMDUsMy4xMDUsMCwwLDAsMS4wNzEuOTI1Yy0zLjU3LjA5My02LjQ1My0uODkyLTgtMy4wNzQtMy44LTUuMzU3LDEuNzEyLTE1LjgsMTIuMzI1LTIzLjMyNSIgdHJhbnNmb3JtPSJ0cmFuc2xhdGUoMi42NjEgMC4wMzkpIiBmaWxsPSIjOThiNWQ2Ii8+PHBhdGggZD0iTTQxLjA0OCw0MC43NzhDMjUuOTMsNTEuNTA2LDguNTkyLDUzLDIuMjczLDQ0LjA4N2ExMy44NjYsMTMuODY2LDAsMCwxLTIuMTE5LTEwLDguMDczLDguMDczLDAsMCwwLDEuMDU5LDIuMTg2YzQuODE2LDYuOCwxOS42NDcsNC41NjIsMzMuMTIxLTQuOTkxUzU0LjgsOC40NjgsNDkuOTgyLDEuNjcxQTguMzE4LDguMzE4LDAsMCwwLDQ4LjMzNywwLDEzLjg1NCwxMy44NTQsMCwwLDEsNTcsNS4yNjdjNi4zMTksOC45MTEtLjg0MywyNC44LTE1Ljk0NywzNS41MTEiIHRyYW5zZm9ybT0idHJhbnNsYXRlKC0wLjAwMSAwKSIgZmlsbD0iIzAwNzZhZCIvPjxwYXRoIGQ9Ik03NC4zNzYsMy44NzJWNy4zNDVINzMuMTNWMy44NzJoLS45NTFWMi44aDMuMTQ4VjMuODcyWm0xLjE4MiwzLjQ3M0w3Ni4zMiwyLjhoMS4yMjNsLjg2NSwyLjMyLjkxLTIuMzJoMS4yMzVsLjY3NSw0LjU0SDc5Ljk4M2wtLjMwNi0yLjQ2Mkw3OC42NjIsNy4zNDVoLS41NDhsLS45NTktMi40NjItLjM2MiwyLjQ2MloiIHRyYW5zZm9ybT0idHJhbnNsYXRlKDE5Ny4wNjQgNy42NTkpIiBmaWxsPSIjMDA3NmFkIi8+PC9nPjwvc3ZnPg==";
    private final Logger logger = LoggerFactory.getLogger(JasperFunction.class.getName());
    @Autowired
    CamundaClient camundaClient;



    @Override
    public JasperOutput execute(OutboundConnectorContext outboundConnectorContext) throws Exception {
        JasperInput jasperInput;
        try {
           jasperInput = outboundConnectorContext.bindVariables(JasperInput.class);
        } catch (Exception e) {
            throw new ConnectorException(JasperError.ERROR_BAD_INPUTPARAMETER, "PDFFunction can't bind variable " + e.getMessage() + "]");
        }
        FileRepoFactory fileRepoFactory = FileRepoFactory.getInstance();

        JasperGeneration jasperGeneration = new JasperGeneration();
        // Load input
        FileVariableReference sourceFiledocSourceReference = FileVariableReference.fromObject(jasperInput.getJasperReport());
        FileVariable fileVariable = loadDocSourceFromReference(sourceFiledocSourceReference, fileRepoFactory, outboundConnectorContext);


        // -- prepare output
        StorageDefinition destinationStorageDefinition = jasperInput.getDestinationStorageDefinitionObject();

        if (destinationStorageDefinition == null) {
            // no storage: this is a problem
            logger.error("No destination storage definition define ");
            throw new ConnectorException(JasperError.ERROR_NO_DESTINATION_STORAGE_DEFINITION_DEFINE);
        }


        Map<String, Object> historyData = new HashMap<>();
        if (Boolean.TRUE.equals(jasperInput.getIncludeProcessHistory())) {
            HistoryAccess historyAccess = new HistoryAccess(camundaClient);
            historyData = historyAccess.getAllHistory(outboundConnectorContext.getJobContext().getProcessInstanceKey());
        }
        Map<String, Object> contextData = new HashMap<>();
        if (Boolean.TRUE.equals(jasperInput.getIncludeContext())) {
            ContextAccess contextAccess = new ContextAccess(camundaClient);
            contextData = contextAccess.getContext(outboundConnectorContext);
        }

        Map<String, Object> imagesData = new HashMap<>();
        if (Boolean.TRUE.equals(jasperInput.getIncludeDiagramImage())) {
            DiagramAccess diagramAccess = new DiagramAccess(camundaClient);
            long processDefinitionKey = outboundConnectorContext.getJobContext().getProcessDefinitionKey();
            imagesData.put("diagramImage", diagramAccess.getDiagramAsPng(processDefinitionKey));
        }

        // ---------- generate report
        JasperGeneration.FORMAT format = jasperInput.getFormatExport();

        ByteArrayOutputStream jasperReport = jasperGeneration.generate(format,
                fileVariable.getName(),
                fileVariable.getValueStream(),
                jasperInput.getData(),
                contextData,
                historyData,
                imagesData);

        // save the result
        try {
            JasperOutput jasperOutput = new JasperOutput();
            FileVariable fileVariableOut = new FileVariable();

            fileVariableOut.setValue(jasperReport.toByteArray());
            fileVariableOut.setName(jasperInput.getDestinationFileName() + format.extension);
            fileVariableOut.setMimeType(format.mimeType);
            fileVariableOut.setStorageDefinition(destinationStorageDefinition);
            // Second, write it to the fileRepo
            FileVariableReference outputFileReference = fileRepoFactory.saveFileVariable(fileVariableOut, outboundConnectorContext);
            jasperOutput.destinationFile = outputFileReference;
            return jasperOutput;
        } catch (Exception e) {
            logger.error("Error during save to name[{}] StorageDefinition[{}] : {}",
                    jasperInput.getDestinationFileName(), destinationStorageDefinition, e.getMessage());

            throw new ConnectorException(JasperError.ERROR_SAVE_ERROR,
                    "Name [" + jasperInput.getDestinationFileName() + "] StorageDefinition [" + destinationStorageDefinition + "] Error " + e);
        }


    }




    @Override
    public String getDescription() {
        return "Execute a Jasper report";
    }

    @Override
    public String getLogo() {
        return WORKER_LOGO;
    }

    @Override
    public String getCollectionName() {
        return "PDF";
    }

    @Override
    public Map<String, String> getListBpmnErrors() {
        return Map.of(JasperError.ERROR_BAD_INPUTPARAMETER, JasperError.ERROR_BAD_INPUTPARAMETER_EXPLANATION,
                JasperError.INCORRECT_STORAGEDEFINITION, JasperError.INCORRECT_STORAGEDEFINITION_EXPLANATION,
                JasperError.ERROR_LOAD_DOCSOURCE, JasperError.ERROR_LOAD_DOCSOURCE_LABEL,
                JasperError.ERROR_LOAD_ERROR, JasperError.ERROR_LOAD_ERROR_LABEL,
                JasperError.ERROR_NO_DESTINATION_STORAGE_DEFINITION_DEFINE, JasperError.ERROR_NO_DESTINATION_STORAGE_DEFINITION_DEFINE_LABEL,
                JasperError.ERROR_SAVE_ERROR, JasperError.ERROR_SAVE_ERROR_LABEL,
                JasperError.ERROR_EXECUTING_JASPER, JasperError.ERROR_EXECUTING_JASPER_LABEL);
    }

    @Override
    public Class<?> getInputParameterClass() {
        return JasperInput.class;
    }

    @Override
    public Class<?> getOutputParameterClass() {
        return JasperOutput.class;
    }

    /**
     * Only task at this moment (no InboundConnector)
     *
     * @return list of items where the function applies
     */
    @Override
    public List<String> getAppliesTo() {
        return List.of("bpmn:ServiceTask");
    }


    private FileVariable loadDocSourceFromReference(FileVariableReference docReference,
                                                    FileRepoFactory fileRepoFactory,
                                                    OutboundConnectorContext outboundConnectorContext) {
        try {
            FileVariable docSource = fileRepoFactory.loadFileVariable(docReference, outboundConnectorContext);

            // get the file - don't get any value here, because in a Stream approach, we don't ant to consu;eteh stream
            if (docSource == null || (docSource.isValueBytes() && docSource.getValue() == null)) {
                String json = docReference.toJson();
                logger.error("Can't read file [{}] ", json);
                throw new ConnectorException(JasperError.ERROR_LOAD_ERROR,
                        "Can't read file [" + docReference.toJson() + "]");
            }
            return docSource;
        } catch (ConnectorException ce) {
            throw ce;
        } catch (Exception e) {

            try {
                logger.error("Exception loadDocument[{}] : {} ",
                        docReference.toJson(),
                        e.getMessage()
                                .replace("\r", "\\r")
                                .replace("\n", "\\n")
                                .replace("\t", "\\t"));
            } catch (Exception e2) {
                logger.error("Exception load : {} (JsonPrint: {} ", e.getMessage(), e2.getMessage());
            }
            throw new ConnectorException(JasperError.ERROR_LOAD_DOCSOURCE, "DocReference[" + docReference + "] Error : " + e);
        }
    }
}
