package com.google.refine.jruby;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.expr.LanguageSpecificParser;
import com.google.refine.expr.ParsingException;

import java.util.Properties;

import org.apache.jena.base.Sys;
import org.jruby.embed.ScriptingContainer;

import javax.script.*;

public class JRubyEvaluable implements Evaluable {

    private final String sFunctionName;
    private static final ScriptEngine engine;

    static public LanguageSpecificParser createParser() {
        return new LanguageSpecificParser() {

            @Override
            public Evaluable parse(String s) throws ParsingException {
                return new JRubyEvaluable(s);
            }
        };
    }

    static {
        ScriptEngineManager manager = new ScriptEngineManager();
        engine = manager.getEngineByName("jruby");
    }

    public JRubyEvaluable(String s) {
        sFunctionName = String.format("__temp_%d__", Math.abs(s.hashCode()));

        String sb = "def " + sFunctionName + "(value = 0, cell = nil, cells = nil, row = nil, rowIndex = nil)\n";
        sb += s + "\n";
        sb += "end\n";

        try {
            engine.eval(sb);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object evaluate(Properties properties) {
        try {
            Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
            bindings.put("_value", properties.get("value"));
            bindings.put("_cell", properties.get("cell"));
            bindings.put("_cells", properties.get("cells"));
            bindings.put("_row", properties.get("row"));
            bindings.put("_rowIndex", properties.get("rowIndex"));
            String s = sFunctionName + "(_value, _cell, _cells, _row, _rowIndex)";
            return engine.eval(s);
        } catch (ScriptException e) {
            return new EvalError(e.toString());
        }
    }
}
