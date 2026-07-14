package com.uit.api.common;

import org.apache.fesod.sheet.context.AnalysisContext;
import org.apache.fesod.sheet.read.listener.ReadListener;

import com.uit.api.entry.CaseData;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExcelReadListener implements ReadListener<CaseData> {

    @Override
    public void invoke(CaseData data, AnalysisContext context) {
        log.info("读取到一条数据: {}", data);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        log.info("所有数据读取完成！");
    }
}
