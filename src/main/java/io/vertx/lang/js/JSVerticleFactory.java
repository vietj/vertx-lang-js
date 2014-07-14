/*
 * Copyright 2014 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.lang.js;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.spi.VerticleFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.net.URL;
import java.util.Scanner;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class JSVerticleFactory implements VerticleFactory {

  private Vertx vertx;
  private ScriptEngine engine;

  @Override
  public void init(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public String prefix() {
    return "js";
  }
  @Override
  public Verticle createVerticle(String verticleName, ClassLoader classLoader) throws Exception {
    return new JSVerticle(verticleName);
  }

  private class JSVerticle extends AbstractVerticle {

    private final String verticleName;

    private JSVerticle(String verticleName) {
      this.verticleName = verticleName;
    }

    @Override
    public void start() throws Exception {
      init();
      engine.eval("require('" + verticleName + "');");
    }

    @Override
    public void stop() throws Exception {
      // TODO
    }
  }

  @Override
  public void close() {
  }

  private synchronized void init() {
    if (engine == null) {
      ScriptEngineManager mgr = new ScriptEngineManager();
      engine = mgr.getEngineByName("nashorn");
      if (engine == null) {
        throw new IllegalStateException("Cannot find Nashorn JavaScript engine - maybe you are not running with Java 8 or later?");
      }
      URL url = getClass().getClassLoader().getResource("vertx-js/util/require.js");
      if (url == null) {
        throw new IllegalStateException("Cannot find vertx/util/require.js on classpath");
      }
      try (Scanner scanner = new Scanner(url.openStream(), "UTF-8").useDelimiter("\\A")) {
        String requireJS = scanner.next();
        engine.put(ScriptEngine.FILENAME, "require.js");
        engine.eval(requireJS);
      } catch (Exception e) {
        throw new IllegalStateException("Failed to load vertx/require.js", e);
      }
      try {
        // Put the globals in
        engine.put("__jvertx", vertx);
        // As a temporary hack we also put the engine itself in, this allows us to set script name from JS
        // which we need to do until # sourceURL = is supported so we can name evals
        engine.put("__engine", engine);
        engine.eval("var Vertx = require('vertx-js/vertx'); var vertx = new Vertx(__jvertx); var console = require('vertx-js/util/console');");
      } catch (ScriptException e) {
        throw new IllegalStateException("Failed to eval: " + e.getMessage(), e);
      }
    }
  }

}
