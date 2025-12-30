package com.example.ems.dto.csv;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCsvDto {

    @CsvBindByName(column = "Name")
    private String name;

    @CsvBindByName(column = "Description")
    private String description;

    @CsvBindByName(column = "Icon")
    private String icon;

    @CsvBindByName(column = "Type")
    private String typeName;
}
