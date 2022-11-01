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

final class sample {

  public static void main(String argv[]) {
  // A sample code to invoke the embedded system

    Eval.initializeSystem();
    // Initialize the system by loading all implementation classes.

    Eval.loadProgram("sample.lsp");
    // Load your program into the system.

    Object value = Eval.runProgram("(foo 1 2)");
    // Run the program by giving an S-expression to evaluate.
    // You may run your program as many times as you want.

    IO.println(value);
    // Print the result if you want.
  }

}
