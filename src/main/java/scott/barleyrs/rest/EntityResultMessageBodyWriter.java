package scott.barleyrs.rest;

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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import scott.barleydb.api.core.entity.Entity;
import scott.barleydb.api.core.entity.EntityContextState;
import scott.barleydb.api.core.entity.Node;
import scott.barleydb.api.core.entity.NotLoaded;
import scott.barleydb.api.core.entity.RefNode;
import scott.barleydb.api.core.entity.ToManyNode;
import scott.barleydb.api.core.entity.ValueNode;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class EntityResultMessageBodyWriter implements MessageBodyWriter<Entity> {

    private static final SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    private final List<String> namespaces;

    public EntityResultMessageBodyWriter() {
        System.out.println("IW NEW!!!");
        this.namespaces = new LinkedList<String>();
        namespaces.add("scott.picdb");
    }

    @Override
    public long getSize(Entity arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        return 0; //not needed by jersey
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        boolean result = (mediaType.equals(MediaType.APPLICATION_JSON_TYPE) && type.equals(Entity.class));
        return result;
    }

    @Override
    public void writeTo(Entity  result, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        System.out.println("converting Entity to JSON");
        result.getEntityContext().setEntityContextState(EntityContextState.INTERNAL);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonGenerator gen = mapper.getFactory().createGenerator(entityStream);
            Set<Entity> started = new HashSet<>();
            JsonNode json = toJson(mapper, result, started);
            gen.writeTree(json);
        }
        finally {
            result.getEntityContext().setEntityContextState(EntityContextState.USER);
        }

    }

    private JsonNode toJson(ObjectMapper mapper, Entity entity, Set<Entity> started) {
        if (!started.add(entity)) {
            return null;
        }
        try {
            ObjectNode jsonEntity = mapper.createObjectNode();
            for (Node node: entity.getChildren(Node.class)) {
               putNode(mapper, jsonEntity, node, started);
            }
            return jsonEntity;
        }
        finally {
            started.remove(entity);
        }
    }

    private void putNode(ObjectMapper mapper, ObjectNode jsonEntity, Node node, Set<Entity> started) {
        if (node instanceof ValueNode) {
            setValue(jsonEntity, node.getName(), (ValueNode)node);
        }
        else if (node instanceof RefNode) {
            Entity reffedEntity = ((RefNode) node).getReference(false);
            if (reffedEntity != null) {
                if (reffedEntity.isClearlyUnderstoodIfInDatabaseOrNot()) {
                    JsonNode je = toJson(mapper, reffedEntity, started);
                    if (je != null) {
                        jsonEntity.set(node.getName(), je);
                    }
                }
                else if (reffedEntity.isFetchRequired()){
                    /*
                     * a fetch is required, we just output the ID
                     */
                    jsonEntity.put(node.getName(), reffedEntity.getKey().getValue().toString());
                }

            }
            else {
                jsonEntity.putNull(node.getName());
            }
        }
        else if (node instanceof ToManyNode) {
            ToManyNode tm = (ToManyNode)node;
            if (!tm.getList().isEmpty()) {
                ArrayNode array = jsonEntity.arrayNode();
                for (Entity e: tm.getList()) {
                    JsonNode je = toJson(mapper, e, started);
                    if (je != null) {
                        array.add( je );
                    }
                }
                jsonEntity.set(tm.getName(), array);
            }
        }
    }

    private void setValue(ObjectNode jsonEntity, String name, ValueNode node) {
        Object value = node.getValue();
        if (value == null) {
            jsonEntity.putNull(node.getName());
        }
        else if (value instanceof String) {
            jsonEntity.put(node.getName(), (String)value);
        }
        else if (value instanceof Long) {
            jsonEntity.put(node.getName(), (Long)value);
        }
        else if (value instanceof Date) {
            jsonEntity.put(node.getName(), df.format((Date)value));
        }
        else if (value instanceof Integer) {
            jsonEntity.put(node.getName(), (Integer)value);
        }
        else if (value instanceof BigDecimal) {
            jsonEntity.put(node.getName(), (BigDecimal)value);
        }
        else if (value instanceof Boolean) {
            jsonEntity.put(node.getName(), (Boolean)value);
        }
        else if (value == NotLoaded.VALUE) {
            //we skip these properties
        }
        else {
            throw new IllegalStateException("Cannot serialize value of type " + value.getClass().getSimpleName());
        }
    }

}
