package net.natpad.dung.expression;

import java.util.List;

public class ArrayLiteral<T> implements IExpressionValue {

	public final List<T> list;
	public final ExpressionValueProducer<T> producer;
	
	public ArrayLiteral(List<T> list, ExpressionValueProducer<T> producer ) {
		this.list = list;
		this.producer = producer;
	}
	
	
	@Override
	public IExpressionValue getById(Object id) {
		int offset = -1;
		if (id instanceof Number) {
			offset = ((Number) id).intValue();
		} else {
			offset = Integer.parseInt(id.toString());
		}
	
		return offset<0 ? null : producer.produce(list.get(offset));
	}
	
	@Override
	public String toString() {
		return list.toString();
	}

}
