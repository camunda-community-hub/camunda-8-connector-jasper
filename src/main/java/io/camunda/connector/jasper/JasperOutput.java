package io.camunda.connector.jasper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.camunda.connector.cherrytemplate.CherryOutput;
import io.camunda.connector.cherrytemplate.RunnerParameter;

import java.util.List;
import java.util.Map;

public class JasperOutput implements CherryOutput {

    public static final String OUTPUT_DESTINATION_FILE = "destinationFile";
    public static final RunnerParameter JASPER_PARAMETER_DESTINATION_FILE = new RunnerParameter(OUTPUT_DESTINATION_FILE, // name
            "Destination variable name", // label
            String.class, // class
            RunnerParameter.Level.REQUIRED, "Process variable where the file reference is saved");
    public String destinationFileJson;

    public Object destinationFile;

    @JsonIgnore
    @Override
    public List<Map<String, Object>> getOutputParameters() {
        List<RunnerParameter> runnerParametersCollectList = List.of(JASPER_PARAMETER_DESTINATION_FILE);
        return runnerParametersCollectList.stream().map(t -> t.toMap()).toList();
    }

}
