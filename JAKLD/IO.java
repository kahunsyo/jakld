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

import java.io.*;
import java.math.BigInteger;
import java.lang.reflect.*;

final class IO {

  private final static PushbackReader consoleInput
  = new PushbackReader(new InputStreamReader(System.in));

  private static PushbackReader currentInputPort = consoleInput;

  private final static Writer consoleOutput
  = new OutputStreamWriter(System.out);

  private static Writer currentOutputPort = consoleOutput;

  private final static Boolean T = Boolean.TRUE;
  private final static Boolean F = Boolean.FALSE;

  private final static char EOF = (char) -1;
  private final static char multipleEscapeChar = '|';
  private final static char singleEscapeChar = '\\';

  private final static int TAeof = 1;
  private final static int TAkokka = 2;
  private final static int TAdot = 4;

  private final static int CAwhitespace = 0;
  private final static int CAterminating = 1;
  private final static int CAnonTerminating = 2;
  private final static int CAsingleEscape = 3;
  private final static int CAmultipleEscape = 4;
  private final static int CAconstituent = 5;

  private final static int[] charAttribute = new int[128];

  static {
    for (int i = 0; i < 128; i++)
      charAttribute[i] = CAconstituent;

    charAttribute['\t'] = CAwhitespace;
    charAttribute['\n'] = CAwhitespace;
    charAttribute['\013'] = CAwhitespace;
    charAttribute['\f'] = CAwhitespace;
    charAttribute['\r'] = CAwhitespace;
    charAttribute['\034'] = CAwhitespace;
    charAttribute['\035'] = CAwhitespace;
    charAttribute['\036'] = CAwhitespace;
    charAttribute['\037'] = CAwhitespace;
    charAttribute[' '] = CAwhitespace;

    charAttribute[singleEscapeChar] = CAsingleEscape;
    charAttribute[multipleEscapeChar] = CAmultipleEscape;

    charAttribute['('] = CAterminating;
    charAttribute[')'] = CAterminating;
    charAttribute['\''] = CAterminating;
    charAttribute['`'] = CAterminating;
    charAttribute[','] = CAterminating;
    charAttribute['"'] = CAterminating;
    charAttribute[';'] = CAterminating;
    charAttribute['#'] = CAnonTerminating;
  }

  private static int cat(int c) {
    if (c >= 0 && c < 128)
      return charAttribute[c];
    else
      return CAconstituent;
  }

  private final static Object kokkaToken = new Misc("#<right-parenthesis>");
  private final static Object dotToken = new Misc("#<dot>");
  final static Object eofObject = new Misc("#<end-of-file>");

  private static char inRead(PushbackReader in) throws IOException {
    char c = (char) in.read();
    if (c == EOF)
      throw Eval.error("unexpected EOF while reading a character");
    return c;
  }

  private final static StringBuffer tokenBuffer = new StringBuffer();

  private static Object readObject(PushbackReader in, int tokenAllowed)
                       throws IOException {
    char c;
    int a;
    for (;;) {
      do
        if ((c = (char) in.read()) == EOF)
          if ((tokenAllowed & TAeof) != 0)
            return eofObject;
          else
            throw Eval.error("unexpected EOF while reading an object");
      while ((a = cat(c)) == CAwhitespace);

      if (a == CAterminating || a == CAnonTerminating) {
        Object x = charMacroReader(c, in, tokenAllowed);
        if (x == null)
          continue;
        else
          return x;
      }
      tokenBuffer.setLength(0);
      boolean escape = false;
      for (;;) {
        if (a == CAconstituent || a == CAnonTerminating) {
          tokenBuffer.append(c);
        } else if (a == CAsingleEscape) {
          escape = true;
          tokenBuffer.append(inRead(in));
        } else if (a == CAmultipleEscape) {
          escape = true;
          for (;;) {
            c = inRead(in);
            a = cat(c);
            if (a == CAmultipleEscape)
              break;
            else if (a == CAsingleEscape)
              c = inRead(in);
            tokenBuffer.append(c);
          }
        } else if (a == CAterminating) {
          in.unread(c);
          break;
        } else if (a == CAwhitespace)
          break;

        if ((c = (char) in.read()) == EOF) break;
        a = cat(c);
      }
      String s = tokenBuffer.toString();
      if (escape)
        return Symbol.intern(s);
      else if (s.equals("."))
        if ((tokenAllowed & TAdot) != 0)
          return dotToken;
        else
          throw Eval.error("dot '.' in a wrong place");
      else
        try {
          return readNumber(s, 10);
        } catch (NumberFormatException e) {
          return Symbol.intern(s);
        }
    }
  }

