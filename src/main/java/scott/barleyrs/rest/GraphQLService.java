package scott.barleyrs.rest;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import scott.barleydb.api.core.Environment;
import scott.barleydb.api.exception.BarleyDBException;
import scott.barleydb.api.graphql.BarleyGraphQLSchema;
import scott.barleydb.api.graphql.GraphQLContext;
import scott.barleydb.api.specification.SpecRegistry;
import scott.barleydb.bootstrap.JdbcEnvironmentBootstrap;

import static java.util.Objects.requireNonNull;

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


    @Inject
    private JdbcEnvironmentBootstrap bootstrap;

    private Map<String,BarleyGraphQLSchema> schemasByNamespace = new HashMap<>();

    @POST
    @Path("/graphql/{namespace}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object execute(
            @PathParam("namespace") String namespace,
            Map<String,Object> body) throws BarleyDBException {

        System.out.println("HELLO!!!!!!!!!!!!!");

        BarleyGraphQLSchema schema = getOrCreate(namespace);
        GraphQLContext ctx = schema.newContext();
        String query = (String)body.get("query");
        Object result = ctx.execute(query);
        System.out.println("DONE!!!!!!!!!!!!!");
        return result;
    }

    private synchronized BarleyGraphQLSchema getOrCreate(String namespace) {
        BarleyGraphQLSchema schema = schemasByNamespace.get(namespace);
        if (schema == null) {
            schemasByNamespace.put(namespace, schema = new BarleyGraphQLSchema(getMandatorySpecRegistry(namespace), env, namespace, null));
        }
        return schema;
    }

    private SpecRegistry getMandatorySpecRegistry(String namespace) {
        for (SpecRegistry reg: bootstrap.getSpecRegistries()) {
            if (reg.getDefinitionsSpec(namespace) != null) {
                return reg;
            }
        }
        requireNonNull("Spec Registry with namespace " + namespace + " not found");
        return null;
    }


}
