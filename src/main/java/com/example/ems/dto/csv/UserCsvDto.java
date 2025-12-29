package com.example.ems.dto.csv;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserCsvDto {

    @CsvBindByName(column = "FULL NAME")
    private String name;

    @CsvBindByName(column = "EMAIL ADDRESS")
    private String email;

    @CsvBindByName(column = "ROLE")
    private String roleName; 

    @CsvBindByName(column = "ACTIVE")
    private boolean isActive;
}