  private static String readToken(PushbackReader in) throws IOException {
    char c;
    int a;
    tokenBuffer.setLength(0);
    for (;;) {
      if ((c = (char) in.read()) == EOF)
        break;
      else if ((a = cat(c)) == CAterminating) {
        in.unread(c);
        break;
      } else if (a == CAwhitespace)
        break;
      else if (a == CAsingleEscape || a == CAmultipleEscape)
        throw Eval.error("escape '" + c + "' not allowed");
      else
        tokenBuffer.append(c);
    }
    return tokenBuffer.length() == 0 ? "" : tokenBuffer.toString();
  }

  static Number readNumber(String s, int radix)
                throws NumberFormatException {
    if (s.length() > 0 && s.charAt(0) == '+')
      s = s.substring(1);
    try {
      return Num.makeInt(Integer.parseInt(s, radix));
    } catch (NumberFormatException e) {}
    try {
      return new BigInteger(s, radix);
    } catch (NumberFormatException e) {
      if (radix == 10)
        return new Double(s);
      else
        throw e;
    }
  }

  private final static Symbol Squote = Symbol.intern("quote");
  private final static Symbol Squasiquote = Symbol.intern("quasiquote");
  private final static Symbol Sunquote = Symbol.intern("unquote");
  private final static Symbol SunquoteSplicing
                              = Symbol.intern("unquote-splicing");

  private static
  Object charMacroReader(char c, PushbackReader in, int tokenAllowed)
         throws IOException {
    switch (c) {
    case '(': {
      Object x = readObject(in, TAkokka);
      if (x == kokkaToken)
        return List.nil;

      Pair val = new Pair(x, List.nil);
      Pair last = val;

      for (;;) {
        x = readObject(in, TAkokka | TAdot);
        if (x == kokkaToken)
          return val;
        else if (x == dotToken) {
          last.cdr = readObject(in, 0);
          if (readObject(in, TAkokka) != kokkaToken)
            throw Eval.error("right parenthesis ')' missing");
          return val;
        } else {
          Pair y = new Pair(x, List.nil);
          last.cdr = y;
          last = y;
        }
      }
    }
    case ')':
      if ((tokenAllowed & TAkokka) != 0)
        return kokkaToken;
      else
        throw Eval.error("right parenthesis ')' in a wrong place");
    case '\'':
      return List.list(Squote, readObject(in, 0));
    case '`':
      return List.list(Squasiquote, readObject(in, 0));
    case ',':
      if ((c = inRead(in)) == '@')
        return List.list(SunquoteSplicing, readObject(in, 0));
      else {
        in.unread(c);
        return List.list(Sunquote, readObject(in, 0));
      }
    case '"':
      tokenBuffer.setLength(0);
      while ((c = inRead(in)) != '"') {
        if (cat(c) == CAsingleEscape)
          c = inRead(in);
        tokenBuffer.append(c);
      }
      return tokenBuffer.toString();
    case ';':
      while ((c = (char) in.read()) != '\n')
        if (c == EOF) return null;
      return null;
    case '#':
      return sharpSignMacroReader(in);
    default:
      throw Eval.systemError("undefined # reader");
    }
  }

