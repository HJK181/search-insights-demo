package com.example.searchinsightsdemo.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.searchinsightsdemo.rest.dto.AnalyticsRequest;
import com.example.searchinsightsdemo.rest.dto.ChartData;
import com.example.searchinsightsdemo.service.AthenaQueryService;

@RestController
@RequestMapping("/insights")
public class AthenaQueryController {

	@Autowired
	private AthenaQueryService queryService;

	@GetMapping("/count")
	public ResponseEntity<Integer> getCount() {

		return ResponseEntity.ok(queryService.getCount());
	}

	@GetMapping("/ctr")
	public ResponseEntity<ChartData> getCTR(@Valid AnalyticsRequest request) {

		return ResponseEntity.ok(queryService.getCTR(request));
	}

	@GetMapping("/cr")
	public ResponseEntity<ChartData> getCR(@Valid AnalyticsRequest request) {
		
		return ResponseEntity.ok(queryService.getCR(request));
	}
}
