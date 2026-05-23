package io.camunda.connector.jasper;

public class JasperError {


    public static final String ERROR_BAD_INPUTPARAMETER = "BAD_INPUTPARAMETER";
    public static final String ERROR_BAD_INPUTPARAMETER_EXPLANATION = "During the bind, some input does not have the expected type";


    public static final String INCORRECT_STORAGEDEFINITION = "INCORRECTSTORAGEDEFINITION";
    public static final String INCORRECT_STORAGEDEFINITION_EXPLANATION = "Definition to access the storage is incorrect";

    public static final String ERROR_LOAD_DOCSOURCE = "LOAD_DOCSOURCE";
    public static final String ERROR_LOAD_DOCSOURCE_LABEL = "The reference can't be decoded";

    public static final String ERROR_LOAD_ERROR = "LOAD_ERROR";
    public static final String ERROR_LOAD_ERROR_LABEL = "An error occurs during the load";

    public static final String ERROR_NO_DESTINATION_STORAGE_DEFINITION_DEFINE = "NO_DESTINATION_STORAGE_DEFINITION";
    public static final String ERROR_NO_DESTINATION_STORAGE_DEFINITION_DEFINE_LABEL = "A destination storage must be provided (where do we store the result?";

    public static final String ERROR_SAVE_ERROR = "SAVE_ERROR";
    public static final String ERROR_SAVE_ERROR_LABEL = "An error occurs during the save";


    public static final String ERROR_EXECUTING_JASPER = "ERROR_EXECUTING_JASPER";
    public static final String ERROR_EXECUTING_JASPER_LABEL = "Error executing Jasper report";

    public static final String GENERATING_DIAGRAM = "GENERATING_DIAGRAM";
    public static final String GENERATING_DIAGRAM_LABEL = "Generating BPMN Diagram";

}
