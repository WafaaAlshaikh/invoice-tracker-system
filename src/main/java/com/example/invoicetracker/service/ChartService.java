package com.example.invoicetracker.service;

import com.example.invoicetracker.model.entity.Category;
import com.example.invoicetracker.model.entity.Product;
import com.example.invoicetracker.repository.CategoryRepository;
import com.example.invoicetracker.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import org.knowm.xchart.PieChart;
import org.knowm.xchart.PieChartBuilder;
import org.knowm.xchart.BitmapEncoder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChartService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public byte[] generateProductByCategoryPieChart() throws IOException {
        List<Product> products = productRepository.findAll();
        List<Category> categories = categoryRepository.findAll();

        Map<String, Long> countByCategory = categories.stream()
                .collect(Collectors.toMap(
                        Category::getCategoryName,
                        cat -> products.stream()
                                .filter(p -> p.getCategory().getCategoryId().equals(cat.getCategoryId()))
                                .count()));

        PieChart chart = new PieChartBuilder()
                .width(800).height(600)
                .title("Products by Category")
                .build();

        countByCategory.forEach((category, count) -> {
            if (count > 0)
                chart.addSeries(category, count);
        });

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitmapEncoder.saveBitmap(chart, baos, BitmapEncoder.BitmapFormat.PNG);
        return baos.toByteArray();
    }

}
