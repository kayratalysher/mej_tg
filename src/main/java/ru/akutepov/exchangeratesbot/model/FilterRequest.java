package ru.akutepov.exchangeratesbot.model;

import lombok.Data;

import java.util.Map;

@Data
public class FilterRequest {

    private Map<String, Object> filters;
}
