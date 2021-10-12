package com.example.searchinsightsdemo.rest.dto;

import java.util.Collection;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class ChartData {

	@Singular
	Collection<String>	labels;
	@Singular(value = "data")
	Collection<Number>	data;
}
