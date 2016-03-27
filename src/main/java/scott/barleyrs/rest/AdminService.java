package scott.barleyrs.rest;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import scott.barleydb.api.config.Definitions;
import scott.barleydb.api.config.EntityType;
import scott.barleydb.api.config.NodeType;
import scott.barleydb.api.core.Environment;
import scott.barleydb.api.core.types.JavaType;

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
public class AdminService {

    @Inject
    private Environment env;


    public void createEntityType() {

    }

    public void deleteEntityType() {

    }

    public void listEntityTypes() {
    }

    /**
     * Gets the entity type as a JSON schema.<br/>
     * The schema is compatibly extended with extra barley information.
     *
     */
    @GET
    @Path("/entitytypes/{namespace}/{entityType}")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonNode getEntityTypeJsonSchema(
            @PathParam("namespace") String namespace,
            @PathParam("entityType") String entityTypeName) {

         Definitions definitions = env.getDefinitions(namespace);
         String className = namespace + ".model." + entityTypeName;
         EntityType entityType = definitions.getEntityTypeMatchingInterface(className, true);

         ObjectMapper mapper = new ObjectMapper();

         ObjectNode schemaRoot = mapper.createObjectNode();
         schemaRoot.put("title", "JSON Schema for namepsace " + namespace + " and entity " + entityTypeName);
         schemaRoot.put("type", "object");
         /*
          * required section.
          */
         ArrayNode required = mapper.createArrayNode();
         for (NodeType nodeType: entityType.getNodeTypes()) {
             if (nodeType.isMandatory()) {
                 required.add( nodeType.getName() );
             }
         }
         schemaRoot.set("required", required);

         /*
          * properties section
          */
         ObjectNode properties = mapper.createObjectNode();
         for (NodeType nodeType: entityType.getNodeTypes()) {
             ObjectNode prop = mapper.createObjectNode();
             if (nodeType.getJavaType() != null) {
                 prop.put("type", toJSONSchemaType(nodeType.getJavaType()));
                 properties.set(nodeType.getName(), prop);
             }
         }
         schemaRoot.set("properties", properties);
         return schemaRoot;
    }

    private String toJSONSchemaType(JavaType javaType) {
        switch (javaType) {
        case STRING:
            return "string";
        case INTEGER:
            return "integer";
        case LONG:
            return "integer";
        case BOOLEAN:
            return "boolean";
        case BIGDECIMAL:
            return "number";
        case ENUM:
            return "string";
        default:
            return "object";
        }
    }

}
