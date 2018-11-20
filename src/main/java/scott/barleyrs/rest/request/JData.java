package scott.barleyrs.rest.request;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static scott.barleyrs.rest.request.DPath.path;
import static scott.barleyrs.rest.request.DPred.matches;
import static scott.barleyrs.rest.request.Helper.collectAndPrint;

public class JData {

	public static void main(String args[]) throws IOException {

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

		File from = new File("src/test/resources/misc.json");
		ObjectMapper mapper = new ObjectMapper();
		TypeReference<HashMap<String,Object>> typeRef
				= new TypeReference<HashMap<String,Object>>() {};
		HashMap<String,Object> rootData = mapper.readValue(from, typeRef);


		DValue root = DValueFac.asValue(null, null, rootData);

		//get the array of order part views
		DArray opArray  = root.get(path("orderPartViewDTO2s"));
		System.out.println(opArray);

		//get the id of the first order part view
		DValue id1 = opArray.get(0).get(path("id"));
		System.out.println(id1.getActual());

		//get the id of the first order part view
		try  {
			opArray.get(1).get(path("id"));
		}
		catch(ArrayIndexOutOfBoundsException x) {}

		//get all order part ids
		DValues ids = root.getAll(path("orderPartViewDTO2s", "id"));
		ids.stream().forEach(it -> System.out.println(it.getActual()));



		DValues poIdsForAllVouchers = root.getAll(path("orderPartViewDTO2s", "voucher", "print_object", "id"));
		System.out.println("print object ids across all vouchers");
		poIdsForAllVouchers.stream().forEach(it -> System.out.println(it.getActual()));


		/*
		 * all passengers who have no class information in their products
		 */
		DValues noClassPass = root.getAll(path("orderPartViewDTO2s", "voucher", "product_details", "pricing", "no_class", "parts", "passengers"));

		/*
		 * all passengers in the journey data
		 */
		DValues allPassengers = root.getAll(path("orderPartViewDTO2s", "journeys", "passengerData", "passengers"));


		/*
		System.out.println("NO CLASS PASS");
		noClassPass.stream().forEach(it -> System.out.println(it.getActual()));

		System.out.println("=====================");
		allPassengers.stream()
				.forEach(it -> System.out.println(((DValue) it).getActual()));
*/
		System.out.println("000000000000000000000000000000000000000000000000=");
		noClassPass
			/*
			 * cross product of all noClPass IDS and passengers
			 */
		  .crossProduct("pid", "passenger", allPassengers)

		  /*
		   * filter where noClPass == passengers.@id
		   */
		  .filter(matches(path("pid"), path("passenger", "@id")))

		  /*
		   * stream and print
		   */
		  .stream()
		  .map(dg -> (DValue)dg.get(path("passenger")))
		  .forEach(it -> System.out.println("!!!!! " + it.getActual()));
    							  
			//.forEach(it -> System.out.println("!!!!" + it.get(path("pid")).getActual() + "  " + it.get(path("passengers")).getActual()));

		
		System.out.println("FINAL BEST BEUTIFUL AND AWESOME");
	
		DValues noClassPassengerData = noClassPass
				  .crossProduct("pid", "passenger", allPassengers)
				  .filter(matches(path("pid"), path("passenger", "@id")))
				  .getAll(path("passenger"));
		
		noClassPassengerData.stream()
				  .forEach(JData::printPassenger);

	}
	
	private static void printPassenger(DValue p) {
		System.out.println(
				"ID = " + p.get(path("@id")).getActual() + 
				"  TYPE = " + p.get(path("type")).getActual() + 
				"  CHALLENGED = " + p.get(path("challenged")).getActual());		
	}
}

