package scott.barleyrs.rest.request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.security.PermitAll;

public class JData {

	public static void main(String args[]) {
		
		DMap root = new DMap(null, null, new HashMap<>());

		DValues values1 = root.get("tickets", "subtickets").asValues();
		
		DValues voucherIds = root.get("vouchers", "id").asValues();
		
		root.get("tickets", "subtickets")
		  .stream()
		    .filter(dv -> dv.get("voucherid").matchesOneOf(voucherIds));

		
		root.get("tickets", "subtickets").asValues()
			.withMatching("voucherid", voucherIds)
			 .stream();
		
		voucherIds.stream()
		  .map(root.get("tickets", "subtickets")
				  	.asValues()
				  	.withMatchingField("voucherid"))
		  			.singleValue();
				//value <- arraxy <- array <- filter <- single
		/*
		 * 
		 * TODO
		 *  DValue should be an interface
		 *  
		 *  DArray and DScalar and DMap should implement DValue
		 */

		
		
	}
}

/**
 * a point in the data hierarchy from where to search
 * @author scott
 *
 */
interface DPoint {
	DPoint getParent();

	DValue get(String ...name);

	DValues getAll(String ...name);
}

class EmptyPoint implements DPoint {
	private final DPoint parent;
	private final String field;
	public EmptyPoint(DPoint parent, String field) {
		this.parent = parent;
		this.field = field;
	}
	
	@Override
	public DPoint getParent() {
		return parent;
	}

	@Override
	public DValue get(String ...names) {
		//TODO: propertly nest
		return new DScalar(this, names[0], null);
	}

	@Override
	public DValues getAll(String... name) {
		//TODO:
		return null;
	}
	
}


interface DValue extends DPoint {
	DPoint getParent();

	boolean matchesOneOf(DValues values);

	String getField();

	Object getActual();
			
	Stream<DValue> stream();
	
	DValues asValues();
}

class DScalar implements DValue {
	private final DPoint parent;
	private final String field;
	private final Object value;
	public DScalar(DPoint parent, String field, Object value) {
		this.parent = parent;
		this.field = field;
		this.value = value;
	}
	
	@Override
	public boolean matchesOneOf(DValues values) {
		//TODO:
		return false;
	}

	public DFilter withMatching(String field, DValues valuesToMatch) {
		return null;
	}

	@Override
	public DPoint getParent() {
		return parent;
	}

	@Override
	public String getField() {
		return field;
	}

	public Object getValue() {
		return value;
	}
			
	@Override
	public Stream<DValue> stream() {
		return Collections.<DValue>singletonList(this).stream();
	}
	
	@Override
	public DValue get(String... name) {
		return null;
	}

	@Override
	public DValues getAll(String... name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getActual() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DValues asValues() {
		// TODO Auto-generated method stub
		return null;
	}
}

abstract class DValues {
	
	public DFilter filter(Predicate<DValue> predicate) {
		return new DFilter(this, predicate);
	}
	
	public Function<DValue,DValue> withMatchingField(String fieldName) {
		return (dv) -> 
			new LinkedList<DValue>().stream()
				.filter(mydv -> mydv.get(fieldName).equals(dv))
				.findFirst()
				.orElse(null);
	}

	public DFilter withMatching(String string, DValues values) {
		return null;
	}

	public abstract Stream<DValue> stream();
}

class DFilter extends DValues {
	private final DValues source;
	private Predicate<DValue> predicate;

	public DFilter(DValues source, Predicate<DValue> predicate) {
		this.source = source;
		this.predicate = predicate;
	}

	@Override
	public Stream<DValue> stream() {
		return source.stream().filter(predicate);
	}
	
}


class DArrayItem implements DValue {
	private final DArray array;
	private final int index;

	public DArrayItem(DArray array, int index) {
		this.array = array;
		this.index = index;
	}

	@Override
	public boolean matchesOneOf(DValues values) {
		//TODO:
		return false;
	}



	private static Object getValue(DArray array, int index) {
		return null;
	}

	@Override
	public DValue get(String... name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DValues getAll(String... name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DPoint getParent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getField() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getActual() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Stream<DValue> stream() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public DValues asValues() {
		// TODO Auto-generated method stub
		return null;
	}

}

class DArray extends DValues implements DValue {
	private final List<Object> list;
	private final DPoint parent;
	private final String field;
	public DArray(DPoint parent, String field, List<Object> list) {
		this.parent = parent;
		this.field = field;
		this.list = list;
	}

	@Override
	public boolean matchesOneOf(DValues values) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public DPoint getParent() {
		return parent;
	}

	@Override
	public DValue get(String... name) {
		return null;
	}

	
	@Override
	public DValues getAll(String... name) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getField() {
		return field;
	}

	public DArrayItem get(int index) {
		if (index >= list.size()) {
			throw new ArrayIndexOutOfBoundsException(index);
		}
		return new DArrayItem(this, index);
	}

	@Override
	public Stream<DValue> stream() {
		//TODO: natively use stream
		return toDArrayItems().stream();
	}
	
	private List<DValue> toDArrayItems() {
		List<DValue> result = new ArrayList<>(list.size());
		for (int i=0; i<list.size(); i++) {
			result.add(new DArrayItem(this, i));
		}
		return result;
	}

	@Override
	public Object getActual() {
		return list;
	}

	@Override
	public DValues asValues() {
		// TODO Auto-generated method stub
		return null;
	}
}

class DValueFac {
	
	public static DValue asValue(DPoint parent, String field, Object object) {
		if (object instanceof List) {
			return new DArray(parent, field, (List<Object>)object);
		}
		else if (object instanceof Map) {
			return new DMap(parent, field, (Map<String, Object>)object);
		}
		return new DScalar(parent, field, object);
	}
}

class DMap implements DValue {
	private final DPoint parent;
	private final String field;
	private final Map<String,Object> data;

	public DMap(DPoint parent, String field, Map<String, Object> data) {
		this.parent = parent;
		this.field = field;
		this.data = data;
	}
	
	@Override
	public boolean matchesOneOf(DValues values) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DPoint getParent() {
		return parent;
	}

	@Override
	public DValue get(String ...names) {
		LinkedList<String> parts = new LinkedList<>(Arrays.asList(names));
		String name = parts.getFirst();
		DValue value = DValueFac.asValue(this, name, data.get(name));
		if (parts.size() == 1) {
			return value;
		}
		else {
			List<String> rest = parts.subList(1,  names.length);
			return value.get(rest.toArray(new String[rest.size()]));
		}
	}

	@Override
	public DValues getAll(String... name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getField() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getActual() {
		return data;
	}

	@Override
	public Stream<DValue> stream() {
		//each DValue has this map as it's parent and it's field is the map key.
		return data.entrySet().stream()
				.map(en -> DValueFac.asValue(this, en.getKey(), en.getValue()));
	}

	@Override
	public DValues asValues() {
		// TODO Auto-generated method stub
		return null;
	}	
	
}