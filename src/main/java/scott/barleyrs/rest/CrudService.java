package scott.barleyrs.rest;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContext;
import scott.barleydb.api.exception.SortException;
import scott.barleydb.api.exception.execution.SortServiceProviderException;
import scott.barleydb.api.exception.execution.query.SortQueryException;
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
    public Entity getEntityById(@PathParam("namespace") String namespace, @PathParam("entityType") String entityTypeName, @PathParam("id") String id) {
        EntityContext ctx = new EntityContext(env, namespace);
        String interfaceName = namespace + entityTypeName;
        EntityType entityType = ctx.getDefinitions().getEntityTypeMatchingInterface(interfaceName, true);
        return ctx.getOrLoad(entityType, id);
    }

    @GET
    @Path("/entities/{namespace}/{entityType}/")
    @Produces(MediaType.APPLICATION_JSON)
    public QueryResult<?> listEntities(@PathParam("namespace") String namespace, @PathParam("entityType") String entityTypeName) throws SortException {
        EntityContext ctx = new EntityContext(env, namespace);
        String interfaceName = namespace + entityTypeName;
        EntityType entityType = ctx.getDefinitions().getEntityTypeMatchingInterface(interfaceName, true);
        QueryObject<?> qo = ctx.getUnitQuery(entityType);
        return ctx.performQuery(qo);
    }


}
