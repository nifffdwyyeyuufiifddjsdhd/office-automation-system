package com.officeautomationsystemapi.utilt;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

// 导出前清理单元格，= + - @ 开头会被 Excel 当公式，加单引号
public final class ExcelSafe {

    private ExcelSafe() {
    }

    private static final char[] RISK = {'=', '+', '-', '@', '\t', '\r'};

    public static void sanitize(Workbook workbook) {
        if (workbook == null) {
            return;
        }
        for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
            Sheet sheet = workbook.getSheetAt(s);
            if (sheet == null) {
                continue;
            }
            for (Row row : sheet) {
                if (row == null) {
                    continue;
                }
                for (Cell cell : row) {
                    if (cell == null || cell.getCellType() != CellType.STRING) {
                        continue;
                    }
                    String value = cell.getStringCellValue();
                    if (value == null || value.isEmpty()) {
                        continue;
                    }
                    char first = value.charAt(0);
                    boolean risk = false;
                    for (char c : RISK) {
                        if (first == c) {
                            risk = true;
                            break;
                        }
                    }
                    if (risk) {
                        cell.setCellValue("'" + value);
                    }
                }
            }
        }
    }
}
