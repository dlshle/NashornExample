package com.company;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    static public class BoxInt {
        private int n;
        public BoxInt(int n) {
            this.n = n;
        }

        public int getN() {
            return n;
        }

        @Override
        public String toString() {
            return String.valueOf(n);
        }
    }

    static public class FibCalculator {
        private static final int CAP = 101;

        private int[] dp;

        public FibCalculator() {
            dp = new int[CAP];
            Arrays.fill(dp, -1);
            dp[0] = 0;
            dp[1] = 1;
        }

        public BoxInt get(int n) {
            return new BoxInt(calculate(n));
        }

        public int calculate(int n) {
            if (dp[n] != -1) {
                return dp[n];
            }
            dp[n] = calculate(n-1) + calculate(n-2);
            return dp[n];
        }
    }

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
        System.out.println("fib func avg: " + runFibFunNTimes(100));
        System.out.println("fib binding avg: " + runFibBindingNTimes(100));
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

    /*
    private static void runWithCompiledFunction() {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        String script = "function convert(x) { return  Packages."+BoxVal.class.getName()+".of(x);}";
        try {
            CompiledScript compiled = ((Compilable)engine).compile(script);
            Invocable invocable = (Invocable) compiled.getEngine();
            System.out.println(invocable.invokeFunction("convert", "a"));
            System.out.println(invocable.invokeFunction("convert", "b"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
     */

    private static double runFibFunNTimes(int n) {
        long[] diffTimes = new long[n];
        AtomicInteger i = new AtomicInteger(0);
        runNtimes(() -> diffTimes[i.getAndIncrement()] = measurePerformance(() -> runFibFunc(80)), n);
        long tot = 0;
        for (int j = 0; j < n; j++) {
            tot += diffTimes[j];
        }
        return tot / (n * 1.0);
    }

    private static double runFibBindingNTimes(int n) {
        long[] diffTimes = new long[n];
        AtomicInteger i = new AtomicInteger(0);
        runNtimes(() -> diffTimes[i.getAndIncrement()] = measurePerformance(() -> runFibBinding(80)), n);
        long tot = 0;
        for (int j = 0; j < n; j++) {
            tot += diffTimes[j];
        }
        return tot / (n * 1.0);
    }

    private static void runFibFunc(int n) {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        try {
            engine.eval("function fib(x) { var fib = new "+FibCalculator.class.getName()+"(); return fib.get(x); }");
            // can not pass bindings to engine.eval if you want to execute pure function
            Invocable invocable = (Invocable) engine;
            ((BoxInt)invocable.invokeFunction("fib", n)).getN();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runFibBinding(int n) {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
        String script = "function calc(x) { return new " +FibCalculator.class.getName()+"().get(x);}; var a = calc(p);";
        Bindings bindings = engine.createBindings();
        bindings.put("p", n);
        try {
            CompiledScript compiled = ((Compilable) engine).compile(script);
            compiled.eval(bindings);
            ((BoxInt)bindings.get("a")).getN();
            // can not execute convert from compiled script because after the compilation of the script, no function can be found from the compiled script
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void runNtimes(Runnable fn, int n) {
        for (int i  = 0; i < n; i++) {
            fn.run();
        }
    }

    private static long measurePerformance(Runnable fn) {
        Instant before = Instant.now();
        fn.run();
        long diff = Instant.now().toEpochMilli() - before.toEpochMilli();
        return diff;
    }
}
