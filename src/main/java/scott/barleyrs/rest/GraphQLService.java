package scott.barleyrs.rest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.QueryBatcher;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityConstraint;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.Node;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.exception.BarleyDBException;
import scott.barleydb.api.persist.PersistRequest;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QPropertyCondition;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.specification.KeyGenSpec;
import scott.barleydb.server.jdbc.query.QueryResult;
import scott.barleyrs.rest.request.FetchGraph;
import scott.barleyrs.rest.request.GraphQLRequest;
import scott.barleyrs.rest.request.QueryDef;

/*
 * #%L
 * BarleyRS
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2016 Scott Sinclair
 *       <scottysinclair@gmail.com>
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

@Path("/barleyrs")
@Component
public class GraphQLService {

    @Inject
    private Environment env;

    @GET
    @Path("/graphql/{namespace}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String,Object> getEntityById(
            @PathParam("namespace") String namespace,
            Map<String,Object> body) throws BarleyDBException {

        EntityContext ctx = new EntityContext(env, namespace);
        
        Map<QueryDef,QueryObject<Object>> queries = new LinkedHashMap<>();
        QueryBatcher qbatch = new QueryBatcher();
        
        GraphQLRequest request = new GraphQLRequest(body);
        
        for (QueryDef queryDef: request.getQueryDefs()) {
        	QueryObject<Object> q = createQuery(ctx, queryDef);	
        	queries.put(queryDef, q);
        	qbatch.addQuery(q);
        }
        
        ctx.performQueries(qbatch);
        
        Map<String, Object> output = new HashMap<>();
        int i = 0;
        for (QueryDef queryDef: request.getQueryDefs()) {
        	toResultList(qbatch.getResults().get(i++), queryDef.getFetchGraph());
        }
        return output;
    }

    
	private QueryObject<Object> createQuery(EntityContext ctx,  QueryDef queryDef) {
        EntityType entityType = ctx.getDefinitions().getEntityTypeMatchingInterface(queryDef.getEntityTypeName(), true);
        QueryObject<Object> qo = new QueryObject<>( entityType.getInterfaceName() );
        addProjection(qo, queryDef);
        addWhereClause(qo, queryDef);
		return qo;
	}

    private void addWhereClause(QueryObject<Object> qo, QueryDef queryDef) {
	}


	private List<Map<String, Object>> toResultList(QueryResult<?> queryResult, FetchGraph fetchGraph) {
    	List<Map<String,Object>> result = new LinkedList<>();
    	for (Entity entity: queryResult.getEntityList()) {
    		result.add(toMap(entity, fetchGraph));
    	}
    	return result;
    }

    private Map<String, Object> toMap(Entity entity, FetchGraph fetchGraph) {
		Map<String,Object> entityMap = new HashMap<>();
		for (Node node: entity.getChildren()) {
			if (node instanceof ValueNode) {
				ValueNode vn = (ValueNode)node;
				if (vn.isLoaded()) {
					entityMap.put(node.getName(), vn.getValue());
				}
			}
			else if (fetchGraph.isFetched(node.getName())) {
				if (node instanceof RefNode) {
					RefNode rn = (RefNode)node;
					Map<String, Object> map = toMap(rn.getReference(), fetchGraph.getSubGraph(node.getName()));
					entityMap.put(node.getName(), map);
				}
				if (node instanceof ToManyNode) {
					ToManyNode tmn = (ToManyNode)node;
					List<Map<String, Object>> list = new LinkedList<>();
					for (Entity e: tmn.getList()) {
						list.add(toMap(e, fetchGraph.getSubGraph(node.getName())));
					}
					entityMap.put(node.getName(),  list);
				}
			}
		}
    	return entityMap;
	}

	private void addProjection(QueryObject<?> qo, QueryDef queryDef) {
//        for (String property: queryDef.split(",")) {
//            QProperty<Object> prop = new QProperty<>(qo, property);
//            qo.andSelect(prop);
//        }
    }

    /**
     * Sets a condition where the key of the entity is equal to id.
     * @param entityType the entity type
     * @param qo the query object
     * @param id the key value
     * @return the condition PK = ID
     */
    private QPropertyCondition keyEquals(EntityType entityType , QueryObject<?> qo, String id) {
        NodeType keyNodeType = entityType.getNodeType( entityType.getKeyNodeName(), true);
        QProperty<Object> keyProperty = new QProperty<>(qo, entityType.getKeyNodeName());
        return keyProperty.equal( convert(keyNodeType, id, true) );
    }

    private Object convert(NodeType nodeType, String id, boolean convertEmptyStringToNull) {
        if (id == null || (id.isEmpty() && convertEmptyStringToNull)) {
            return null;
        }
        if (nodeType.getRelationInterfaceName() != null && nodeType.getJdbcType() != null) {
            EntityType refType = nodeType.getEntityType().getDefinitions().getEntityTypeMatchingInterface(nodeType.getRelationInterfaceName(), true);
            NodeType keyType = refType.getNodeType( refType.getKeyNodeName(), true );
            return convert(keyType, id, convertEmptyStringToNull);
        }
        switch (nodeType.getJavaType()) {
            case STRING : return id;
            case INTEGER: return Integer.parseInt(id);
            case BOOLEAN: return Boolean.parseBoolean( id );
            case LONG: return Long.parseLong( id );
            case BIGDECIMAL: return new BigDecimal( id );
            case ENUM: return id; //TODO
            case SQL_DATE: return id; //TODO
            case UTIL_DATE: return id; //TODO
            case UUID: return UUID.fromString(id);
        }
        return id;
    }


}
