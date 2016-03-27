package scott.barleyrs.rest;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.exception.SortException;
import scott.barleydb.api.query.QProperty;
import scott.barleydb.api.query.QPropertyCondition;
import scott.barleydb.api.query.QueryObject;
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


    public void persist() {
    }

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
        return keyProperty.equal( convert(keyNodeType, id) );
    }

    private Object convert(NodeType nodeType, String id) {
        switch (nodeType.getJavaType()) {
            case LONG: return Long.parseLong( id );
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
        String interfaceName = namespace + ".model." + entityTypeName;
        return definitions.getEntityTypeMatchingInterface(interfaceName, true);
    }


}
