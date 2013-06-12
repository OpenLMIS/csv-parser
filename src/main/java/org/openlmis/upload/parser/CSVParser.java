/*
 * Copyright © 2013 VillageReach.  All Rights Reserved.  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 *
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.openlmis.upload.parser;

import lombok.NoArgsConstructor;
import org.openlmis.upload.Importable;
import org.openlmis.upload.RecordHandler;
import org.openlmis.upload.exception.UploadException;
import org.openlmis.upload.model.AuditFields;
import org.openlmis.upload.model.ModelClass;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.supercsv.exception.SuperCsvCellProcessorException;
import org.supercsv.exception.SuperCsvConstraintViolationException;
import org.supercsv.exception.SuperCsvException;
import org.supercsv.util.CsvContext;

import java.io.IOException;
import java.io.InputStream;

@Component
@NoArgsConstructor
public class CSVParser {

  @Transactional
  public int process(InputStream inputStream, ModelClass modelClass, RecordHandler recordHandler, AuditFields auditFields)
      throws UploadException {

    CsvBeanReader csvBeanReader = null;
    String[] headers = null;

    try {
      csvBeanReader = new CsvBeanReader(modelClass, inputStream);
      headers = csvBeanReader.getHeaders();
      csvBeanReader.validateHeaders();
      Importable importedModel;

      while ((importedModel = csvBeanReader.readWithCellProcessors()) != null) {
        recordHandler.execute(importedModel, csvBeanReader.getRowNumber(), auditFields);
      }
      recordHandler.postProcess();
    } catch (SuperCsvConstraintViolationException constraintException) {
      if (constraintException.getMessage().contains("^\\d{1,2}/\\d{1,2}/\\d{4}$")) {
        createHeaderException("incorrect.date.format", headers, constraintException);
      }

      createHeaderException("missing.mandatory", headers, constraintException);
    } catch (SuperCsvCellProcessorException processorException) {
      createHeaderException("incorrect.data.type", headers, processorException);
    } catch (SuperCsvException superCsvException) {
      if (csvBeanReader.length() > headers.length) {
        throw new UploadException("incorrect.file.format");
      }

      createDataException("column.do.not.match", headers, superCsvException);
    } catch (IOException e) {
      throw new UploadException(e.getStackTrace().toString());
    }

    return csvBeanReader.getRowNumber() - 1;
  }


  private void createHeaderException(String error, String[] headers, SuperCsvException exception) {
    CsvContext csvContext = exception.getCsvContext();
    String header = headers[csvContext.getColumnNumber() - 1];
    Integer rowNum = csvContext.getRowNumber() - 1;
    throw new UploadException(error, header, "of Record No. ", rowNum.toString());
  }

  private void createDataException(String error, String[] headers, SuperCsvException exception) {
    CsvContext csvContext = exception.getCsvContext();
    Integer rowNum = csvContext.getRowNumber() - 1;
    throw new UploadException(error, headers.toString(), "in Record No. ", rowNum.toString(), csvContext.getRowSource().toString());
  }

  public void process(InputStream inputStream, ModelClass modelClass, RecordHandler handler) throws IOException {
    process(inputStream, modelClass, handler, null);
  }

}
