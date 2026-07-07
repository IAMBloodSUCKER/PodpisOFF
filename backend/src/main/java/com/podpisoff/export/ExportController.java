package com.podpisoff.export;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping(value = "/subscriptions.csv", produces = "text/csv")
    public ResponseEntity<String> subscriptionsCsv() {
        String content = exportService.exportSubscriptionsCsv();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"podpisoff-subscriptions.csv\"")
            .body(content);
    }

    @GetMapping(
        value = "/subscriptions.xlsx",
        produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )
    public ResponseEntity<byte[]> subscriptionsExcel() {
        byte[] content = exportService.exportSubscriptionsExcel();
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            ))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"podpisoff-subscriptions.xlsx\"")
            .body(content);
    }
}
