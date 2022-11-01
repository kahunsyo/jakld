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

final class Char {

  private final static Character[] chtab = new Character[128];

  static {
    for (int i = 0; i < 128; i++)
      chtab[i] = new Character((char) i);
  }

  static Character makeChar(char c) {
    if (c >= 0 && c < 128)
      return chtab[c];
    else
      return new Character(c);
  }

  private final static Boolean T = Boolean.TRUE;
  private final static Boolean F = Boolean.FALSE;

  static { Subr.def("Char", "charp", "char?", 1); }
  public static Boolean charp(Object obj) {
    return obj instanceof Character ? T : F;
  }

  static { Subr.def("Char", "charEQ", "char=?", 2); }
  public static Boolean charEQ(char c1, char c2) {
    return c1 == c2 ? T : F;
  }

  static { Subr.def("Char", "charLT", "char<?", 2); }
  public static Boolean charLT(char c1, char c2) {
    return c1 < c2 ? T : F;
  }

  static { Subr.def("Char", "charGT", "char>?", 2); }
  public static Boolean charGT(char c1, char c2) {
    return c1 > c2 ? T : F;
  }

  static { Subr.def("Char", "charLE", "char<=?", 2); }
  public static Boolean charLE(char c1, char c2) {
    return c1 <= c2 ? T : F;
  }

  static { Subr.def("Char", "charGE", "char>=?", 2); }
  public static Boolean charGE(char c1, char c2) {
    return c1 >= c2 ? T : F;
  }

  static { Subr.def("Char", "charEQci", "char-ci=?", 2); }
  public static Boolean charEQci(char c1, char c2) {
    return Character.toLowerCase(c1) == Character.toLowerCase(c2) ? T : F;
  }

  static { Subr.def("Char", "charLTci", "char-ci<?", 2); }
  public static Boolean charLTci(char c1, char c2) {
    return Character.toLowerCase(c1) < Character.toLowerCase(c2) ? T : F;
  }

  static { Subr.def("Char", "charGTci", "char-ci>?", 2); }
  public static Boolean charGTci(char c1, char c2) {
    return Character.toLowerCase(c1) > Character.toLowerCase(c2) ? T : F;
  }

  static { Subr.def("Char", "charLEci", "char-ci<=?", 2); }
  public static Boolean charLEci(char c1, char c2) {
    return Character.toLowerCase(c1) <= Character.toLowerCase(c2) ? T : F;
  }

  static { Subr.def("Char", "charGEci", "char-ci>=?", 2); }
  public static Boolean charGEci(char c1, char c2) {
    return Character.toLowerCase(c1) >= Character.toLowerCase(c2) ? T : F;
  }

  static { Subr.def("Char", "alphabetic", "char-alphabetic?", 1); }
  public static Boolean alphabetic(char c) {
    return Character.isLetter(c) ? T : F;
  }

  static { Subr.def("Char", "numeric", "char-numeric?", 1); }
  public static Boolean numeric(char c) {
    return Character.isDigit(c) ? T : F;
  }

  static { Subr.def("Char", "whitespace", "char-whitespace?", 1); }
  public static Boolean whitespace(char c) {
    return Character.isWhitespace(c) ? T : F;
  }

  static { Subr.def("Char", "upperCase", "char-upper-case?", 1); }
  public static Boolean upperCase(char c) {
    return Character.isUpperCase(c) ? T : F;
  }

  static { Subr.def("Char", "lowerCase", "char-lower-case?", 1); }
  public static Boolean lowerCase(char c) {
    return Character.isLowerCase(c) ? T : F;
  }

  static { Subr.def("Char", "char2integer", "char->integer", 1); }
  public static Integer char2integer(char c) {
    return Num.makeInt((int) c);
  }

  static { Subr.def("Char", "integer2char", "integer->char", 1); }
  public static Character integer2char(int n) {
    return makeChar((char) n);
  }

  static { Subr.def("Char", "upcase", "char-upcase", 1); }
  public static Character upcase(char c) {
    return makeChar(Character.toUpperCase(c));
  }

  static { Subr.def("Char", "downcase", "char-downcase", 1); }
  public static Character downcase(char c) {
    return makeChar(Character.toLowerCase(c));
  }

  static { Subr.def("Char", "stringp", "string?", 1); }
  public static Boolean stringp(Object obj) {
    return obj instanceof String ? T : F;
  }

