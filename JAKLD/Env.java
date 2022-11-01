// A Lisp Driver to be embedded in Java Applications

// The contents of this file are subject to the Mozilla Public License
// Version 1.0 (the "License"); you may not use this file except in
// compliance with the License. You may obtain a copy of the License at
// http://www.mozilla.org/MPL/
//
// Software distributed under the License is distributed on an "AS IS"
// basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
// License for the specific language governing rights and limitations
// under the License.
//
// The Original Code is JAKLD code, released November 26, 2002.
//
// The Initial Developer of the Original Code is Taiichi Yuasa.
// Portions created by Taiichi Yuasa are Copyright (C) 2002
// Taiichi Yuasa. All Rights Reserved.
//
// Contributor(s): Taiichi Yuasa <yuasa@kuis.kyoto-u.ac.jp>

final class Env {

  private Symbol symbol;
  private Object value;
  private Env next;

  Env(Symbol sym, Object val, Env env) {
    symbol = sym;
    value = val;
    next = env;
  }

  static Object vref(Symbol sym, Env env) {
    for (; env != null; env = env.next)
      if (env.symbol == sym)
        return env.value;
    return sym.valueOf();
  }

  static Function fref(Symbol sym, Env env) {
    for (; env != null; env = env.next)
      if (env.symbol == sym)
        return (Function) env.value;
    return sym.functionOf();
  }

  static { Subr.defSpecial("Env", "vset", "set!", 2, 0, false); }
  public static Object vset(Symbol sym, Object expr, Env env) {
    Object val = Eval.eval(expr, env);
    for (; env != null; env = env.next)
      if (env.symbol == sym)
        return env.value = val;

    return sym.setValue(val);
  }

  static Env lambdaBind(Object params, List args, Env env) {
    while (params instanceof Pair) {
      if (args == List.nil)
        throw Eval.error("too few arguments");
      env = new Env((Symbol) ((Pair) params).car, args.car, env);
      params = ((Pair) params).cdr;
      args = (List) args.cdr;
    }
    if (params != List.nil)
      env = new Env((Symbol) params, args, env);
    else if (args != List.nil)
      throw Eval.error("too many arguments");
    return env;
  }

  static Env recBind(Object val, Env newenv, Env env) {
    Env ep;
    for (ep = newenv; ep.next != env; ep = ep.next) ;
    ep.value = val;
    return ep;
  }

  static void recBind1(Object val, Env env) {
    env.value = val;
  }

  static void init() {}
}
