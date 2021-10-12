package com.example.searchinsightsdemo.rest.dto;

import java.time.LocalDate;

import javax.validation.constraints.AssertTrue;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnalyticsRequest {

	@DateTimeFormat(iso = ISO.DATE)
	private LocalDate	from;
	@DateTimeFormat(iso = ISO.DATE)
	private LocalDate	to;

	@AssertTrue
	public boolean isValidDateRange() {
		return from != null && to != null && !to.isBefore(from);
	}
}
