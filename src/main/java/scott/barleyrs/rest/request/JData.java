package scott.barleyrs.rest.request;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static scott.barleyrs.rest.request.DPath.path;
import static scott.barleyrs.rest.request.DPred.matches;

public class JData {

	public static void main(String args[]) {

		/*
		DMap root = new DMap(null, null, new HashMap<>());

		DValues values1 = root.get(path("tickets", "subtickets")).asValues();
		
		DValues voucherIds = root.get(path("vouchers", "id")).asValues();
		
		root.get(path("tickets", "subtickets"))
		  .stream()
		    .filter(dv -> dv.get(path("voucherid")).matchesOneOf(voucherIds));

		
		root.get(path("tickets", "subtickets")).asValues()
			.withMatching("voucherid", voucherIds)
			 .stream();

		//value <- array <- array <- filter <- single

		//values <- filter (eq 3)
		//values <- filter (eq (point + path))

		//voucherids <- group (voucherids + values) <- get (filter (eq (value path), voucherid))
		//voucherids <- group (voucherids  values) <- get ("values" <- filter (eq (value path), "voucherids"))


		//voucherids <- (group (vid  values))* <- filter ("values " (eq (value path), "voucherids"))) getSingle

		//voucherids  <- (id subt*)*  <- (id subt)* <- CHECK (id subt)*
*/

		DValue root = null;

		DValues voucherIds = root.get(path("vouchers", "id")).asValues();

		DValues subTickets = root.get(path("tickets", "subtickets")).asValues();

		voucherIds
				// to groups of (vid, subTicket)*
				.crossProduct("vid", "subTickets", subTickets)

				//filter where (vid == subTicket.id
				.filter(matches(path("vid"), path("subTickets", "id")));


		/*
		root.get(path("tickets", "subtickets"))
				.asValues()
				.filter(DPred.matches(3));


		root.get(path("tickets", "subtickets"))
				.asValues()
				.filter(DPred.matches(path("voucherid")));
*/

		/*
		voucherIds.stream()
		  .map(root.get("tickets", "subtickets")
				  	.asValues()
				  	.withMatchingField("voucherid"))
		  			.singleValue();
		  			*/
		/*
		 * 
		 * TODO
		 *  DValue should be an interface
		 *  
		 *  DArray and DScalar and DMap should implement DValue
		 */

		
		
	}
}

//p -> p -> p
class DPath {

	private final DPath next;
	private final String name;

	public DPath(DPath next, String name) {
		this.next = next;
		this.name = name;
	}

	public DPath next() {
		return next;
	}

	public String name() {
		return name;
	}

	public static DPath path(String ...names) {
		DPath next = null;
		List<String> rev = new ArrayList<>(asList(names));
		Collections.reverse(rev);
		for (String name: rev) {
			next = new DPath(next, name);
		}
		return next;
	}

	public DPath first() {
		return this;
	}

	public int size() {
		if (next == null) {
		  return 1;
		}
		return next.size() + 1;
	}

	public DPath tail() {
		if (next == null) {
			return null;
		}
		return next;
	}
}

class DPred {



	/**
	 * @return
	 */
	public static Predicate<DValue> matches(DValue value) {
		return (dv) -> dv.matches( value );
	}

	public static Predicate<DValue> matches(DPath pathA, DPath pathB) {
		return (dv) -> dv.get(pathA).matches(dv.get(pathB));
	}

}

/**
 * a point in the data hierarchy from where to search
 * @author scott
 *
 */
interface DPoint {
	DPoint getParent();

	DValue get(DPath path);

	DValues getAll(DPath path);
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
	public DValue get(DPath path) {
		//TODO: propertly nest
		return new DScalar(this, path.first().name(), null);
	}

	@Override
	public DValues getAll(DPath path) {
		//TODO:
		return null;
	}
	
}


interface DValue extends DPoint {
	DPoint getParent();

	boolean matchesOneOf(DValues values);

	boolean matches(DValue value);

	boolean matches(Object object);

	String getField();

	Object getActual();
			
	Stream<? extends DValue> stream();
	
	DValues asValues();

	DGroups flatMapThenMerge(String myGroupKey, String otherGroupKey, DValues values);
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
	public boolean matches(DValue value) {
		return false;
	}

