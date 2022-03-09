package com.example.demo.entity;

import lombok.Data;

import java.util.List;

@Data
public class Bucket {
    private List<String> dates;
    private List<String> values;
    private List<String> chainGrowthRate;
}
