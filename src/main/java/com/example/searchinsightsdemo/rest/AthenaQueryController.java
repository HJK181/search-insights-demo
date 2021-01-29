package com.example.searchinsightsdemo.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
