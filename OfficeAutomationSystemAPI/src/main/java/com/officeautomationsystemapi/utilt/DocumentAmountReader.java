package com.officeautomationsystemapi.utilt;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 从附件里猜金额，只做提示，最终由填单人确认
// 支持 txt/md/csv/log/json、docx、xlsx/xls，图片和 PDF 返回 null
public class DocumentAmountReader {

    // 金额关键词后紧跟的数字，最可信
    private static final Pattern KEYWORD_AMOUNT = Pattern.compile(
            "(?:报销金额|申请金额|合计金额|总金额|金额|合计|总计|小计|总额|总价|费用|价款|价税合计)[^0-9\\n]{0,10}([0-9]{1,9}(?:\\.[0-9]{1,2})?)");
    // 数字后面带元或人民币
    private static final Pattern SUFFIX_AMOUNT = Pattern.compile(
            "([0-9]{1,9}(?:\\.[0-9]{1,2})?)\\s*(?:元|人民币|RMB)");
    // 带货币符号前缀
    private static final Pattern PREFIX_AMOUNT = Pattern.compile(
            "[￥¥]\\s*([0-9]{1,9}(?:\\.[0-9]{1,2})?)");
    // 最后看两位小数的数字，取最大的
    private static final Pattern DECIMAL_AMOUNT = Pattern.compile("([0-9]{1,9}\\.[0-9]{2})");

    public static class AmountHit {
        private final double amount;
        private final String source;

        public AmountHit(double amount, String source) {
            this.amount = amount;
            this.source = source;
        }

        public double getAmount() {
            return amount;
        }

        public String getSource() {
            return source;
        }
    }

    // 读不到返回 null，调用方当作没有建议
    public static AmountHit read(File file, String ext) {
        if (file == null || !file.exists()) return null;
        String e = ext == null ? "" : ext.toLowerCase(Locale.ROOT);
        String text;
        try {
            if (e.equals("txt") || e.equals("md") || e.equals("csv") || e.equals("log") || e.equals("json")) {
                text = readText(file);
            } else if (e.equals("docx")) {
                text = readDocx(file);
            } else if (e.equals("xlsx") || e.equals("xls")) {
                text = readExcel(file);
            } else {
                return null;
            }
        } catch (Exception ex) {
            // 加密或损坏的文件会抛异常，一律返回 null
            return null;
        }
        return guess(text);
    }

    // 纯文本最多读 200KB
    private static String readText(File file) throws Exception {
        byte[] bytes = Files.readAllBytes(file.toPath());
        int len = Math.min(bytes.length, 200 * 1024);
        return new String(bytes, 0, len, StandardCharsets.UTF_8);
    }

    private static String readDocx(File file) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream in = new FileInputStream(file); XWPFDocument doc = new XWPFDocument(in)) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                sb.append(p.getText()).append('\n');
            }
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    List<String> cells = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        cells.add(cell.getText());
                    }
                    // 同一行用空格连起来，关键词正则才匹配得到
                    sb.append(String.join(" ", cells)).append('\n');
                }
            }
        }
        return sb.toString();
    }

    private static String readExcel(File file) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream in = new FileInputStream(file); Workbook wb = WorkbookFactory.create(in)) {
            for (Sheet sheet : wb) {
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    for (Cell cell : row) {
                        if (cell.getCellType() == CellType.NUMERIC) {
                            cells.add(trimNumber(cell.getNumericCellValue()));
                        } else if (cell.getCellType() == CellType.STRING) {
                            cells.add(cell.getStringCellValue());
                        }
                    }
                    if (!cells.isEmpty()) sb.append(String.join(" ", cells)).append('\n');
                }
            }
        }
        return sb.toString();
    }

    private static String trimNumber(double value) {
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(Math.round(value * 100.0) / 100.0);
    }

    // 按可信度从高到低取第一个命中，带上上下文
    public static AmountHit guess(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        AmountHit hit = firstHit(KEYWORD_AMOUNT, text);
        if (hit == null) hit = firstHit(SUFFIX_AMOUNT, text);
        if (hit == null) hit = firstHit(PREFIX_AMOUNT, text);
        if (hit == null) hit = largestDecimal(text);
        return hit;
    }

    private static AmountHit firstHit(Pattern pattern, String text) {
        Matcher m = pattern.matcher(text);
        if (!m.find()) return null;
        try {
            double amount = Double.parseDouble(m.group(1));
            if (amount <= 0 || amount > 99999999) return null;
            return new AmountHit(amount, snippet(text, m.start(), m.end()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static AmountHit largestDecimal(String text) {
        Matcher m = DECIMAL_AMOUNT.matcher(text);
        double best = -1;
        int start = 0;
        int end = 0;
        while (m.find()) {
            double v = Double.parseDouble(m.group(1));
            if (v > best && v <= 99999999) {
                best = v;
                start = m.start();
                end = m.end();
            }
        }
        if (best <= 0) return null;
        return new AmountHit(best, snippet(text, start, end));
    }

    // 取前后各 20 个字符，换行压成空格，当前端提示
    private static String snippet(String text, int start, int end) {
        int from = Math.max(0, start - 20);
        int to = Math.min(text.length(), end + 20);
        return text.substring(from, to).replaceAll("\\s+", " ").trim();
    }
}
