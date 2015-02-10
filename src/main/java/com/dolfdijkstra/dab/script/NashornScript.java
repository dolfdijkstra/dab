package com.dolfdijkstra.dab.script;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import jdk.nashorn.internal.objects.NativeArray;
import jdk.nashorn.internal.runtime.ScriptObject;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;

import com.dolfdijkstra.dab.Script;

public class NashornScript implements Script {

    private final long waitTime;

    private class ScriptItemIterator implements Iterator<ScriptItem> {
        private final Next iterator;
        private ScriptItem next;

        private ScriptItemIterator(Next n) {

            this.iterator = n;
            getnext();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public ScriptItem next() {
            return getnext();
        }

        private ScriptItem getnext() {
            Object o;
            ScriptItem last = next;
            try {
                o = iterator.next();// invokeFunction("next");
                System.out.println(o.getClass());

                if (o instanceof String) {
                    // System.out.println(o);
                    next = new ConstantWaitItem(new HttpGet(o.toString()), waitTime);
                } else if (o instanceof ScriptObject) {
                    ScriptObject so = (ScriptObject) o;
                    String uri = asString(so.get("url"));
                    String method = StringUtils.defaultString(
                            asString(so.get("method")), "GET");
                    if ("GET".equalsIgnoreCase(method)) {
                        HttpGet r = new HttpGet(uri);

                        addHeaders(so, r);
                        next = new ConstantWaitItem(r, waitTime);
                    } else if ("POST".equalsIgnoreCase(method)) {
                        HttpPost r = new HttpPost(uri);
                        addHeaders(so, r);
                        String body = asString(so.get("body"));
                        if (body != null)
                            r.setEntity(new StringEntity(body));
                        // Base64.decodeBase64(body);
                        next = new ConstantWaitItem(r, waitTime);
                    }

                    so.entrySet()
                            .stream()
                            .map(e -> String.format("%s: %s", e.getKey(),
                                    e.getValue())).forEach(System.out::println);
                    // for (Entry<Object, Object> b : b0.getMap()) {
                    // System.out.printf("%s: %s -> %s%n", b.getKey(),
                    // b.getValue()
                    // .getClass().getName(), b.getValue());
                    // }
                } else {
                    System.out
                            .println(Arrays.toString(o.getClass().getInterfaces()));
                    System.out.println(o.getClass().getSuperclass());
                }

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            System.out.printf("next is %s%n", next);
            return last;

        }

        private String asString(Object object) {
            return object == null ? null : String.valueOf(object);
        }

        private void addHeaders(ScriptObject so, HttpRequestBase r) {
            Collection<String> headers = asArray(so.get("headers"));
            headers.stream().map(h -> h.split(":"))
                    .forEach(e -> r.addHeader(e[0].trim(), e[1].trim()));

        }

        @SuppressWarnings("unchecked")
        private <T> Collection<T> asArray(Object o) {
            if (o instanceof NativeArray) {
                NativeArray s = (NativeArray) o;
                return (Collection<T>) s.values();
            }
            return Collections.emptyList();
        }

    }

    // final Class<?> cls;
    // final Method isArray;
    // final Method isFunction;
    // final Method call;
    // final Method values;

    public NashornScript(String name, long waitTime) throws ClassNotFoundException,
            NoSuchMethodException, SecurityException {
        this.name = name;
        this.waitTime = Math.max(0, waitTime);
        // cls = Class.forName("jdk.nashorn.api.scripting.ScriptObjectMirror");
        // Arrays.asList(cls.getMethods())
        // .forEach(
        // m -> System.out.printf("%s %s (%s)%n{}%n", m.getReturnType()
        // .getName(), m.getName(), Arrays.toString(m
        // .getParameters())));
        //
        // isArray = cls.getMethod("isArray");
        // isFunction = cls.getMethod("isFunction");
        // call = cls.getMethod("callMember", String.class, Object[].class);
        // values = cls.getMethod("values");

    }

    public static Script build(String name, long waitTime) throws Exception {
        if (Files.isReadable(Paths.get(name)))
            return new NashornScript(name, waitTime);
        throw new FileNotFoundException(String.format("Cannot read from '%s'", name));
    }

    public static void main(String... args) throws Exception {
        Script s = NashornScript.build("script.js", 0);

        Iterator<ScriptItem> i = s.iterator();
        for (int j = 0; j < 4; j++) {
            ScriptItem item = i.next();
            System.out.println(item.request());
        }
    }

    private String name;

    @Override
    public Iterator<ScriptItem> iterator() {
        final ScriptEngineManager engineManager = new ScriptEngineManager();
        final ScriptEngine engine = engineManager.getEngineByName("nashorn");
        try (Reader reader = new FileReader(name)) {
            engine.eval(reader);
            // Bindings bindings =
            // engine.getBindings(ScriptContext.ENGINE_SCOPE);
            // for (Entry<String, Object> b : bindings.entrySet()) {
            // System.out.printf("%s: %s -> %s%n", b.getKey(), b.getValue()
            // .getClass().getName(), b.getValue());
            // }

            Invocable invocable = (Invocable) engine;

            return new ScriptItemIterator(invocable.getInterface(Next.class));
        } catch (IOException | ScriptException e) {
            throw new RuntimeException(e);
        }
    }

    public interface Next {
        Object next();
    }

}
