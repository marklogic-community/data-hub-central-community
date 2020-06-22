package com.marklogic.grove.boot.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.marklogic.client.query.StructuredQueryBuilder;
import com.marklogic.client.query.StructuredQueryDefinition;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class SearchService {
	/**
	 * Uses a QueryBuilderAndCriteria object so that a criteria string can be captured (if needed) and then applied
	 * against the overall StructuredQueryDefinition.
	 *
	 * @param queryBuilder
	 * @param filters
	 * @return
	 */
	public StructuredQueryDefinition buildQueryWithCriteria(StructuredQueryBuilder queryBuilder, JsonNode filters) {
		QueryBuilderAndCriteria builder = new QueryBuilderAndCriteria(queryBuilder);
		StructuredQueryDefinition query = buildQuery(builder, filters);
		if (builder.criteria != null) {
			query.setCriteria(builder.criteria);
		}
		return query;
	}

	public StructuredQueryDefinition buildQuery(QueryBuilderAndCriteria builder, JsonNode filters) {
		if (filters.has("and")) {
			JsonNode and = filters.get("and");
			return builder.queryBuilder.and(arrayFromNode(builder, and));
		}
		else if (filters.has("or")) {
			JsonNode or = filters.get("or");
			return builder.queryBuilder.or(arrayFromNode(builder, or));
		}
		else if (filters.has("not")) {
			JsonNode not = filters.get("not");
			return builder.queryBuilder.not(buildQuery(builder, not));
		}
		else if (filters.has("near")) {
			JsonNode near = filters.get("near");
			return builder.queryBuilder.near(arrayFromNode(builder, near));
		}
		else {
			String type = filters.has("type") ? filters.get("type").asText() : "selection";
			if (type.equals("queryText")) {
				if (filters.has("constraint")) {
					return createConstraint(builder, filters.get("constraintType").asText(), filters.get("constraint").asText(), "EQ", filters.get("value"));
				}

				String value = filters.get("value").asText();
				if (value != null && !value.isEmpty()) {
					builder.criteria = value;
				}

				return builder.queryBuilder.and();
			}
			else {
				List<StructuredQueryDefinition> queries = new ArrayList<>();
				if (type.equals("selection")) {
					JsonNode value = filters.get("value");
					if (value.isArray()) {
						for (JsonNode v : value) {
							queries.addAll(createSelectionQueries(builder, filters.get("constraintType").asText(), filters.get("constraint").asText(), v));
						}
					} else {
						queries.addAll(createSelectionQueries(builder, filters.get("constraintType").asText(), filters.get("constraint").asText(), value));
					}
				}
				else if (type.equals("range")) {
					filters.get("value").fieldNames().forEachRemaining(key -> {
						String constraintType = filters.has("constraintType") ? filters.get("constraintType").asText() : "";
						queries.add(createConstraint(builder, constraintType, filters.get("constraint").asText(), key.toUpperCase(), filters.get("value").get(key)));
					});
				}

				if (queries.size() == 1) {
					return queries.get(0);
				}

				String filterMode = filters.has("mode") ? filters.get("mode").asText() : "and";
				if (filterMode.equals("or")) {
					return builder.queryBuilder.or(queries.toArray(new StructuredQueryDefinition[0]));
				}

				return builder.queryBuilder.and(queries.toArray(new StructuredQueryDefinition[0]));
			}
		}
	}

	private StructuredQueryDefinition[] arrayFromNode(QueryBuilderAndCriteria builder, JsonNode node) {
		StructuredQueryDefinition[] q;
		if (!node.isArray()) {
			q = new StructuredQueryDefinition[] { buildQuery(builder, node) };
		}
		else {
			q = StreamSupport.stream(node.spliterator(), false)
				.map(jsonNode -> buildQuery(builder, jsonNode))
				.toArray(StructuredQueryDefinition[]::new);
		}
		return q;
	}

	private List<StructuredQueryDefinition> createSelectionQueries(QueryBuilderAndCriteria builder, String constraintType, String constraint, JsonNode v) {
		List<StructuredQueryDefinition> queries = new ArrayList<>();
		if (v.has("not")) {
			queries.add(builder.queryBuilder.not(createConstraint(builder, constraintType, constraint, "EQ", v.get("not"))));
		}
		else {
			queries.add(createConstraint(builder, constraintType, constraint, "EQ", v));
		}
		return queries;
	}

	private StructuredQueryDefinition createConstraint(QueryBuilderAndCriteria builder, String constraintType, String constraint, String operator, JsonNode value) {
		switch (constraintType) {
			case "value":
				return builder.queryBuilder.valueConstraint(constraint, value.asText());
			case "word":
				return builder.queryBuilder.wordConstraint(constraint, value.asText());
			case "custom":
				return builder.queryBuilder.customConstraint(constraint, value.asText());
			case "collection":
				return builder.queryBuilder.collectionConstraint(constraint, value.asText());
			case "geospatial":
				if (value.has("north")) {
					return builder.queryBuilder.geospatialConstraint(constraint, builder.queryBuilder.box(value.get("south").asDouble(), value.get("west").asDouble(), value.get("north").asDouble(), value.get("east").asDouble()));
				}
				else if (value.has("latitude")) {
					return builder.queryBuilder.geospatialConstraint(constraint, builder.queryBuilder.point(value.get("latitude").asDouble(), value.get("longitude").asDouble()));
				}
				else if (value.has("radius")) {
					return builder.queryBuilder.geospatialConstraint(constraint, builder.queryBuilder.circle(builder.queryBuilder.point(value.get("point").get("latitude").asDouble(), value.get("point").get("longitude").asDouble()), value.get("radius").asDouble()));
				}
				else if (value.has("point") && value.get("point").isArray()) {
					return builder.queryBuilder.geospatialConstraint(constraint, builder.queryBuilder.polygon(
						StreamSupport.stream(value.get("point").spliterator(), false)
							.map(jsonNode -> builder.queryBuilder.point(jsonNode.get("latitude").asDouble(), jsonNode.get("longitude").asDouble()))
							.toArray(StructuredQueryBuilder.Point[]::new)
					));
				}
			default:
				return builder.queryBuilder.rangeConstraint(constraint, StructuredQueryBuilder.Operator.valueOf(operator), value.asText());
		}
	}

	public JsonNode processResults(JsonNode node) {
		StreamSupport.stream(node.get("results").spliterator(), false)
			.map(jsonNode -> (ObjectNode)jsonNode)
			.map(jsonNode -> {
				try {
					jsonNode.put("id", URLEncoder.encode(jsonNode.get("uri").asText(), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return jsonNode;
			}).collect(Collectors.toList());
		return node;
	}
}

class QueryBuilderAndCriteria {

	StructuredQueryBuilder queryBuilder;
	String criteria;

	public QueryBuilderAndCriteria(StructuredQueryBuilder queryBuilder) {
		this.queryBuilder = queryBuilder;
	}
}
