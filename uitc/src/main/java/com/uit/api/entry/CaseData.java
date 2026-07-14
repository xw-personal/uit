package com.uit.api.entry;

import lombok.Data;

@Data
public class CaseData {

    private String id;

    private String tital;

    private String moduleName;

    private String priority;

    private String precondition;

    private String step;

    private String value;

    private String expectedResult;

    private String actualResult;
}
