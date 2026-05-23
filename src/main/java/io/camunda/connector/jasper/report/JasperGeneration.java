package io.camunda.connector.jasper.report;

import io.camunda.connector.api.error.ConnectorException;
import io.camunda.connector.jasper.JasperError;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.oasis.JROdsExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRPptxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.*;
import net.sf.jasperreports.pdf.JRPdfExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class JasperGeneration {

    private static final JasperReportCache reportCache = new JasperReportCache();

    private final Logger logger = LoggerFactory.getLogger(JasperGeneration.class.getName());

    public ByteArrayOutputStream generate(FORMAT format, String jrxmlName,
                                          InputStream jrxmlStream,
                                          Map<String, Object> processVariablesData,
                                          Map<String, Object> contextData,
                                          Map<String, Object> historyData,
                                          Map<String,Object> imagesData
    )
            throws ConnectorException {
        long begin = System.currentTimeMillis();
        String analysis = "report[" + jrxmlName + "]";
        try {
            logger.info("[{}] compilation started...", jrxmlName);
            analysis += " compilation started...";
            byte[] jrxmlBytes = jrxmlStream.readAllBytes();
            JasperReport jasperReport = reportCache.getJasperReport(jrxmlName, jrxmlBytes);
            long endCompilation = System.currentTimeMillis();

            logger.info("[{}] fill report started...", jrxmlName);
            analysis += "Done. File report started...";
            Map<String, Object> reportData = new HashMap<>(historyData);
            reportData.put("variables", processVariablesData);
            reportData.put("context", contextData);
            reportData.put("history", historyData);
            reportData.put("images", imagesData);

            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, reportData, new JREmptyDataSource());
            long endFillReport = System.currentTimeMillis();

            logger.info("[{}] export started...", jrxmlName);
            analysis += "Done. Export started...";
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            @SuppressWarnings({"rawtypes", "unchecked"})
            Exporter exporter = switch (format) {
                case PDF -> new JRPdfExporter();
                case WORD -> new JRDocxExporter();
                case EXCEL -> new JRXlsxExporter();
                case POWERPOINT -> new JRPptxExporter();
                case OPENOFFICEWRITER -> new JROdtExporter();
                case OPENOFFICECALC -> new JROdsExporter();
                case CSV -> new JRCsvExporter();
                case HTML -> new HtmlExporter();
            };
            ExporterOutput exporterOutput = switch (format) {
                case CSV -> new SimpleWriterExporterOutput(out);
                case HTML -> new SimpleHtmlExporterOutput(out);
                default -> new SimpleOutputStreamExporterOutput(out);
            };
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(exporterOutput);
            exporter.exportReport();
            long endExport = System.currentTimeMillis();

            logger.info("JasperGeneration [{}] format [{}] completed in {} ms (compilation {} ms fill {} ms, export {} ms)",
                    jrxmlName,
                    format,
                    endExport - begin,
                    endCompilation - begin,
                    endFillReport - endCompilation,
                    endExport - endFillReport);
            return out;

        } catch (Exception e) {
            logger.error("Generate Jasper {} processVariablesData[{}] ContextData[{}] HistoryData[{}]:  ",
                    analysis,
                    processVariablesData,
                    contextData,
                    historyData, e);
            throw new ConnectorException(JasperError.ERROR_EXECUTING_JASPER, e.getMessage());
        }

    }

    public enum FORMAT {
        PDF("application/pdf", ".pdf"),
        WORD("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
        EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
        POWERPOINT("application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx"),
        OPENOFFICEWRITER("application/vnd.oasis.opendocument.text", ".odt"),
        OPENOFFICECALC("application/vnd.oasis.opendocument.spreadsheet", ".ods"),
        CSV("text/csv", ".csv"),
        HTML("text/html", ".html");

        public final String mimeType;
        public final String extension;

        FORMAT(String mimeType, String extension) {
            this.mimeType = mimeType;
            this.extension = extension;
        }
    }


}