  private static Object sharpSignMacroReader(PushbackReader in)
                        throws IOException {
    String s;
    char c = inRead(in);
    switch (c) {
    case 't':
      if ((s = readToken(in)).length() != 0)
         throw Eval.error("#t followed by garbage \"" + s + "\"");
      return T;
    case 'f':
      if ((s = readToken(in)).length() != 0)
         throw Eval.error("#f followed by garbage \"" + s + "\"");
      return F;
    case '\\': {
      c = inRead(in);
      s = readToken(in);
      if (s.length() == 0)
        return Char.makeChar(c);
      else if (c == 's') {
        if (s.equals("pace")) return Char.makeChar(' ');
      } else if (c == 'n') {
        if (s.equals("ewline")) return Char.makeChar('\n');
      } else if (c == 't') {
        if (s.equals("ab")) return Char.makeChar('\t');
      } else if (c == 'f') {
        if (s.equals("ormfeed")) return Char.makeChar('\f');
      } else if (c == 'r') {
        if (s.equals("eturn")) return Char.makeChar('\r');
      }
      throw Eval.error("unknown character #\\" + c + s);
    }
    case '(': {
      List list = List.nil;
      Object x;
      while ((x = readObject(in, TAkokka)) != kokkaToken)
        list = new Pair(x, list);
      return List.list2vector(List.nreverse(list));
    }
    case 'b':
      s = readToken(in);
      try {
        return readNumber(s, 2);
      } catch (NumberFormatException e) {
        throw Eval.error("bad number format #b" + s);
      }
    case 'o':
      s = readToken(in);
      try {
        return readNumber(s, 8);
      } catch (NumberFormatException e) {
        throw Eval.error("bad number format #o" + s);
      }
    case 'd':
      s = readToken(in);
      try {
        return readNumber(s, 10);
      } catch (NumberFormatException e) {
        throw Eval.error("bad number format #d" + s);
      }
    case 'x':
      s = readToken(in);
      try {
        return readNumber(s, 16);
      } catch (NumberFormatException e) {
        throw Eval.error("bad number format #x" + s);
      }
    default:
      throw Eval.error("unknown syntax #" + c);
    }
  }

  private static void writeObject(Object x, Writer out) throws IOException {
    if (x == null)
      out.write("#<null>");
    else if (x instanceof Boolean)
      out.write(x == T ? "#t" : "#f");
    else if (x instanceof Number) {
      if (x instanceof Double
           && (((Double) x).isInfinite() || ((Double) x).isNaN())) {
        out.write("#<"); out.write(x.toString()); out.write(">");
      } else
        out.write(x.toString());
    }
    else if (x instanceof Character) {
      char c = ((Character) x).charValue();
      out.write("#\\");
      if (c == ' ') out.write("space");
      else if (c == '\n') out.write("newline");
      else if (c == '\t') out.write("tab");
      else if (c == '\f') out.write("formfeed");
      else if (c == '\r') out.write("return");
      else out.write(c);
    }
    else if (x instanceof String) {
      String s = (String) x;
      int len = s.length();
      out.write('\"');
      for (int i = 0; i < len; i++) {
        char c = s.charAt(i);
        if (c == singleEscapeChar || c == '"')
          out.write(singleEscapeChar);
        out.write(c);
      }
      out.write('\"');
    }
    else if (x instanceof List) {
      if (x == List.nil)
        out.write("()");
      else {
        Pair pair = (Pair) x;
        if (pair.car == Squote && pair.cdr instanceof Pair
             && ((Pair) pair.cdr).cdr == List.nil) {
          out.write('\''); writeObject(((Pair) pair.cdr).car, out);
        } else if (pair.car == Squasiquote && pair.cdr instanceof Pair
                    && ((Pair) pair.cdr).cdr == List.nil) {
          out.write('`'); writeObject(((Pair) pair.cdr).car, out);
        } else if (pair.car == Sunquote && pair.cdr instanceof Pair
                    && ((Pair) pair.cdr).cdr == List.nil) {
          out.write(','); writeObject(((Pair) pair.cdr).car, out);
        } else if (pair.car == SunquoteSplicing && pair.cdr instanceof Pair
                    && ((Pair) pair.cdr).cdr == List.nil) {
          out.write(",@"); writeObject(((Pair) pair.cdr).car, out);
        } else {
          out.write('(');
          writeObject(pair.car, out);
          while ((x = pair.cdr) instanceof Pair) {
            pair = (Pair) x;
            out.write(' ');
            writeObject(pair.car, out);
          }
          if (x != List.nil) {
            out.write(" . ");
            writeObject(x, out);
          }
          out.write(')');
        }
      }
    }
    else if (x instanceof Object[]) {
      out.write("#(");
      int len = Array.getLength(x);
      if (len > 0) {
        Object[] v = (Object[]) x;
        for (int i = 0;;) {
          writeObject(v[i], out);
          if (++i >= len)
            break;
          out.write(' ');
        }
      }
      out.write(')');
    }
    else if (x instanceof Writer)
      out.write("#<output port>");
    else if (x instanceof PushbackReader)
      out.write("#<input port>");
    else
      out.write(x.toString());
  }

