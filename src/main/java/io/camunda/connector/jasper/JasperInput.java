package io.camunda.connector.jasper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.cherrytemplate.CherryInput;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import io.camunda.connector.jasper.report.JasperGeneration;
import io.camunda.connector.toolbox.JasperError;
import io.camunda.filestorage.storage.StorageDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * the JsonIgnoreProperties is mandatory: the template may contain additional widget to help the designer, especially on the OPTIONAL parameters
 * This avoids the MAPPING Exception
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JasperInput implements CherryInput {
    /**
     * Attention, each Input here must be added in the JasperFunction, list of InputVariables
     */
    public static final String JASPER_REPORT = "jasperReport";
    public static final String PARAMETERS = "parameters";
    public static final String FORMATEXPORT = "formatExport";
    public static final String INCLUDEPROCESSHISTORY = "includeProcessInstory";
    public static final String DESTINATION_FILE_NAME = "destinationFileName";
    public static final String DESTINATION_JSONSTORAGEDEFINITION = "destinationJsonStorageDefinition";

    public static final RunnerParameter jasperParameterDestinationFileName = new RunnerParameter(
            JasperInput.DESTINATION_FILE_NAME,
            // name
            "Destination file name", // label
            String.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Name of the new file created");
    public static final RunnerParameter pdfParameterDestinationJsonStorageDefinition = new RunnerParameter(
            JasperInput.DESTINATION_JSONSTORAGEDEFINITION, // name
            "JSon Storage Destination", // label
            Map.class, // class
            RunnerParameter.Level.OPTIONAL, // level
            "Storage Definition in Json.");

    private final Logger logger = LoggerFactory.getLogger(JasperInput.class.getName());

    private Object jasperReport;
    private String destinationFileName;

    private Object destinationJsonStorageDefinition;

    private Map<String, Object> parameters;


    private Boolean includeProcessHistory;

    private String formatExport;

    public Object getJasperReport() {
        return jasperReport;
    }


    public JasperGeneration.FORMAT getFormatExport() {
        try {
            if (formatExport == null) {
                return JasperGeneration.FORMAT.PDF;
            }
            return JasperGeneration.FORMAT.valueOf(formatExport);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid format export: " + formatExport);
            return JasperGeneration.FORMAT.PDF;
        }
    }
    public String getDestinationFileName() {
        return destinationFileName;
    }

    public Object getDestinationJsonStorageDefinition() {
        return destinationJsonStorageDefinition;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Boolean getIncludeProcessHistory() {
        return includeProcessHistory;
    }

    @JsonIgnore
    @Override
    public List<Map<String, Object>> getInputParameters() {
        List<RunnerParameter> runnerParametersCollectList = new ArrayList<>();

        return runnerParametersCollectList.stream().map(t -> t.toMap()).toList();


    }

    /**
     * Return a Storage definition
     *
     * @return the storage definition
     * @throws ConnectorException if the connection
     */
    @JsonIgnore
    public StorageDefinition getDestinationStorageDefinitionObject() throws ConnectorException {
        try {
            StorageDefinition storageDefinitionObj = null;
            // Attention, it may be an empty string due to the modeler which not like null value
            if (getDestinationJsonStorageDefinition() != null && !getDestinationJsonStorageDefinition().toString().trim().isEmpty()) {
                storageDefinitionObj = StorageDefinition.getFromObject(getDestinationJsonStorageDefinition());
                return storageDefinitionObj;
            }

            return null;

        } catch (Exception e) {
            logger.error("Can't get the FileStorage - bad Gson value :" + destinationJsonStorageDefinition);
            throw new ConnectorException(JasperError.INCORRECT_STORAGEDEFINITION,
                    "FileStorage information" + destinationJsonStorageDefinition);
        }
    }
}
