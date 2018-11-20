package scott.barleyrs.rest.request;

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

public class JData {

	public static void main(String args[]) {
		
		DMap root = new DMap(null, new HashMap<>());

		DValues values1 = root.get("tickets", "subtickets").asArray();

		
		DValues voucherIds = root.get("vouchers", "id").asArray();
		
		root.get("tickets", "subtickets")
		  .stream()
		    .filter(dv -> dv.get("voucherid").matchesOneOf(voucherIds));

		
		root.get("tickets", "subtickets")
			.withMatching("voucherid", voucherIds)
			 .stream();
		
		voucherIds.stream()
		  .map(root.get("tickets", "subtickets").withMatchingField("voucherid"));
				
		/*
		 * 
		 * TODO
		 *  DValue should be an interface
		 *  
		 *  DArray and DScalar and DMap should implement DValue
		 */

		
		
	}
	
}

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
		return new DValue(this, names[0], null);
	}

	@Override
	public DValues getAll(String... name) {
		return null;
	}
	
}



class DValue implements DPoint {
	private final DPoint parent;
	private final String field;
	private final Object value;
	public DValue(DPoint parent, String field, Object value) {
		this.parent = parent;
		this.field = field;
		this.value = value;
	}
	
	public Function<DValue,DValue> withMatchingField(String fieldName) {
		return (dv) -> 
			new LinkedList<DValue>().stream()
				.filter(mydv -> mydv.get(fieldName).equals(dv))
				.findFirst()
				.orElse(null);
	}

	public DFilter withMatching(String field, DValues valuesToMatch) {
		return null;
	}

	public boolean matchesOneOf(DValues values) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public DPoint getParent() {
		return parent;
	}

	public String getField() {
		return field;
	}

	public Object getValue() {
		return value;
	}
		
//	public DPoint asPoint() {
////		if (value instanceof Map) {
////			return new DMap(parent, (Map<String,Object>)value);
////		}
////		return new EmptyPoint(parent, field);
//		return th
//	}
	
	public Stream<DValue> stream() {
		return asArray().stream();
	}

	public DArray asArray() {
		return DArray.of(this);
	}
	
	public boolean isArray() {
		return DArray.isArray(this);
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
	
	
}

abstract class DValues {
	
	public DFilter filter(Predicate<DValue> predicate) {
		return new DFilter(this, predicate);
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


class DArrayItem extends DValue {
	private final DArray array;
	private final int index;

	public DArrayItem(DArray array, int index) {
		super(array, array.getField(), getValue(array, index));
		this.array = array;
		this.index = index;
	}
	
	private static Object getValue(DArray array, int index) {
		return null;
	}
}

class DArray extends DValues implements DPoint {
	private final DValue array;
	private DArray(DValue array) {
		this.array = array;
	}
		
	@Override
	public DPoint getParent() {
		return array.getParent();
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
		return array.getField();
	}
	
	public static DArray of(DValue value) {
		return new DArray(value);
	}

	public static boolean isArray(DValue value) {
		return value.getValue() instanceof List;
	}
	
	public DArrayItem get(int index) {
		if (array.getValue() instanceof List) {
			return new DArrayItem(this, index);
		}
		else if (index == 0) {
			return new DArrayItem(this, index);
		}
		throw new ArrayIndexOutOfBoundsException(index);
	}

	@Override
	public Stream<DValue> stream() {
		if (array.getValue() instanceof List) {
				//foreach item create array item
		}
		return Collections.singletonList(array).stream();
	}
}

class DMap implements DPoint {
	private final DPoint parent;
	private final Map<String,Object> data;

	public DMap(DPoint parent, Map<String, Object> data) {
		this.parent = parent;
		this.data = data;
	}

	@Override
	public DPoint getParent() {
		return parent;
	}

	@Override
	public DValue get(String ...names) {
		LinkedList<String> parts = new LinkedList<>(Arrays.asList(names));
		String name = parts.getFirst();
		DValue value = new DValue(this, name, data.get(name));
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
	
	
}