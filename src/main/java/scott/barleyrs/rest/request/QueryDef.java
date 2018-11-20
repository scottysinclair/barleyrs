package scott.barleyrs.rest.request;

/*
 * #%L
 * BarleyRS
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2014 - 2018 Scott Sinclair
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

import java.util.Map;

public class QueryDef {
	
	private String name;
	private Map<String, Object> data;

	public QueryDef(String name, Object object) {
		this.name = name;
		this.data = (Map<String, Object>)object;
	}
	
	public FetchGraph getFetchGraph() {
		return new FetchGraph((Map<String, Object>) data.get("fetch"));
	}

	public String getEntityTypeName() {
		return name;
	}

}