  private static void displayObject(Object x, Writer out)
                      throws IOException {
    if (x == null)
      out.write("#<null>");
    else if (x instanceof Boolean)
      out.write(((Boolean) x).booleanValue() ? "#t" : "#f");
    else if (x instanceof Symbol)
      out.write(Symbol.symbol2string((Symbol) x));
    else if (x instanceof List) {
      out.write('(');
      if (x != List.nil) {
        for (;;) {
          displayObject(((Pair) x).car, out);
          x = ((Pair) x).cdr;
          if (!(x instanceof Pair))
            break;
          out.write(' ');
        }
        if (x != List.nil) {
          out.write(" . ");
          displayObject(x, out);
        }
      }
      out.write(')');
    }
    else if (x instanceof Object[]) {
      out.write("#(");
      int len = Array.getLength(x);
      if (len > 0) {
        Object[] v = (Object[]) x;
        for (int i = 0;;) {
          displayObject(v[i], out);
          if (++i >= len)
            break;
          out.write(' ');
        }
      }
      out.write(')');
    }
    else if (x instanceof Writer)
      out.write("#<output port>");
    else if (x instanceof PushbackReader)
      out.write("#<input port>");
    else
      out.write(x.toString());
  }

  static { Subr.def("IO", "read", 0, 1); }
  public static Object read(PushbackReader in) throws IOException {
    return readObject(in == null ? currentInputPort : in, TAeof);
  }

  static { Subr.def("IO", "readChar", "read-char", 0, 1); }
  public static Object readChar(PushbackReader in) throws IOException {
    if (in == null)
      in = currentInputPort;
    char c = (char) in.read();
    if (c == EOF) return eofObject;
    return Char.makeChar(c);
  }

  static { Subr.def("IO", "peekChar", "peek-char", 0, 1); }
  public static Object peekChar(PushbackReader in) throws IOException {
    if (in == null)
      in = currentInputPort;
    char c = (char) in.read();
    if (c == EOF) return eofObject;
    in.unread(c);
    return Char.makeChar(c);
  }

  static { Subr.def("IO", "charReady", "char-ready?", 0, 1); }
  public static Boolean charReady(PushbackReader in) throws IOException {
    return (in == null ? currentInputPort : in).ready() ? T : F;
  }

  static { Subr.def("IO", "write", 1, 1); }
  public static Object write(Object x, Writer out) throws IOException {
    if (out == null)
      out = currentOutputPort;

    writeObject(x, out);
    out.flush();
    return x;
  }

