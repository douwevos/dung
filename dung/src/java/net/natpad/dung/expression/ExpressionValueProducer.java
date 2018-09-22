package net.natpad.dung.expression;

public interface ExpressionValueProducer<T> {

	public IExpressionValue produce(T in);
	
}
