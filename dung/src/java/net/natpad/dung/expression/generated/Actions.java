/* generated by natpad.cup Tue Mar 28 17:45:45 CEST 2017 */
package net.natpad.dung.expression.generated;

import net.natpad.dung.expression.ExpressionHelper;
import net.natpad.dung.expression.IExpressionValue;
import net.natpad.dung.expression.StringLiteral;
import net.natpad.dung.expression.generated.runtime.LrParserContext;
import net.natpad.dung.expression.generated.runtime.LrSymbol;

public class Actions {


	Actions() {
	}

	/** Method with the actual generated action code. */
	public final LrSymbol runAction(LrParserContext cupContext, int cupActionId) throws Exception {
		/* Symbol object for return from actions */
		LrSymbol cupResult;

		/* select the action based on the action number */
		switch (cupActionId) {
			case 0: { // $START ::= expression EOF 
				Object RESULT = null;
				LrSymbol cupstart_val = cupContext.getFromTop(1);
				IExpressionValue start_val = (IExpressionValue) cupstart_val.value;
				RESULT = start_val;
				cupResult = new LrSymbol(0/*$START*/, RESULT);
			}
			/* ACCEPT */
			cupContext.doneParsing();
			return cupResult;

			case 1: { // expression ::= value 
				IExpressionValue RESULT = null;
				LrSymbol cupv = cupContext.getFromTop(0);
				IExpressionValue v = (IExpressionValue) cupv.value;
				RESULT=v;
				cupResult = new LrSymbol(1/*expression*/, RESULT);
			}
			return cupResult;

			case 2: { // expression ::= expression DOT ID 
				IExpressionValue RESULT = null;
				LrSymbol cupe = cupContext.getFromTop(2);
				IExpressionValue e = (IExpressionValue) cupe.value;
				LrSymbol cupi = cupContext.getFromTop(0);
				Object i = (Object) cupi.value;

				RESULT = e.getById(i);

				cupResult = new LrSymbol(1/*expression*/, RESULT);
			}
			return cupResult;

			case 3: { // value ::= variable_value 
				IExpressionValue RESULT = null;
				LrSymbol cupv = cupContext.getFromTop(0);
				IExpressionValue v = (IExpressionValue) cupv.value;
				RESULT=v;
				cupResult = new LrSymbol(2/*value*/, RESULT);
			}
			return cupResult;

			case 4: { // value ::= primitive_value 
				IExpressionValue RESULT = null;
				LrSymbol cupv = cupContext.getFromTop(0);
				IExpressionValue v = (IExpressionValue) cupv.value;
				RESULT=v;
				cupResult = new LrSymbol(2/*value*/, RESULT);
			}
			return cupResult;

			case 5: { // variable_value ::= ID 
				IExpressionValue RESULT = null;
				LrSymbol cupi = cupContext.getFromTop(0);
				Object i = (Object) cupi.value;

				ExpressionHelper exprHelper = (ExpressionHelper) cupContext;
				RESULT = exprHelper.getById(i);

				cupResult = new LrSymbol(3/*variable_value*/, RESULT);
			}
			return cupResult;

			case 6: { // variable_value ::= ID LBRACK expression RBRACK 
				IExpressionValue RESULT = null;
				LrSymbol cupi = cupContext.getFromTop(3);
				Object i = (Object) cupi.value;
				LrSymbol cupe = cupContext.getFromTop(1);
				IExpressionValue e = (IExpressionValue) cupe.value;

				ExpressionHelper exprHelper = (ExpressionHelper) cupContext;
				IExpressionValue iev = exprHelper.getById(i);
				RESULT = iev.getById(e);

				cupResult = new LrSymbol(3/*variable_value*/, RESULT);
			}
			return cupResult;

			case 7: { // primitive_value ::= STRING 
				IExpressionValue RESULT = null;
				LrSymbol cups = cupContext.getFromTop(0);
				String s = (String) cups.value;
				RESULT=new StringLiteral(s);
				cupResult = new LrSymbol(4/*primitive_value*/, RESULT);
			}
			return cupResult;

			case 8: { // primitive_value ::= NUMBER 
				IExpressionValue RESULT = null;

				cupResult = new LrSymbol(4/*primitive_value*/, RESULT);
			}
			return cupResult;

			case 9: { // primitive_value ::= NIL 
				IExpressionValue RESULT = null;

				cupResult = new LrSymbol(4/*primitive_value*/, RESULT);
			}
			return cupResult;

			default:
				throw new Exception("Invalid action number found in internal parse table");
			}
		}

}
