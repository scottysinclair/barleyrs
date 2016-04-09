package scott.barleyrs.rest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.core.entity.Node;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.core.entity.ValueNode;
import scott.barleydb.api.exception.SortException;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.persist.SortPersistException;
import scott.barleydb.api.persist.PersistRequest;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QPropertyCondition;
import scott.barleydb.api.query.QueryObject;
import scott.barleydb.api.specification.KeyGenSpec;
import scott.barleydb.server.jdbc.query.QueryResult;

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
public class CrudService {

    @Inject
    private Environment env;

    @GET
    @Path("/entities/{namespace}/{entityType}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Entity getEntityById(
            @PathParam("namespace") String namespace,
            @PathParam("entityType") String entityTypeName,
            @PathParam("id") String id,
            @QueryParam("proj") String projecting) throws SortException {

        EntityContext ctx = new EntityContext(env, namespace);
        EntityType entityType = getEntityType(ctx.getDefinitions(), namespace, entityTypeName);
        QueryObject<?> qo = new QueryObject<>( entityType.getInterfaceName() );

        if (projecting != null) {
            addProjection(qo, projecting);
        }

        qo.where( keyEquals(entityType, qo, id) );
        List<Entity> list = ctx.performQuery(qo).getEntityList();
        return list.isEmpty() ? null : list.get(0);
    }

