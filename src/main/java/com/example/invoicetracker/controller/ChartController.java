package com.example.invoicetracker.controller;

import com.example.invoicetracker.service.ChartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class ChartController {

    private final ChartService chartService;

    @GetMapping(value = "/charts/products-by-category", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getProductsByCategoryChart() throws IOException {
        byte[] chartImage = chartService.generateProductByCategoryPieChart();
        return ResponseEntity.ok().body(chartImage);
    }

}