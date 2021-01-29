package com.example.searchinsightsdemo.service;

import static com.example.searchinsightsdemo.db.tables.Analytics.ANALYTICS;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AthenaQueryService {

	@Autowired
	private DSLContext context;

	public int getCount() {
		return context.fetchCount(ANALYTICS);
	}
}