  static { Subr.def("Char", "makeString", "make-string", 1, 1); }
  public static String makeString(int length, Character fill) {
    if (length == 0)
      return "";
    else {
      char c = (fill == null ? ' ' : fill.charValue());
      char[] v = new char[length];
      for (int i = 0; i < length; i++)
        v[i] = c;
      return new String(v);
    }
  }

  static {
    Subr.def("Char", "list2string", "string", 0, true);
    Subr.def("Char", "list2string", "list->string", 1);
  }
  public static String list2string(List args) {
    if (args == List.nil)
      return "";
    else {
      StringBuffer sb = new StringBuffer();
      do
        sb.append(((Character) args.car).charValue());
      while ((args = (List) args.cdr) != List.nil);
      return sb.toString();
    }
  }

  static { Subr.def("Char", "string2list", "string->list", 1); }
  public static List string2list(String s) {
    List val = List.nil;
    for (int i = s.length() - 1; i >= 0; i--)
      val = new Pair(makeChar(s.charAt(i)), val);
    return val;
  }

  static { Subr.def("Char", "stringLength", "string-length", 1); }
  public static Integer stringLength(String s) {
    return Num.makeInt(s.length());
  }

  static { Subr.def("Char", "stringRef", "string-ref", 2); }
  public static Character stringRef(String s, int index) {
    return makeChar(s.charAt(index));
  }

  static { Subr.def("Char", "stringEQ", "string=?", 2); }
  public static Boolean stringEQ(String s1, String s2) {
    return s1.equals(s2) ? T : F;
  }

  static { Subr.def("Char", "stringEQci", "string-ci=?", 2); }
  public static Boolean stringEQci(String s1, String s2) {
    return s1.equalsIgnoreCase(s2) ? T : F;
  }

  static { Subr.def("Char", "stringLT", "string<?", 2); }
  public static Boolean stringLT(String s1, String s2) {
    return s1.compareTo(s2) < 0 ? T : F;
  }

  static { Subr.def("Char", "stringGT", "string>?", 2); }
  public static Boolean stringGT(String s1, String s2) {
    return s1.compareTo(s2) > 0 ? T : F;
  }

  static { Subr.def("Char", "stringLE", "string<=?", 2); }
  public static Boolean stringLE(String s1, String s2) {
    return s1.compareTo(s2) <= 0 ? T : F;
  }

  static { Subr.def("Char", "stringGE", "string>=?", 2); }
  public static Boolean stringGE(String s1, String s2) {
    return s1.compareTo(s2) >= 0 ? T : F;
  }

  static { Subr.def("Char", "stringLTci", "string-ci<?", 2); }
  public static Boolean stringLTci(String s1, String s2) {
    return s1.toLowerCase().compareTo(s2.toLowerCase()) < 0 ? T : F;
  }

  static { Subr.def("Char", "stringGTci", "string-ci>?", 2); }
  public static Boolean stringGTci(String s1, String s2) {
    return s1.toLowerCase().compareTo(s2.toLowerCase()) > 0 ? T : F;
  }

  static { Subr.def("Char", "stringLEci", "string-ci<=?", 2); }
  public static Boolean stringLEci(String s1, String s2) {
    return s1.toLowerCase().compareTo(s2.toLowerCase()) <= 0 ? T : F;
  }

  static { Subr.def("Char", "stringGEci", "string-ci>=?", 2); }
  public static Boolean stringGEci(String s1, String s2) {
    return s1.toLowerCase().compareTo(s2.toLowerCase()) >= 0 ? T : F;
  }

  static { Subr.def("Char", "stringAppend", "string-append", 0, true); }
  public static String stringAppend(List args) {
    if (args == List.nil)
      return "";

    String s1 = (String) args.car;
    args = (List) args.cdr;
    if (args == List.nil)
      return s1;

    String s2 = (String) args.car;
    args = (List) args.cdr;
    if (args == List.nil)
      return s1+s2;

    StringBuffer sb = new StringBuffer(s1);
    sb.append(s2);
    do
      sb.append((String) args.car);
    while ((args = (List) args.cdr) != List.nil);
    return sb.toString();
  }

  static { Subr.def("Char", "substring", 2, 1); }
  public static String substring(String s, int from, Integer to) {
    if (to == null)
      return s.substring(from);
    else
      return s.substring(from, to.intValue());
  }

  static void init() {}
}
