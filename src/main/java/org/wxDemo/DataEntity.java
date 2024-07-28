package org.wxDemo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
class DataEntity {
    @ExcelProperty("号码1")
    private String column1;
    @ExcelProperty("号码2")
    private String column2;
    @ExcelProperty("号码3")
    private String column3;
    @ExcelProperty("号码4")
    private String column4;
    @ExcelProperty("号码5")
    private String column5;
}