package net.natpad.dung.expression;

public class StringLiteralProducer implements ExpressionValueProducer<String> {

	private static StringLiteralProducer instance = new StringLiteralProducer();
	
	public static StringLiteralProducer instance() {
		return instance;
	}
	
	@Override
	public IExpressionValue produce(String in) {
		return in == null ? null : new StringLiteral(in);
	}
}
