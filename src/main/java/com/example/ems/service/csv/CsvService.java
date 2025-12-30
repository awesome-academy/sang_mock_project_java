package com.example.ems.service.csv;

import com.opencsv.bean.*;
import com.opencsv.CSVWriter;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;
import java.util.stream.Stream;

@Service
public class CsvService {

    public <T> void exportToCsv(HttpServletResponse response, List<T> data, Class<T> clazz, String fileName) {
        try {
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

            StatefulBeanToCsv<T> writer = new StatefulBeanToCsvBuilder<T>(response.getWriter())
                    .withQuotechar(CSVWriter.DEFAULT_QUOTE_CHARACTER) 
                    .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                    .withOrderedResults(true)
                    .build();

            writer.write(data);
            
        } catch (Exception ex) {
            throw new RuntimeException("Error exporting CSV: " + ex.getMessage());
        }
    }
    
    public <T> void exportToCsvStream(HttpServletResponse response, Stream<T> dataStream, Class<T> clazz, String fileName) {
        try {
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);

            StatefulBeanToCsv<T> writer = new StatefulBeanToCsvBuilder<T>(response.getWriter())
                    .withQuotechar(CSVWriter.DEFAULT_QUOTE_CHARACTER) 
                    .withSeparator(CSVWriter.DEFAULT_SEPARATOR)
                    .withOrderedResults(true)
                    .build();

            dataStream.forEach(item -> {
                try {
                    writer.write(item);
                } catch (Exception e) {
                    throw new RuntimeException("Error writing CSV row", e);
                }
            });
            
        } catch (Exception ex) {
            throw new RuntimeException("Error exporting CSV Stream: " + ex.getMessage());
        }
    }

    public <T> List<T> importFromCsv(MultipartFile file, Class<T> clazz) {
        if (file.isEmpty()) throw new RuntimeException("File is empty");

        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
                    .withType(clazz)
                    .withIgnoreLeadingWhiteSpace(true)
                    .withQuoteChar(CSVWriter.DEFAULT_QUOTE_CHARACTER)
                    .build();

            return csvToBean.parse();
        } catch (Exception ex) {
            throw new RuntimeException("Error parsing CSV file: " + ex.getMessage());
        }
    }
}