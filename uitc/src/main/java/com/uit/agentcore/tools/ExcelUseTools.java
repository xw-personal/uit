package com.uit.agentcore.tools;

import org.apache.fesod.sheet.FesodSheet;

import com.uit.api.common.ExcelReadListener;
import com.uit.api.entry.CaseData;

import dev.langchain4j.agent.tool.Tool;

public class ExcelUseTools {

    @Tool
    public void read(String fileName){
        ExcelReadListener excelReadListener = new ExcelReadListener();
        FesodSheet.read(fileName, CaseData.class, excelReadListener)
            .sheet()
            .doRead();
        
    }
}