	@Override
	public boolean matches(Object object) {
		return false;
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
	public DGroups flatMapThenMerge(String myGroupKey, String otherGroupKey, DValues otherValues) {
		return new DGroups(stream()
				.map(v -> new DGroup(myGroupKey, this, otherGroupKey, otherValues)));
	}

	@Override
	public DValue get(DPath path) {
		return null;
	}

	@Override
	public DValues getAll(DPath path) {
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
	
	public DFilter withMatchingField(String fieldName) {
		return new DFilter(this, (dv ) -> dv.get(path(fieldName)).matches(null));
	}

	public DFilter withMatching(String string, DValues values) {
		return new DFilter(this, (dv ) -> dv.matchesOneOf(values));
	}

	public abstract Stream<? extends DValue> stream();

	public DGroups crossProduct(String myGroupKey, String otherGroupKey, DValues otherValues) {
		//for each id, stream otherValues
		return new DGroups(stream()
				.map(v -> v.flatMapThenMerge(myGroupKey, otherGroupKey, otherValues))
				.flatMap(DGroups::stream));

	}
}


/**
 * A stream of groups
 */
class DGroups extends DValues  {
	private final Stream<DGroup> groups;

	public DGroups(Stream<DGroup> groups) {
		this.groups = groups;
	}

	@Override
	public Stream<DGroup> stream() {
		return groups;
	}
}

/**
 *  group of data
 */
class DGroup implements DValue  {
	private final Map<Object, Object> contents = new HashMap<>();

	public DGroup(String valueAKey, DValue a, String valueBKey, DValues b) {
		contents.put(valueAKey, a);
		contents.put(valueBKey, b);
	}

	@Override
	public Stream<DGroup> stream() {
		return Collections.singletonList(this).stream();
	}

	@Override
	public DGroups flatMapThenMerge(String myGroupKey, String otherGroupKey, DValues values) {
		return null;
	}

	@Override
	public DValue get(DPath path) {
		return null;
	}

	@Override
	public DValues getAll(DPath path) {
		return null;
	}

	@Override
	public DPoint getParent() {
		return null;
	}

	@Override
	public boolean matchesOneOf(DValues values) {
		return false;
	}

	@Override
	public boolean matches(DValue value) {
		return false;
	}

	@Override
	public boolean matches(Object object) {
		return false;
	}

	@Override
	public String getField() {
		return null;
	}

	@Override
	public Object getActual() {
		return null;
	}

	@Override
	public DValues asValues() {
		return null;
	}
}


class DFilter extends DValues {
	private final DValues source;
	private Predicate<DValue> predicate;

	public DFilter(DValues source, Predicate<DValue> predicate) {
		this.source = source;
		this.predicate = predicate;
	}

	@Override
	public Stream<? extends DValue> stream() {
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

	@Override
	public boolean matches(DValue value) {
		return false;
	}

	@Override
	public boolean matches(Object object) {
		return false;
	}

	private static Object getValue(DArray array, int index) {
		return null;
	}

	@Override
	public DValue get(DPath path) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DValues getAll(DPath path) {
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

	@Override
	public DGroups flatMapThenMerge(String myGroupKey, String otherGroupKey, DValues values) {
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
	public boolean matches(DValue value) {
		return false;
	}

	@Override
	public boolean matches(Object object) {
		return false;
	}

	@Override
	public DPoint getParent() {
		return parent;
	}

	@Override
	public DValue get(DPath path) {
		return null;
	}

	
	@Override
	public DValues getAll(DPath path) {
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

	@Override
	public DGroups flatMapThenMerge(String myGroupKey, String otherGroupKey, DValues values) {
		return null;
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
	public boolean matches(DValue value) {
		return false;
	}

	@Override
	public boolean matches(Object object) {
		return false;
	}

	@Override
	public DPoint getParent() {
		return parent;
	}

	@Override
	public DValue get(DPath path) {
		DPath first = path.first();
		DValue value = DValueFac.asValue(this, first.name(), data.get(first.name()));
		if (path.size() == 1) {
			return value;
		}
		else {
			DPath rest = path.tail();
			return value.get(rest);
		}
	}

	@Override
	public DValues getAll(DPath path) {
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
	public DGroups flatMapThenMerge(String myGroupKey, String otherGroupKey, DValues values) {
		return null;
	}

	@Override
	public DValues asValues() {
		// TODO Auto-generated method stub
		return null;
	}	
	
}

