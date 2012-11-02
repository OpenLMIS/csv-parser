package org.openlmis.upload.parser;

import org.openlmis.upload.Importable;
import org.openlmis.upload.RecordHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.prefs.CsvPreference;

import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class CSVParser {

    private ImportFieldParser importFieldParser;

    @Autowired
    public CSVParser(ImportFieldParser importFieldParser) {
        this.importFieldParser = importFieldParser;
    }

    public void process(File csvFile, Class<? extends Importable> modelClass, RecordHandler recordHandler) throws Exception {

        FileReader fileReader = new FileReader(csvFile);
        CsvBeanReader csvBeanReader = new CsvBeanReader(fileReader, CsvPreference.STANDARD_PREFERENCE);

        String[] headers = csvBeanReader.getHeader(true);
        trimWhiteSpaceFromHeaders(headers);
        Set<String> headersSet = new LinkedHashSet<String>(Arrays.asList(headers));

        List<CellProcessor> cellProcessors = importFieldParser.parse(modelClass, headersSet);

        CellProcessor[] processors = cellProcessors.toArray(new CellProcessor[0]);
        Importable importedModel;

        while ((importedModel = csvBeanReader.read(modelClass, headers, processors)) != null) {
            recordHandler.execute(importedModel);
        }
    }

    private void trimWhiteSpaceFromHeaders(String[] headers) {
        for (int i = 0; i < headers.length; i++) {
            headers[i] = headers[i].trim();
        }
    }
}
