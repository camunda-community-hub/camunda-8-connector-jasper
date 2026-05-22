package io.camunda.connector.jasper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.cherrytemplate.CherryInput;
import io.camunda.connector.cherrytemplate.RunnerParameter;
import io.camunda.connector.jasper.report.JasperGeneration;
import io.camunda.filestorage.storage.StorageDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static final String DATA = "data";
    public static final String FORMAT_EXPORT = "formatExport";
    public static final String INCLUDE_PROCESS_HISTORY = "includeProcessHistory";
    public static final String INCLUDE_CONTEXT = "includeContext";
    public static final String DESTINATION_FILE_NAME = "destinationFileName";
    public static final String DESTINATION_JSONSTORAGEDEFINITION = "destinationJsonStorageDefinition";

    public static final RunnerParameter jasperParameterJasperReport = new RunnerParameter(
            JasperInput.JASPER_REPORT,
            // name
            "Jasper Report", // label
            Object.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Access to the Jasper Report. FileReference object. See FileStorage for more details.");

    public static final RunnerParameter jasperParameterData = new RunnerParameter(
            JasperInput.DATA,
            // name
            "Data", // label
            Object.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Map of data. Only data passed here are visible in the report, to avoid to expose all process variables on the report.");

    public static final RunnerParameter jasperParameterFormatExport = new RunnerParameter(
            JasperInput.FORMAT_EXPORT,
            // name
            "Format export", // label
            String.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Format of the document generated")
            .addChoice(JasperGeneration.FORMAT.PDF.toString(), "PDF")
            .addChoice(JasperGeneration.FORMAT.WORD.toString(), "Word")
            .addChoice(JasperGeneration.FORMAT.EXCEL.toString(), "Excel")
            .addChoice(JasperGeneration.FORMAT.CSV.toString(), "Csv")
            .addChoice(JasperGeneration.FORMAT.POWERPOINT.toString(), "PowerPoint")
            .addChoice(JasperGeneration.FORMAT.OPENOFFICEWRITER.toString(), "Openoffice Writer")
            .addChoice(JasperGeneration.FORMAT.OPENOFFICECALC.toString(), "Openoffice Calc")
            .addChoice(JasperGeneration.FORMAT.HTML.toString(), "Html");

    public static final RunnerParameter jasperParameterIncludeHistory = new RunnerParameter(
            JasperInput.INCLUDE_PROCESS_HISTORY,
            // name
            "Include history", // label
            Boolean.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Include the process instance history. History is accessible in the report (all tasks exectued)")
            .addChoice(Boolean.TRUE.toString(), "Yes")
            .addChoice(Boolean.FALSE.toString(), "No");
    public static final RunnerParameter jasperParameterIncludeContext = new RunnerParameter(
            JasperInput.INCLUDE_CONTEXT,
            // name
            "Include context", // label
            Boolean.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Include the context (processInstanceKey, processDefinitionKey, name...")
            .addChoice(Boolean.TRUE.toString(), "Yes")
            .addChoice(Boolean.FALSE.toString(), "No");
    public static final RunnerParameter jasperParameterDestinationFileName = new RunnerParameter(
            JasperInput.DESTINATION_FILE_NAME,
            // name
            "Destination file name", // label
            String.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Name of the new file created");
    public static final RunnerParameter jasperParameterDestinationJsonStorageDefinition = new RunnerParameter(
            JasperInput.DESTINATION_JSONSTORAGEDEFINITION, // name
            "JSon Storage Destination", // label
            Map.class, // class
            RunnerParameter.Level.REQUIRED, // level
            "Storage Definition in Json.");

    public static List<RunnerParameter> runnerParametersCollectList = List.of(
            jasperParameterJasperReport,
            jasperParameterData,
            jasperParameterFormatExport,
            jasperParameterIncludeContext,
            jasperParameterIncludeHistory,
            jasperParameterDestinationFileName,
            jasperParameterDestinationJsonStorageDefinition);
    private final Logger logger = LoggerFactory.getLogger(JasperInput.class.getName());

    private Object jasperReport;
    private String destinationFileName;

    private Object destinationJsonStorageDefinition;

    private Map<String, Object> data;


    private Boolean includeProcessHistory;

    private Boolean includeContext;

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

    public Map<String, Object> getData() {
        return data;
    }

    public Boolean getIncludeProcessHistory() {
        return includeProcessHistory;
    }

    public Boolean getIncludeContext() {
        return includeContext;
    }

    @JsonIgnore
    @Override
    public List<Map<String, Object>> getInputParameters() {


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
