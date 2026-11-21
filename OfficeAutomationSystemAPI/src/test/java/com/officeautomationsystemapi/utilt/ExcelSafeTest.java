package com.officeautomationsystemapi.utilt;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

// 覆盖导出注入防护：以 = + - @ 开头的文本必须被前缀单引号，普通文本不受影响
class ExcelSafeTest {

    @Test
    void sanitizePrefixesDangerousText() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("s");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("=cmd|' /C calc'!A0");
            row.createCell(1).setCellValue("+1+1");
            row.createCell(2).setCellValue("-2");
            row.createCell(3).setCellValue("@SUM(A1)");
            row.createCell(4).setCellValue("正常文本");
            row.createCell(5).setCellValue(12.5);

            ExcelSafe.sanitize(workbook);

            assertEquals("'=cmd|' /C calc'!A0", row.getCell(0).getStringCellValue());
            assertEquals("'+1+1", row.getCell(1).getStringCellValue());
            assertEquals("'-2", row.getCell(2).getStringCellValue());
            assertEquals("'@SUM(A1)", row.getCell(3).getStringCellValue());
            assertEquals("正常文本", row.getCell(4).getStringCellValue());
            assertEquals(12.5, row.getCell(5).getNumericCellValue(), 0.0001);
        }
    }

    @Test
    void sanitizeIgnoresNullWorkbook() {
        ExcelSafe.sanitize(null);
    }
}
