package com.example.ems.service.csv;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
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
    
    public <T> List<T> readCsvFromFile(File file, Class<T> clazz) {
        try (Reader reader = new BufferedReader(new FileReader(file))) {
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

    public void validateHeaders(File file, String[] expectedHeaders) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String headerLine = br.readLine();
            if (headerLine == null) throw new IllegalArgumentException("File is empty");

            String[] actualHeaders = Arrays.stream(headerLine.split(","))
                    .map(h -> h.trim().replace("\"", "").replace("\uFEFF", ""))
                    .toArray(String[]::new);

            List<String> actualList = Arrays.asList(actualHeaders);
            List<String> missingHeaders = new ArrayList<>();

            for (String expected : expectedHeaders) {
                boolean found = actualList.stream().anyMatch(h -> h.equalsIgnoreCase(expected));
                if (!found) {
                    missingHeaders.add(expected);
                }
            }

            if (!missingHeaders.isEmpty()) {
                throw new IllegalArgumentException("Missing required columns: " + String.join(", ", missingHeaders)
                        + ". Expected headers: " + Arrays.toString(expectedHeaders));
            }

        } catch (IOException e) {
            throw new RuntimeException("Error reading CSV header: " + e.getMessage());
        }
    }

    public void cleanUpTempFile(File file) {
        if (file != null && file.exists()) {
            try {
                Files.delete(file.toPath());
                log.info("Deleted temp file: {}", file.getName());
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", file.getAbsolutePath());
            }
        }
    }
}