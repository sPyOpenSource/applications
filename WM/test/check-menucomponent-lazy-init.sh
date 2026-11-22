#!/bin/sh
# Regression check for the JNode boot crash in java.awt.MenuComponent.
#
# JNode's VM initializes java.awt.MenuComponent from core:start_initial_thread
# during boot, before the AWT toolkit is available.  An eager static field
# initializer such as
#
#   private static transient Toolkit toolkit = Toolkit.getDefaultToolkit();
#
# therefore throws an uncaught NullPointerException and kills the initial
# thread.  Toolkit resolution must happen on first use instead.
set -eu

src="$(dirname "$0")/../src/java/awt/MenuComponent.java"

if grep -qE 'static[^;]*Toolkit[[:space:]]+toolkit[[:space:]]*=[[:space:]]*Toolkit\.getDefaultToolkit' "$src"; then
  echo "FAIL: $src eagerly resolves the AWT Toolkit in a static initializer" >&2
  exit 1
fi

if ! grep -qE 'final[[:space:]]+Toolkit' "$src" || \
   ! grep -qE 'toolkit[[:space:]]*=[[:space:]]*Toolkit\.getDefaultToolkit\(\)' "$src"; then
  echo "FAIL: $src no longer lazily resolves the toolkit in getToolkit()" >&2
  exit 1
fi

echo "PASS: MenuComponent resolves the AWT Toolkit lazily"