class Helper {
	public static <T> Stream<T> collectAndPrint(Object ctx, Stream<T> stream) {
		return stream;
//		List<T> list = stream.collect(Collectors.toList());
//		System.out.println(ctx + " ==> " + list);
//		return list.stream();
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

	public String toString() {
		if (next == null) {
			return name;
		}
		return name + " -> " + next.toString();
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

	<T extends DValue >T get(DPath path);

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
		return Objects.equals(getActual(), value.getActual());
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
		if (value == null) {
			return Collections.<DValue>emptyList().stream();
		}
		return Collections.<DValue>singletonList(this).stream();
	}


	@Override
	public DGroups flatMapThenMerge(String myGroupKey, String otherGroupKey, DValues otherValues) {
		//DGroup group = new DGroup(myGroupKey, this, otherGroupKey, otherValues);
		DValue me = this;
		List<DGroup> groups = otherValues.stream()
				.map(ov -> new DGroup(myGroupKey, me, otherGroupKey, ov))
				.collect(Collectors.toList());
		return new DGroups(groups);
	}

	@Override
	public DValue get(DPath path) {
		DPath first = path.first();
		DValue value = DValueFac.asValue(this, first.name(), null);
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
		return new DEmptyValues(this, path);
	}

	@Override
	public Object getActual() {
		return value;
	}

	@Override
	public String toString() {
		if (parent != null) {
			return parent.toString() + " <- " + field + "(DScalar "  + value + ")";
		}
		return "(DScalar " + value + ")";
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
		// for each id, stream otherValues
		return new DGroups(
        	stream()
             .map(v -> v.flatMapThenMerge(myGroupKey, otherGroupKey, otherValues))
            .flatMap(DGroups::stream)
			.collect(Collectors.toList()));
	}

	public abstract DValues getAll(DPath rest);
}

class DEmptyValues extends DValues {
	private DPoint parent;
	private DPath path;

	public DEmptyValues(DPoint parent, DPath path) {
		this.parent = parent;
		this.path = path;
	}

	@Override
	public DValues getAll(DPath path) {
		return this;
	}

	@Override
	public Stream<? extends DValue> stream() {
		return Collections.<DValue>emptyList().stream();
	}

	@Override
	public String toString() {
		if (parent == null) {
			return "DEmptyValues";
		}
		return parent.toString() + " <- " + "DEmptyValues";
	}
}

class DAdhocValues extends DValues {
	
	private final DValue value;

	public DAdhocValues(DValue value) {
		this.value = value;
	}

	@Override
	public Stream<? extends DValue> stream() {
		return Collections.singletonList(value).stream();
	}

	@Override
	public DValues getAll(DPath path) {
		return value.getAll(path);
	}
	
}




/**
 * A stream of groups
 */
class DGroups extends DValues  {
	private final List<DGroup> groups;

	public DGroups(List<DGroup> groups) {
		this.groups = groups;
		//this.groups = collectAndPrint(this, groups);
	}

	@Override
	public Stream<DGroup> stream() {
		return groups.stream();
	}

	@Override
	public DValues getAll(DPath path) {
		DPath first = path.first();
		DFieldAcrossValues fieldAcross = new DFieldAcrossValues(this, first.name());
		if (path.size() == 1) {
			return fieldAcross;
		}
		else {
			DPath rest = path.tail();
			return fieldAcross.getAll(rest);
		}
	}
}

/**
 *  group of data
 */
class DGroup extends DValues implements DValue  {
	private final Map<Object, Object> contents = new HashMap<>();

	public DGroup(String valueAKey, DValue a, String valueBKey, DValue b) {
		contents.put(valueAKey, a);
		contents.put(valueBKey, b);
	}

	@Override
	public Stream<DValue> stream() {
		return contents.values().stream()
				.map(o -> (DValue)o);
	}

	@Override
	public DGroups flatMapThenMerge(String myGroupKey, String otherGroupKey, DValues values) {
		return null;
	}

	@Override
	public DValue get(DPath path) {
		DPath first = path.first();
		DValue dvalue = DValueFac.asValue(this, first.name(), contents.get(first.name()));
		if (path.size() == 1) {
			return dvalue;
		}
		return dvalue.get(path.tail());
	}

	@Override
	public DValues getAll(DPath path) {
		DPath first = path.first();
		DValue dvalue = DValueFac.asValue(this, first.name(), contents.get(first.name()));
		if (path.size() == 1) {
			return new DAdhocValues(dvalue);
		}
		else {
			DPath rest = path.tail();
			return dvalue.getAll(rest);
		}
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

}

/**
 * A projection of a single field across many values
 * so users* -> userids* - via user.id
 */
class DFieldAcrossValues extends DValues {
	private final DValues parent;
	private final String field;
	public DFieldAcrossValues(DValues parent, String field) {
		this.parent = parent;
		this.field = field;
	}

