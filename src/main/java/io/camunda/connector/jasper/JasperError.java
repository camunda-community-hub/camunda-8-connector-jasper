package io.camunda.connector.jasper;

public class JasperError {

    public static final String ERROR_UNKNOWN_FUNCTION = "UNKNOWN_FUNCTION";
    public static final String ERROR_UNKNOWN_FUNCTION_EXPLANATION = "The function is unknown. There is a limited number of operation";

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

    public static final String ERROR_CREATE_FILEVARIABLE = "ERROR_CREATE_FILEVARIABLE";
    public static final String ERROR_CREATE_FILEVARIABLE_LABEL = "Error when reading the PDF to create a fileVariable to save";
    public static final String ERROR_SAVE_ERROR = "SAVE_ERROR";
    public static final String ERROR_SAVE_ERROR_LABEL = "An error occurs during the save";


    public static final String ERROR_EXECUTING_JASPER = "ERROR_EXECUTING_JASPER";
    public static final String ERROR_EXECUTING_JASPER_LABEL = "Error executing Jasper report";



}