  static { Subr.def("IO", "display", 1, 1); }
  public static Object display(Object x, Writer out) throws IOException {
    if (out == null)
      out = currentOutputPort;

    displayObject(x, out);
    out.flush();
    return x;
  }

  static { Subr.def("IO", "writeChar", "write-char", 1, 1); }
  public static Character writeChar(Character x, Writer out)
                          throws IOException {
    (out == null ? currentOutputPort : out).write(x.charValue());
    return x;
  }

  static { Subr.def("IO", "newline", 0, 1); }
  public static Boolean newline(Writer out) throws IOException {
    (out == null ? currentOutputPort : out).write('\n');
    return T;
  }

  static { Subr.def("IO", "eofp", "eof-object?", 1); }
  public static Boolean eofp(Object obj) {
    return obj == eofObject ? T: F;
  }

  static { Subr.def("IO", "inputp", "input-port?", 1); }
  public static Boolean inputp(Object obj) {
    return obj instanceof PushbackReader ? T: F;
  }

  static { Subr.def("IO", "outputp", "output-port?", 1); }
  public static Boolean outputp(Object obj) {
    return obj instanceof Writer ? T: F;
  }

  static { Subr.def("IO", "currentInputPort", "current-input-port", 0); }
  public static PushbackReader currentInputPort() {
    return currentInputPort;
  }

  static { Subr.def("IO", "currentOutputPort", "current-output-port", 0); }
  public static Writer currentOutputPort() {
    return currentOutputPort;
  }

  static {
    Subr.def("IO", "consoleInputPort", "console-input-port", 0, 1);
  }
  public static PushbackReader consoleInputPort(String encoding)
                               throws UnsupportedEncodingException {
    if (encoding == null)
      return consoleInput;
    else
      return new PushbackReader(new InputStreamReader(System.in, encoding));
  }

  static {
    Subr.def("IO", "consoleOutputPort", "console-output-port", 0, 1);
  }
  public static Writer consoleOutputPort(String encoding)
                       throws UnsupportedEncodingException {
    if (encoding == null)
      return consoleOutput;
    else
      return new OutputStreamWriter(System.out, encoding);
  }

  static { Subr.def("IO", "closeInputPort", "close-input-port", 1); }
  public static Boolean closeInputPort(PushbackReader in)
                        throws IOException {
    in.close();
    return T;
  }

  static { Subr.def("IO", "closeOutputPort", "close-output-port", 1); }
  public static Boolean closeOutputPort(Writer out) throws IOException {
    out.close();
    return T;
  }

  static { Subr.def("IO", "flushPort", "flush-port", 0, 1); }
  public static Boolean flushPort(Writer out) throws IOException {
    (out == null ? currentOutputPort : out).flush();
    return T;
  }

  static { Subr.def("IO", "openInputString", "open-input-string", 1); }
  public static PushbackReader openInputString(String s) {
    return new PushbackReader(new StringReader(s));
  }

  static { Subr.def("IO", "openOutputString", "open-output-string", 0); }
  public static StringWriter openOutputString() {
    return new StringWriter();
  }

  static { Subr.def("IO", "getOutputString", "get-output-string", 1); }
  public static String getOutputString(StringWriter sw) {
    return sw.toString();
  }

  static { Subr.def("IO", "resetOutputString", "reset-output-string", 1); }
  public static StringWriter resetOutputString(StringWriter sw) {
    sw.getBuffer().setLength(0);
    return sw;
  }

  static { Subr.def("IO", "fileExists", "file-exists?", 1); }
  public static Boolean fileExists(String s) {
    return (new File(s)).exists() ? T : F;
  }

  static { Subr.def("IO", "openInputFile", "open-input-file", 1, 1); }
  public static PushbackReader openInputFile(String name, String encoding)
                               throws FileNotFoundException,
                                      UnsupportedEncodingException {
    FileInputStream fis = new FileInputStream(name);
    return new PushbackReader (encoding == null
                                ? new InputStreamReader(fis)
                                : new InputStreamReader(fis, encoding));
  }

