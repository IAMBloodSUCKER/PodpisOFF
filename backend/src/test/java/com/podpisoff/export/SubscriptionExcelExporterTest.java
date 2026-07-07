package com.podpisoff.export;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.podpisoff.subscription.BillingPeriod;
import com.podpisoff.subscription.Subscription;
import com.podpisoff.user.LocaleCode;
import com.podpisoff.user.User;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class SubscriptionExcelExporterTest {

    @Test
    void russianWorkbookHasLocalizedSheetsAndHeaders() throws Exception {
        byte[] bytes = SubscriptionExcelExporter.export(java.util.List.of(sampleSubscription()), LocaleCode.RU);

        assertTrue(bytes.length > 0);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals("Подписки", workbook.getSheetAt(0).getSheetName());
            assertEquals("Сводка", workbook.getSheetAt(1).getSheetName());
            assertEquals("Название", workbook.getSheetAt(0).getRow(3).getCell(1).getStringCellValue());
            assertEquals("cursor", workbook.getSheetAt(0).getRow(4).getCell(1).getStringCellValue());
            assertEquals(60.0, workbook.getSheetAt(0).getRow(4).getCell(6).getNumericCellValue(), 0.001);
            assertEquals(1.0, workbook.getSheetAt(0).getRow(7).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(60.0, workbook.getSheetAt(0).getRow(8).getCell(1).getNumericCellValue(), 0.001);
        }
    }

    @Test
    void totalsUseFormulasWithCachedValuesForMultipleCurrencies() throws Exception {
        Subscription usd = sampleSubscription();
        Subscription cny = sampleSubscription();
        cny.setTitle("wewerwerwer");
        cny.setCategory("Музыка");
        cny.setAmount(new BigDecimal("2333"));
        cny.setCurrency("CNY");
        cny.setNextBillingDate(LocalDate.of(2026, 8, 6));

        byte[] bytes = SubscriptionExcelExporter.export(java.util.List.of(usd, cny), LocaleCode.RU);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals(CellType.FORMULA, sheet.getRow(8).getCell(1).getCellType());
            assertEquals(2.0, sheet.getRow(8).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(60.0, sheet.getRow(9).getCell(1).getNumericCellValue(), 0.001);
            assertEquals(2333.0, sheet.getRow(10).getCell(1).getNumericCellValue(), 0.001);
            assertTrue(sheet.getRow(11).getCell(1).getNumericCellValue() > 30_000);
        }
    }

    @Test
    void englishWorkbookUsesEnglishHeaders() throws Exception {
        byte[] bytes = SubscriptionExcelExporter.export(java.util.List.of(sampleSubscription()), LocaleCode.EN);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertEquals("Subscriptions", workbook.getSheetAt(0).getSheetName());
            assertEquals("Name", workbook.getSheetAt(0).getRow(3).getCell(1).getStringCellValue());
        }
    }

    private Subscription sampleSubscription() {
        Subscription subscription = new Subscription();
        subscription.setTitle("cursor");
        subscription.setCategory("Софт и VPN");
        subscription.setAmount(new BigDecimal("60.00"));
        subscription.setCurrency("USD");
        subscription.setNextBillingDate(LocalDate.of(2026, 7, 9));
        subscription.setBillingPeriod(BillingPeriod.MONTHLY);
        subscription.setActive(true);
        User user = new User();
        user.setUsername("test");
        subscription.setUser(user);
        return subscription;
    }
}
