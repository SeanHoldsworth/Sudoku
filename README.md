# Sudoku Solvers

These are sudoku solvers based around a 
[constraint propagation](https://en.wikipedia.org/wiki/Local_consistency)
based search algorithm as described by
[Peter Norvig](https://norvig.com/sudoku.html).

The initial version was written in Python which I subsequently translated into
Clojure to see how easily it could be rewritten in a purely functional
language. Since Clojure runs on the JVM then next logical step was to write
a version in Java for benchmarking purposes.

The Problems directory contains a number of text files each containing a number
of Sudoku problems in a standard format. Each puzzle is represented by an 81
character string with one puzzle per line. The 81 characters are the squares
of a Sudoku grid read top to bottom, left to right, with the digits 1 to 9
representing the didgits of the puzzle and the digit 0 used to encode an empty
square.

I've included driver code to exercise the Python and Java versions.