  static { Subr.def("IO", "openOutputFile", "open-output-file", 1, 1); }
  public static Writer openOutputFile(String name, String encoding)
                       throws IOException,
                              UnsupportedEncodingException {
    FileOutputStream fos = new FileOutputStream(name);
    return encoding == null
           ? new OutputStreamWriter(fos)
           : new OutputStreamWriter(fos, encoding);
  }

  static {
    Subr.def("IO", "callWithInputFile", "call-with-input-file", 2, 1);
  }
  public static Object callWithInputFile(String s, Object arg, Object opt)
                       throws IOException {
    PushbackReader in
    = openInputFile(s, (opt == null ? null : (String) arg));
    try {
      return ((Function) (opt == null ? arg : opt)).invoke1(in);
    } finally {
      in.close();
    }
  }

  static {
    Subr.def("IO","callWithOutputFile","call-with-output-file", 2, 1);
  }
  public static Object callWithOutputFile(String s, Object arg, Object opt)
                       throws IOException {
    Writer out = openOutputFile(s, (opt == null ? null : (String) arg));
    try {
      return ((Function) (opt == null ? arg : opt)).invoke1(out);
    } finally {
      out.close();
    }
  }

  static { Subr.def("IO", "withInputFromPort", "with-input-from-port", 2); }
  public static Object withInputFromPort(PushbackReader in, Function f) {
    PushbackReader prev = currentInputPort;
    currentInputPort = in;
    try {
      return f.invoke(List.nil);
    } finally {
      currentInputPort = prev;
    }
  }

  static { Subr.def("IO", "withOutputToPort", "with-output-to-port", 2); }
  public static Object withOutputToPort(Writer out, Function f) {
    Writer prev = currentOutputPort;
    currentOutputPort = out;
    try {
      return f.invoke(List.nil);
    } finally {
      currentOutputPort = prev;
    }
  }

  static { Subr.def("IO", "load", 1, 2); }
  public static String load(String s, Object opt1, Object opt2)
                       throws Throwable {
    String encoding = null;
    boolean verbose = false;
    if (opt1 != null)
      if (opt1 instanceof String) {
        encoding = (String) opt1;
        if (opt2 != null)
          verbose = ((Boolean) opt2).booleanValue();
      } else {
        verbose = ((Boolean) opt1).booleanValue();
        if (opt2 != null)
          encoding = (String) opt2;
      }
    PushbackReader in = openInputFile(s, encoding);
    try {
      Object expr;
      while ((expr = read(in)) != eofObject) {
        Object val = Eval.topLevelEval(expr);
        if (verbose)
          println(val);
      }
    } finally {
      in.close();
    }
    return s;
  }

  static boolean needsEscape(String s) {
    int len = s.length();
    if (len == 0) return true;
    if (cat(s.charAt(0)) != CAconstituent) return true;
    for (int i = 1; i < len; i++) {
      int a = cat(s.charAt(i));
      if (a != CAconstituent && a != CAnonTerminating)
        return true;
    }
    if (s.equals(".")) return true;
    try {
      readNumber(s, 10);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  static void print(String s) {
    try {
      currentOutputPort.write(s);
      currentOutputPort.flush();
    } catch (IOException e) {}
  }

  static void println(String s) {
    try {
      currentOutputPort.write(s);
      newline(null);
      currentOutputPort.flush();
    } catch (IOException e) {}
  }

  static void println(Object x) {
    try {
      writeObject(x, currentOutputPort);
      newline(null);
      currentOutputPort.flush();
    } catch (IOException e) {}
  }

  static String printString(Object x) {
    StringWriter s = new StringWriter();
    try {
      writeObject(x, s);
    } catch (IOException e) {}
    return s.toString();
  }

  static void init() {}
}
