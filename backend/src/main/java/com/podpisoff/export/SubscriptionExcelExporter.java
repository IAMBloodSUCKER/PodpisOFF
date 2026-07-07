package com.podpisoff.export;

import com.podpisoff.subscription.BillingPeriodMath;
import com.podpisoff.subscription.Subscription;
import com.podpisoff.user.LocaleCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class SubscriptionExcelExporter {

    private static final DateTimeFormatter EXPORT_STAMP_RU = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter EXPORT_STAMP_EN = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm");

    private SubscriptionExcelExporter() {
    }

    public static byte[] export(List<Subscription> subscriptions, LocaleCode locale) throws IOException {
        boolean russian = locale == LocaleCode.RU;
        try (Workbook workbook = new XSSFWorkbook()) {
            ExportStyles styles = ExportStyles.create(workbook, russian);
            Sheet sheet = workbook.createSheet(ExportLabels.sheetData(locale));
            int rowIndex = 0;

            Row titleRow = sheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(ExportLabels.title(locale));
            titleCell.setCellStyle(styles.title);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

            Row metaRow = sheet.createRow(rowIndex++);
            Cell metaCell = metaRow.createCell(0);
            String stamp = russian
                ? LocalDateTime.now().format(EXPORT_STAMP_RU)
                : LocalDateTime.now().format(EXPORT_STAMP_EN);
            metaCell.setCellValue(ExportLabels.exportedAt(locale) + " " + stamp);
            metaCell.setCellStyle(styles.muted);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 10));

            rowIndex++;

            int headerRowIndex = rowIndex;
            Row headerRow = sheet.createRow(rowIndex++);
            String[] columns = ExportLabels.columns(locale);
            for (int col = 0; col < columns.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(columns[col]);
                cell.setCellStyle(styles.header);
            }

            int firstDataRowIndex = rowIndex;
            int dataRowNumber = 0;
            for (Subscription subscription : subscriptions) {
                Row row = sheet.createRow(rowIndex++);
                boolean zebra = dataRowNumber % 2 == 1;
                writeDataRow(row, dataRowNumber + 1, subscription, locale, styles, zebra);
                dataRowNumber++;
            }

            int lastDataRowIndex = rowIndex - 1;
            if (!subscriptions.isEmpty()) {
                rowIndex++;
                writeTotalsBlock(
                    sheet,
                    rowIndex,
                    firstDataRowIndex,
                    lastDataRowIndex,
                    locale,
                    styles,
                    subscriptions
                );
                sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, lastDataRowIndex, 0, 10));
                sheet.createFreezePane(0, firstDataRowIndex);
            }

            autosizeColumns(sheet, 11);
            sheet.setColumnWidth(1, 5200);
            sheet.setColumnWidth(2, 4800);
            sheet.setColumnWidth(4, 3200);
            sheet.setColumnWidth(7, 5200);
            sheet.setColumnWidth(8, 3200);
            sheet.setColumnWidth(9, 7000);
            sheet.setColumnWidth(10, 9000);

            createSummarySheet(workbook, subscriptions, locale, styles);

            workbook.setForceFormulaRecalculation(true);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            evaluator.evaluateAll();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static void writeDataRow(
        Row row,
        int index,
        Subscription subscription,
        LocaleCode locale,
        ExportStyles styles,
        boolean zebra
    ) {
        CellStyle textStyle = zebra ? styles.textZebra : styles.text;
        CellStyle moneyStyle = zebra ? styles.moneyZebra : styles.money;
        CellStyle dateStyle = zebra ? styles.dateZebra : styles.date;

        setCell(row, 0, index, textStyle);
        setCell(row, 1, subscription.getTitle(), textStyle);
        setCell(row, 2, subscription.getCategory(), textStyle);
        setMoney(row, 3, subscription.getAmount(), moneyStyle);
        setCell(row, 4, subscription.getCurrency(), textStyle);
        setCell(row, 5, ExportLabels.period(subscription.getBillingPeriod(), locale), textStyle);

        BigDecimal monthly = BillingPeriodMath.monthlyBurn(
            subscription.getAmount(),
            subscription.getBillingPeriod()
        );
        setMoney(row, 6, monthly, moneyStyle);

        Cell dateCell = row.createCell(7);
        dateCell.setCellValue(subscription.getNextBillingDate());
        dateCell.setCellStyle(dateStyle);

        setCell(row, 8, ExportLabels.active(subscription.isActive(), locale), textStyle);
        setCell(row, 9, subscription.getNote() == null ? "" : subscription.getNote(), textStyle);

        String url = subscription.getResourceUrl();
        Cell linkCell = row.createCell(10);
        if (url != null && !url.isBlank()) {
            linkCell.setCellValue(url);
            linkCell.setCellStyle(styles.link);
            CreationHelper helper = row.getSheet().getWorkbook().getCreationHelper();
            Hyperlink hyperlink = helper.createHyperlink(HyperlinkType.URL);
            hyperlink.setAddress(url);
            linkCell.setHyperlink(hyperlink);
        } else {
            linkCell.setCellValue("");
            linkCell.setCellStyle(textStyle);
        }
    }

    private static void writeTotalsBlock(
        Sheet sheet,
        int startRow,
        int firstDataRowIndex,
        int lastDataRowIndex,
        LocaleCode locale,
        ExportStyles styles,
        List<Subscription> subscriptions
    ) {
        int excelFirst = firstDataRowIndex + 1;
        int excelLast = lastDataRowIndex + 1;
        String yes = ExportLabels.activeYes(locale);
        Map<String, BigDecimal> monthlyByCurrency = new LinkedHashMap<>();

        Row title = sheet.createRow(startRow);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue(ExportLabels.summaryTitle(locale));
        titleCell.setCellStyle(styles.summaryTitle);
        sheet.addMergedRegion(new CellRangeAddress(startRow, startRow, 0, 3));

        Row countRow = sheet.createRow(startRow + 1);
        countRow.createCell(0).setCellValue(ExportLabels.summaryCount(locale));
        countRow.getCell(0).setCellStyle(styles.summaryLabel);
        Cell countValue = countRow.createCell(1);
        long activeCount = subscriptions.stream().filter(Subscription::isActive).count();
        setFormulaCell(countValue, String.format("COUNTIF(I%d:I%d,\"%s\")", excelFirst, excelLast, yes), activeCount);
        countValue.setCellStyle(styles.summaryValue);

        Set<String> currencies = new LinkedHashSet<>();
        subscriptions.stream().filter(Subscription::isActive).forEach(subscription -> {
            currencies.add(subscription.getCurrency());
            BigDecimal monthly = BillingPeriodMath.monthlyBurn(
                subscription.getAmount(),
                subscription.getBillingPeriod()
            );
            monthlyByCurrency.merge(subscription.getCurrency(), monthly, BigDecimal::add);
        });

        int offset = 2;
        for (String currency : currencies) {
            Row totalRow = sheet.createRow(startRow + offset);
            totalRow.createCell(0).setCellValue(ExportLabels.summaryMonthly(locale, currency));
            totalRow.getCell(0).setCellStyle(styles.summaryLabel);
            Cell totalCell = totalRow.createCell(1);
            BigDecimal fallback = monthlyByCurrency.getOrDefault(currency, BigDecimal.ZERO);
            setFormulaCell(totalCell, String.format(
                "SUMIFS(G%d:G%d,E%d:E%d,\"%s\",I%d:I%d,\"%s\")",
                excelFirst, excelLast,
                excelFirst, excelLast, currency,
                excelFirst, excelLast, yes
            ), fallback.doubleValue());
            totalCell.setCellStyle(styles.summaryMoney);
            offset++;
        }

        if (ExportApproxRates.needsRubEquivalent(monthlyByCurrency)) {
            Row approxRow = sheet.createRow(startRow + offset);
            approxRow.createCell(0).setCellValue(ExportLabels.summaryApproxRub(locale));
            approxRow.getCell(0).setCellStyle(styles.summaryLabel);
            setMoney(approxRow, 1, ExportApproxRates.approxMonthlyRub(monthlyByCurrency), styles.summaryMoney);
        }
    }

    private static void setFormulaCell(Cell cell, String formula, double fallback) {
        cell.setCellFormula(formula);
        try {
            FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
            CellType resultType = evaluator.evaluateFormulaCell(cell);
            if (resultType != CellType.NUMERIC && resultType != CellType.ERROR) {
                cell.setCellValue(fallback);
            }
        } catch (RuntimeException exception) {
            cell.setCellValue(fallback);
        }
    }

    private static void createSummarySheet(
        Workbook workbook,
        List<Subscription> subscriptions,
        LocaleCode locale,
        ExportStyles styles
    ) {
        Sheet sheet = workbook.createSheet(ExportLabels.sheetSummary(locale));
        Row title = sheet.createRow(0);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue(ExportLabels.summaryByCategory(locale));
        titleCell.setCellStyle(styles.title);

        Row header = sheet.createRow(2);
        header.createCell(0).setCellValue(ExportLabels.categoryHeader(locale));
        header.getCell(0).setCellStyle(styles.header);
        header.createCell(1).setCellValue(ExportLabels.totalHeader(locale));
        header.getCell(1).setCellStyle(styles.header);
        header.createCell(2).setCellValue(locale == LocaleCode.RU ? "Валюта" : "Currency");
        header.getCell(2).setCellStyle(styles.header);

        Map<String, Map<String, BigDecimal>> byCategory = new LinkedHashMap<>();
        subscriptions.stream()
            .filter(Subscription::isActive)
            .forEach(subscription -> byCategory
                .computeIfAbsent(subscription.getCategory(), key -> new LinkedHashMap<>())
                .merge(
                    subscription.getCurrency(),
                    BillingPeriodMath.monthlyBurn(subscription.getAmount(), subscription.getBillingPeriod()),
                    BigDecimal::add
                ));

        int rowIndex = 3;
        Map<String, BigDecimal> currencyTotals = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, BigDecimal>> entry : byCategory.entrySet()) {
            for (Map.Entry<String, BigDecimal> currencyTotal : entry.getValue().entrySet()) {
                Row row = sheet.createRow(rowIndex++);
                boolean zebra = (rowIndex % 2) == 0;
                CellStyle textStyle = zebra ? styles.textZebra : styles.text;
                CellStyle moneyStyle = zebra ? styles.moneyZebra : styles.money;
                row.createCell(0).setCellValue(entry.getKey());
                row.getCell(0).setCellStyle(textStyle);
                setMoney(row, 1, currencyTotal.getValue(), moneyStyle);
                row.createCell(2).setCellValue(currencyTotal.getKey());
                row.getCell(2).setCellStyle(textStyle);
                currencyTotals.merge(currencyTotal.getKey(), currencyTotal.getValue(), BigDecimal::add);
            }
        }

        if (!currencyTotals.isEmpty()) {
            rowIndex++;
            Row totalsTitle = sheet.createRow(rowIndex++);
            totalsTitle.createCell(0).setCellValue(ExportLabels.summaryByCurrency(locale));
            totalsTitle.getCell(0).setCellStyle(styles.summaryTitle);
            sheet.addMergedRegion(new CellRangeAddress(totalsTitle.getRowNum(), totalsTitle.getRowNum(), 0, 2));

            for (Map.Entry<String, BigDecimal> total : currencyTotals.entrySet()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(ExportLabels.summaryMonthly(locale, total.getKey()));
                row.getCell(0).setCellStyle(styles.summaryLabel);
                setMoney(row, 1, total.getValue(), styles.summaryMoney);
                row.createCell(2).setCellValue(total.getKey());
                row.getCell(2).setCellStyle(styles.text);
            }

            if (ExportApproxRates.needsRubEquivalent(currencyTotals)) {
                Row approxRow = sheet.createRow(rowIndex);
                approxRow.createCell(0).setCellValue(ExportLabels.summaryApproxRub(locale));
                approxRow.getCell(0).setCellStyle(styles.summaryLabel);
                setMoney(approxRow, 1, ExportApproxRates.approxMonthlyRub(currencyTotals), styles.summaryMoney);
            }
        }

        autosizeColumns(sheet, 3);
    }

    private static void setCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void setCell(Row row, int col, long value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void setMoney(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    private static void autosizeColumns(Sheet sheet, int count) {
        for (int col = 0; col < count; col++) {
            sheet.autoSizeColumn(col);
            int width = sheet.getColumnWidth(col);
            sheet.setColumnWidth(col, Math.min(width + 512, 12000));
        }
    }

    private static final class ExportStyles {
        private final CellStyle title;
        private final CellStyle muted;
        private final CellStyle header;
        private final CellStyle text;
        private final CellStyle textZebra;
        private final CellStyle money;
        private final CellStyle moneyZebra;
        private final CellStyle date;
        private final CellStyle dateZebra;
        private final CellStyle link;
        private final CellStyle summaryTitle;
        private final CellStyle summaryLabel;
        private final CellStyle summaryValue;
        private final CellStyle summaryMoney;

        private ExportStyles(
            CellStyle title,
            CellStyle muted,
            CellStyle header,
            CellStyle text,
            CellStyle textZebra,
            CellStyle money,
            CellStyle moneyZebra,
            CellStyle date,
            CellStyle dateZebra,
            CellStyle link,
            CellStyle summaryTitle,
            CellStyle summaryLabel,
            CellStyle summaryValue,
            CellStyle summaryMoney
        ) {
            this.title = title;
            this.muted = muted;
            this.header = header;
            this.text = text;
            this.textZebra = textZebra;
            this.money = money;
            this.moneyZebra = moneyZebra;
            this.date = date;
            this.dateZebra = dateZebra;
            this.link = link;
            this.summaryTitle = summaryTitle;
            this.summaryLabel = summaryLabel;
            this.summaryValue = summaryValue;
            this.summaryMoney = summaryMoney;
        }

        private static ExportStyles create(Workbook workbook, boolean russian) {
            DataFormat dataFormat = workbook.createDataFormat();
            String moneyPlain = "#,##0.00";
            String dateFormat = russian ? "dd.mm.yyyy" : "mm/dd/yyyy";

            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.WHITE.getIndex());

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            Font linkFont = workbook.createFont();
            linkFont.setUnderline(Font.U_SINGLE);
            linkFont.setColor(IndexedColors.BLUE.getIndex());

            XSSFColor brand = new XSSFColor(new byte[] {(byte) 108, (byte) 92, (byte) 231}, null);
            XSSFColor brandDark = new XSSFColor(new byte[] {(byte) 45, (byte) 52, (byte) 54}, null);
            XSSFColor zebra = new XSSFColor(new byte[] {(byte) 248, (byte) 247, (byte) 255}, null);
            XSSFColor summaryBg = new XSSFColor(new byte[] {(byte) 255, (byte) 234, (byte) 167}, null);

            CellStyle title = workbook.createCellStyle();
            title.setFont(titleFont);
            applyFill((XSSFCellStyle) title, brandDark);
            title.setAlignment(HorizontalAlignment.LEFT);
            title.setVerticalAlignment(VerticalAlignment.CENTER);
            title.setIndention((short) 1);

            CellStyle muted = workbook.createCellStyle();
            Font mutedFont = workbook.createFont();
            mutedFont.setItalic(true);
            mutedFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            muted.setFont(mutedFont);

            CellStyle header = workbook.createCellStyle();
            header.setFont(headerFont);
            applyFill((XSSFCellStyle) header, brand);
            header.setAlignment(HorizontalAlignment.CENTER);
            header.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorder(header);

            CellStyle text = baseText(workbook);
            CellStyle textZebra = baseText(workbook);
            applyFill((XSSFCellStyle) textZebra, zebra);

            CellStyle money = baseText(workbook);
            money.setDataFormat(dataFormat.getFormat(moneyPlain));
            money.setAlignment(HorizontalAlignment.RIGHT);
            CellStyle moneyZebra = baseText(workbook);
            applyFill((XSSFCellStyle) moneyZebra, zebra);
            moneyZebra.setDataFormat(dataFormat.getFormat(moneyPlain));
            moneyZebra.setAlignment(HorizontalAlignment.RIGHT);

            CellStyle date = baseText(workbook);
            date.setDataFormat(dataFormat.getFormat(dateFormat));
            CellStyle dateZebra = baseText(workbook);
            applyFill((XSSFCellStyle) dateZebra, zebra);
            dateZebra.setDataFormat(dataFormat.getFormat(dateFormat));

            CellStyle link = baseText(workbook);
            link.setFont(linkFont);

            CellStyle summaryTitle = workbook.createCellStyle();
            Font summaryFont = workbook.createFont();
            summaryFont.setBold(true);
            summaryTitle.setFont(summaryFont);
            applyFill((XSSFCellStyle) summaryTitle, summaryBg);
            applyBorder(summaryTitle);

            CellStyle summaryLabel = workbook.createCellStyle();
            summaryLabel.setFont(summaryFont);
            applyBorder(summaryLabel);

            CellStyle summaryValue = workbook.createCellStyle();
            summaryValue.setDataFormat(dataFormat.getFormat("0"));
            summaryValue.setAlignment(HorizontalAlignment.RIGHT);
            applyBorder(summaryValue);

            CellStyle summaryMoney = workbook.createCellStyle();
            summaryMoney.setDataFormat(dataFormat.getFormat(moneyPlain));
            summaryMoney.setAlignment(HorizontalAlignment.RIGHT);
            applyFill((XSSFCellStyle) summaryMoney, summaryBg);
            applyBorder(summaryMoney);

            return new ExportStyles(
                title, muted, header, text, textZebra, money, moneyZebra, date, dateZebra, link,
                summaryTitle, summaryLabel, summaryValue, summaryMoney
            );
        }

        private static CellStyle baseText(Workbook workbook) {
            CellStyle style = workbook.createCellStyle();
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            applyBorder(style);
            return style;
        }

        private static void applyFill(XSSFCellStyle style, XSSFColor color) {
            style.setFillForegroundColor(color);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        private static void applyBorder(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        }
    }
}