	@Override
	public DValues getAll(DPath path) {
		DPath first = path.first();
		DFieldAcrossValues fieldAcross = new DFieldAcrossValues(this, first.name());
		if (path.size() == 1) {
			return fieldAcross;
		}
		else {
			DPath rest = path.tail();
			return fieldAcross.getAll(rest);
		}
	}

	@Override
	public Stream<? extends DValue> stream() {
		return collectAndPrint(this, parent.stream())
				//for each DV get all values for the field
				.map(dv -> 
					dv.getAll(path(field)))
				//flatmap to stream dv
				.flatMap(DValues::stream);
	}

	@Override
	public String toString() {
		if (parent != null) {
			return parent.toString() + " <- " + field + " (FAMV)";
		}
		return String.valueOf(field) + " (FAMV)";
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
	public DValues getAll(DPath path) {
		DPath first = path.first();
		DFieldAcrossValues fieldAcross = new DFieldAcrossValues(this, first.name());
		if (path.size() == 1) {
			return fieldAcross;
		}
		else {
			DPath rest = path.tail();
			return fieldAcross.getAll(rest);
		}
	}

	@Override
	public Stream<? extends DValue> stream() {
		return collectAndPrint(this, source.stream()).filter(predicate);
	}
}


class DArrayItem implements DValue {
	private final DArray array;
	private final int index;
	private final DValue dvalue;

	public DArrayItem(DArray array, int index) {
		this.array = array;
		this.index = index;
		this.dvalue = DValueFac.asValue(this, "array-item", array.getActual().get(index));
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
	public DValue get(DPath path) {
		return dvalue.get(path);
	}

	@Override
	public DValues getAll(DPath path) {
		return dvalue.getAll(path);
	}

	@Override
	public DPoint getParent() {
		return array;
	}

	@Override
	public String getField() {
		return "" + index;
	}

	@Override
	public Object getActual() {
		return dvalue.getActual();
	}

	@Override
	public Stream<DArrayItem> stream() {
		return Collections.singletonList(this).stream();
	}

	@Override
	public DGroups flatMapThenMerge(String myGroupKey, String otherGroupKey, DValues values) {
		return dvalue.flatMapThenMerge(myGroupKey, otherGroupKey, values);
	}

	@Override
	public String toString() {
		if (array != null) {
			return array.toString() + " <- " + index+ " (DArrayIndex)";
		}
		return String.valueOf(index) + " (DArrayIndex)";
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
		DPath first = path.first();
		DFieldAcrossValues fieldAcross = new DFieldAcrossValues(this, first.name());
		if (path.size() == 1) {
			return fieldAcross;
		}
		else {
			DPath rest = path.tail();
			return fieldAcross.getAll(rest);
		}
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
	public List<Object> getActual() {
		return list;
	}

	@Override
	public String toString() {
		if (parent != null) {
			return parent.toString() + "<- " + field + " (DArray)";
		}
		return String.valueOf(field) + " (DArray)";

	}
}

class DValueFac {
	
	public static DValue asValue(DPoint parent, String field, Object object) {
		if (object instanceof  DValue) {
			return (DValue)object;
		}
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
		DPath first = path.first();
		DValue value = DValueFac.asValue(this, first.name(), data.get(first.name()));
		if (path.size() == 1) {
		  if (value instanceof DArray) {
			return (DArray)value;
		  }
		  else {
			return new DArray(this, "asarray", Collections.singletonList(value.getActual()));
		  }
		}
		else {
			DPath rest = path.tail();
			return value.getAll(rest);
		}
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
		return Collections.<DValue>singletonList(this).stream();
		//each DValue has this map as it's parent and it's field is the map key.
//		return data.entrySet().stream()
//				.map(en -> DValueFac.asValue(this, en.getKey(), en.getValue()));
	}

	@Override
	public DGroups flatMapThenMerge(String myGroupKey, String otherGroupKey, DValues values) {
		return null;
	}

	@Override
	public String toString() {
		if (parent != null) {
			return parent.toString() + "<- " + field + " (DMap)";
		}
		return String.valueOf(field) + " (DMap)";
	}
}

