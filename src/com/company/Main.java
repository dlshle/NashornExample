package com.company;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Main {

    public enum BoxVal {
        A("a"),
        B("b");

        private String value;

        BoxVal(String val) {
            value = val;
        }

        public static BoxVal of(String val) {
            return Arrays.stream(BoxVal.values()).filter(v -> v.value.equals(val)).findFirst().orElse(null);
        }
    }

    static public class Box {
        private BoxVal value;
        private Box nested;
        Box(BoxVal value, Box nested) {
            this.value = value;
            this.nested = nested;
        }

        public BoxVal getValue() {
            return value;
        }

        public Box getNested() {
            return nested;
        }

        public void setNested(Box box) {
            nested = box;
        }

        public static int compute(int a, int b)  {
            return a + b;
        }

        @Override
        public String toString() {
            return String.format("Box:{val:%s, nested:%s}", value, nested == null?"null":nested);
        }
    }

    public static void main(String[] args) {
        runWithoutFunction();
        runWithFunction();
    }

    private static void runWithoutFunction() {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        Map<String, String> map = new HashMap<>();
        Box box1 = new Box(BoxVal.A, null);
        Box box2 = new Box(BoxVal.B, null);
        box1.setNested(box2);
        map.put("a", "b");
        Bindings bindings = engine.createBindings();
        bindings.put("map", map);
        bindings.put("greeting", "hi");
        bindings.put("box1", box1);
        bindings.put("box2", box2);
        bindings.put("seed", 123);
        String script = "function convert(x) { return Packages."+BoxVal.class.getName()+".of(x);}; print(map, box1, box2);print(box1.toString());var random = Math.random(seed);print(\"result:\",Packages."+Box.class.getName()+".compute(1, 2));var enumVal = Packages."+BoxVal.class.getName()+".of('a');";
        System.out.println(script);
        try {
            CompiledScript compiled = ((Compilable) engine).compile(script);
            compiled.eval(bindings);
            // can not execute convert from compiled script because after the compilation of the script, no function can be found from the compiled script
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(bindings.get("random"));
        System.out.println(bindings.get("enumVal").getClass().getName());
    }

    private static void runWithFunction() {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        try {
            engine.eval("function convert(x) { return  Packages."+BoxVal.class.getName()+".of(x);}");
            // can not pass bindings to engine.eval if you want to execute pure function
            Invocable invocable = (Invocable) engine;
            System.out.println(invocable.invokeFunction("convert", "a"));
            System.out.println(invocable.invokeFunction("convert", "b"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
