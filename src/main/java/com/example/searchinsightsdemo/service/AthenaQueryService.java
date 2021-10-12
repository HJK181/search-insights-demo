package com.example.searchinsightsdemo.service;

import static com.example.searchinsightsdemo.db.tables.Analytics.ANALYTICS;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.nullif;
import static org.jooq.impl.DSL.sum;
import static org.jooq.impl.SQLDataType.DECIMAL;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.jooq.AggregateFunction;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.exception.DataTypeException;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.searchinsightsdemo.rest.dto.AnalyticsRequest;
import com.example.searchinsightsdemo.rest.dto.ChartData;
import com.example.searchinsightsdemo.rest.dto.ChartData.ChartDataBuilder;

@Service
public class AthenaQueryService {

	@Autowired
	private DSLContext context;

	public int getCount() {
		return context.fetchCount(ANALYTICS);
	}

	public ChartData getCR(AnalyticsRequest request) {
		Field<BigDecimal> crField = saveDiv(sum(ANALYTICS.TRANSACTIONS), sum(ANALYTICS.SEARCHES), new BigDecimal(0));

		return getKPI(request, crField);
	}

	private ChartData getKPI(AnalyticsRequest request, Field<BigDecimal> field) {
		ChartDataBuilder chartDataBuilder = ChartData.builder();
		context.select(ANALYTICS.DT, field)
				.from(ANALYTICS)
				.where(partitionedBetween(request))
				.groupBy(ANALYTICS.DT)
				.orderBy(ANALYTICS.DT.desc())
				.fetch()
				.forEach(rs -> {
					try {
						chartDataBuilder.label(LocalDate.parse(rs.get(0, String.class)).toString());
						chartDataBuilder.data(rs.getValue(1, Double.class) * 100);
					}
					catch (DataTypeException | IllegalArgumentException e) {
						throw new IllegalArgumentException(e);
					}
				});

		return chartDataBuilder.build();
	}

	public ChartData getCTR(AnalyticsRequest request) {
		Field<BigDecimal> ctrField = saveDiv(sum(ANALYTICS.CLICKS), sum(ANALYTICS.SEARCHES), new BigDecimal(0));
		return getKPI(request, ctrField);
	}

	private Field<BigDecimal> saveDiv(AggregateFunction<BigDecimal> dividend, AggregateFunction<BigDecimal> divisor, BigDecimal defaultValue) {
		return coalesce(dividend.cast(DECIMAL.precision(18, 3)).div(nullif(divisor, new BigDecimal(0))), defaultValue);
	}

	private Condition partitionedBetween(AnalyticsRequest request) {
		Condition condition = DSL.trueCondition();
		if (request.getFrom() != null) {
			condition = condition.and(ANALYTICS.DT.greaterOrEqual(request.getFrom().toString()));
		}
		if (request.getTo() != null) {
			condition = condition.and(ANALYTICS.DT.lessOrEqual(request.getTo().toString()));
		}
		return condition;
	}
}