    private void addProjection(QueryObject<?> qo, String projecting) {
        for (String property: projecting.split(",")) {
            QProperty<Object> prop = new QProperty<>(qo, property);
            qo.andSelect(prop);
        }
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

    @GET
    @Path("/entities/{namespace}/{entityType}/")
    @Produces(MediaType.APPLICATION_JSON)
    public QueryResult<?> listEntities(
            @PathParam("namespace") String namespace,
            @PathParam("entityType") String entityTypeName,
            @QueryParam("proj") String projecting) throws SortException {

        EntityContext ctx = new EntityContext(env, namespace);
        EntityType entityType = getEntityType(ctx.getDefinitions(), namespace, entityTypeName);
        QueryObject<?> qo = new QueryObject<>( entityType.getInterfaceName() );

        if (projecting != null) {
            addProjection(qo, projecting);
        }

        QueryResult<?> result = ctx.performQuery(qo);
        return result;
    }

    private EntityType getEntityType(Definitions definitions, String namespace, String entityTypeName) {
        return definitions.getEntityTypeMatchingInterface(entityTypeName, true);
    }


    @POST
    @Path("/entities/{namespace}/{entityType}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<Entity> persist(
             @PathParam("namespace") String namespace,
             @PathParam("entityType") String entityTypeName,
             @PathParam("id") String id,
             ObjectNode rootNode) throws SortException {

        EntityContext ctx = new EntityContext(env, namespace);
        EntityType entityType = getEntityType(ctx.getDefinitions(), namespace, entityTypeName);

        Entity entity = toEntity(ctx, rootNode, entityType);

        ctx.persist(new PersistRequest().save(entity));

        return Collections.emptyList();
    }

    @DELETE
    @Path("/entities/{namespace}/{entityType}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public boolean delete(
             @PathParam("namespace") String namespace,
             @PathParam("entityType") String entityTypeName,
             @PathParam("id") String id,
             ObjectNode rootNode) throws SortException {

        EntityContext ctx = new EntityContext(env, namespace);
        EntityType entityType = getEntityType(ctx.getDefinitions(), namespace, entityTypeName);

        Entity entity = toEntity(ctx, rootNode, entityType);

        ctx.persist(new PersistRequest().delete(entity));

        return true;
    }

    private Entity toEntity(EntityContext ctx, ObjectNode jsObject, EntityType entityType) {
        Object keyValue = getEntityKey(jsObject, entityType);
        Entity entity = newEntityInCorrectState(ctx, entityType, keyValue);

        for (Iterator<String> i = jsObject.fieldNames(); i.hasNext();) {
            String fieldName = i.next();
            JsonNode jsNode = jsObject.get( fieldName );
            if (jsNode == null || jsNode.isNull()) {
                continue;
            }
            Node eNode = entity.getChild(fieldName);
            if (eNode instanceof ValueNode) {
                ((ValueNode)eNode).setValue( convert( eNode.getNodeType(), jsNode.asText(), true));
            }
            else if (eNode instanceof RefNode) {
                RefNode refNode = ((RefNode)eNode);
                if (jsNode.isValueNode()) {
                    /*
                     * we just refer to a key so set it
                     */
                    refNode.setEntityKey( convert( eNode.getNodeType(), jsNode.asText(), true ) );
                }
                else if (jsNode.isObject()) {
                    /*
                     * we refer to a whole object definition, so convert it to an entity.
                     */
                    Entity reference = toEntity(ctx, ((ObjectNode)jsNode), refNode.getEntityType());
                    refNode.setReference( reference );
                }
                else {
                    throw new IllegalStateException("Unexpected JSON node type '" + jsNode + "'");
                }
            }
            else if (eNode instanceof ToManyNode) {
                ToManyNode toManyNode = (ToManyNode)eNode;
                if (!jsNode.isArray()) {
                    throw new IllegalArgumentException("Expected as JSON array for field '" + fieldName + "'");
                }
                for (Iterator<JsonNode> iel = ((ArrayNode)jsNode).elements(); iel.hasNext();) {
                    JsonNode element = iel.next();
                    if (element.isValueNode()) {
                        /*
                         * we just refer to a key so set it
                         */
                        Object key = convertForKey(toManyNode.getEntityType(), element.asText());
                        Entity e = ctx.getOrCreateBasedOnKeyGenSpec(toManyNode.getEntityType(), key);
                        toManyNode.add( e );
                    }
                    else if (element.isObject()){
                        /*
                         * we just refer to a JSON object so convert it to an entity
                         */
                        Entity reference = toEntity(ctx, ((ObjectNode)element), toManyNode.getEntityType());
                        toManyNode.add(reference);
                    }
                    else {
                        throw new IllegalStateException("Unexpected JSON node type '" + jsNode + "'");
                    }
                }

            }
        }
        return entity;
    }

    /**
     *
     * @param ctx
     * @param entityType
     * @param keyValue can be null.
     * @return
     */
    private Entity newEntityInCorrectState(EntityContext ctx, EntityType entityType, Object keyValue) {
        Entity entity;
        if (keyValue == null) {
            if (entityType.getKeyGenSpec() == KeyGenSpec.CLIENT) {
                throw new IllegalStateException("The client should generate the primary key for " + entityType.getInterfaceShortName());
            }
            //there is no PK yet, we know it is a new entity which does not exist in the DB yet
            entity = ctx.newEntity(entityType);
        }
        else {
            /*
             * we have a PK value from the client, but we still need to know if we are doing an insert
             * or an update.
             */
            if (entityType.getKeyGenSpec() == KeyGenSpec.FRAMEWORK) {
                /*
                 * barleydb generates the key, as the key is already there we must be doing an update
                 * of an existing record.
                 * So we pretend that this entity was loaded from the DB so that an update will be perfomed.
                 */
                entity = ctx.newFakeLoadedEntity(entityType, keyValue);
            }
            else {
                /*
                 * so the client provides the PK always for this entity type. We have no way of knowing if it is an
                 * insert of an update, so we use the PERHAPS_IN_DATABASE state.
                 */
                entity = ctx.newPerhapsInDatabaseEntity(entityType, keyValue);
            }
        }
        return entity;
    }

    private Object getEntityKey(ObjectNode jsObject, EntityType entityType) {
        JsonNode keyNode = jsObject.get( entityType.getKeyNodeName() );
        if (keyNode == null || keyNode.isNull()) {
            return null;
        }
        String keyAsString = keyNode.asText();
        if (keyAsString == null || keyAsString.isEmpty()) {
            return null;
        }
        return convertForKey(entityType, keyAsString);
    }

    private Object convertForKey(EntityType entityType, String keyAsString) {
        NodeType keyNodeType = entityType.getNodeType(entityType.getKeyNodeName(), true);
        return convert(keyNodeType, keyAsString, true);
    }


}
