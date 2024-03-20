// Java version of a sudoku solver which I originally wrote in Python. The
// algorthm is recursive backtracking with constraint propagation. It was
// originally developed as a solution to problem 96 of Project Euler but was
// later greatly improved and heavily influenced by ideas I read about in an
// article by Peter Norvig.

// The grid is represented as a vector of (9 x 9) elements. Each element holds
// an integer bitmask (termed a markup) indicating which values that element
// must be selected from. The presence of bit (1 << n) indicates that the
// element still has the possibility of taking value n as its final value.

// The use of constraint propagation dramatically improves the performance of
// a more naive backtracking algorithm by propagating the consequences of any
// choice of value of an element to all related elements. Choosing to set the
// value of an element to a specific value means that no other square in the
// row, column or box of which that element is a member can take that value.

// The interesting part is that constraints imposed by assigning a value to
// an element can then recursively impact other distant elements when it is
// found that setting a value to the element constrains one or more other
// nearby elements (same row, column or box) to also take on a definite value.
// This is reflected in the mutually recursive calls between the 'assign' and
// the 'eliminate' functions.

// Another side effect of constraint propagation is that each step change in
// the problem state can be huge which means it is easier and more efficient
// to create a new copy of the current state at each step rather than try to
// undo the changes made. This also means that the efficient representation
// of state is important and led to the idea of representing markups as
// bitmasks rather than more obvious set types.

// When working through this code it is sometimes useful to be able to
// visualize the layout of the grid.

// 00 01 02 | 03 04 05 | 06 07 08
// 09 10 11 | 12 13 14 | 15 16 17 
// 18 19 20 | 21 22 23 | 24 25 26
// ------------------------------
// 27 28 29 | 30 31 32 | 33 34 35
// 36 37 38 | 39 40 41 | 42 43 44
// 45 46 47 | 48 49 50 | 51 52 53
// ------------------------------
// 54 55 56 | 57 58 59 | 60 61 62
// 63 64 65 | 66 67 68 | 69 70 71
// 72 73 74 | 75 76 77 | 78 79 80

// The other concept where a diagram may help is the bitmask used in the markup
// array. These bitmasks are essentially acting as sets indicating the values
// which are still open to a grid square. A solution will comprise a markup in
// which all elements are bitmasks with only a single bit set.

// +---+---+---+---+---+---+---+---+---+
// | 9 | 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 |
// +---+---+---+---+---+---+---+---+---+
//   0   0   0   0   0   1   0   1   0

// For example a markup element value of 10 (binary 1010) would indicate that
// the corresponding grid square is constrained to take on either the value
// 4 or 2.


public class Sudoku {
    private static int[][] rows = new int[9][9];
    private static int[][] cols = new int[9][9];
    private static int[][] boxes = new int[9][9];
    private static int[][] aoe = new int[81][20];
    private static int[][][] units = new int[81][3][];
    private static int[] bitcounts = new int[1 << 9];

    static {
        int[] starts = {0, 3, 6, 27, 30, 33, 54, 57, 60};
        int[] offsets = {0, 1, 2, 9, 10, 11, 18, 19, 20};

        for (int i = 0; i < 9; i++)
            for (int j = 0; j < 9; j++) {
                int v = 9 * i + j;
                rows[i][j] = v;
                cols[j][i] = v;
                boxes[i][j] = starts[i] + offsets[j];
            }

        for (int i = 0; i < 81; i++) {
            units[i][0] = rows[i / 9];
            units[i][1] = cols[i % 9];
            units[i][2] = boxes[3 * (i / 27) + (i % 9) / 3];
        }

        for (int i = 0; i < 81; i++) {
            int n = 0;
            for (int j = 0; j < 2; j++)
                for (int s : units[i][j])
                    if (s != i)
                        aoe[i][n++] = s;

            for (int s : units[i][2])
                if (s != i) {
                    Boolean notdup = true;
                    for (int j = 0; j < n; j++)
                        if (aoe[i][j] == s) {
                            notdup = false;
                            break;
                        }
                    if (notdup)
                        aoe[i][n++] = s;
                }
        }

        for (int i = 1; i < bitcounts.length; i++) {
            int n = i, count = 0;

            do {
                int b = n & (-n);   // Extract rightmost set bit of n
                count += 1;
                n -= b;
            } while (n > 0);
            
            bitcounts[i] = count;
        }
    }

// Take a grid and replace all empty squares with the possible values it could
// have so as not to contradict existing squares with defined values. The
// value returned is conceptually an array of sets (represented by bitmasks)
// of possible values for each square in the grid.

// Note that the solve function and its helper functions deal in terms of this
// 'markup' format rather than the original grid.

    public static int[] markup(String grid) {
        int[] m = new int[grid.length()];
        int[] v = new int[grid.length()];

        for (int i = 0; i < v.length; i++)
            v[i] = grid.charAt(i) - '0';

        for (int i = 0; i < grid.length(); i++) {
            int d = v[i];

            if (d == 0) {
                int mask = 0x01FF;
                for (int j : aoe[i])
                    if (v[j] != 0) {
                        int b = 1 << (v[j] - 1);
                        if ((b & mask) != 0)
                            mask -= b;
                    }
                m[i] = mask;
            }   
            else
                m[i] = 1 << (d - 1);
        }

        return m;
    }

// This is essentially the inverse of markup. It takes a solved markup array,
// that is a markup array where each element is a bitmak with only one set
// bit, and returns a string comprising the ordered digits of the solution.

    public static String flatten(int[] m) {
        char[] digits = new char[m.length];

        for (int i = 0; i < m.length; i++) {
            int b = m[i], d = 1;

            while ((b & 1) == 0) {
                d += 1;
                b >>= 1;
            }

            digits[i] = (char) (d + '0');
        }

        return new String(digits);
    }

// This is the main part of the solver. It takes a markup, a square and a
// a bitmask with a single bit set which corresponds to the value that is
// to be removed from the markup of that square. If the specified value
// does not exist in the square's markup then that is not a problem and in
// fact is the ideal case because nothing else needs doing. If the value
// does exist then it needs removing and all other squares recursively
// updated to reflect this.

    private static Boolean eliminate(int[] m, int i, int b) {
        // If the value to eliminate is not a possible value in the markup
        // of the specified square then nothing needs doing.

        if ((b & m[i]) == 0)
            return true;

        // If removing a possible value would leave no other possibilities
        // then the markup represents an impossible solution.

        int w = m[i] - b;

        if (w == 0)
            return false;

        m[i] = w;

        // If removing a value means that a square now has only a single
        // possible value, then that value can't appear as a possible value
        // for any other square in the original square's neighborhood.

        if (bitcounts[w] == 1) {
            for (int j : aoe[i])
                if (!eliminate(m, j, w))
                    return false;
        }

        // Check that the eliminated value is still a possibility for at
        // least one other square in each of the original square's row,
        // column and box.

    rcb:
        for (int[] rcb : units[i]) {
            int count = 0, position = 0;

            for (int s : rcb)
                if ((b & m[s]) != 0) {
                    count += 1;
                    if (count > 1)
                        continue rcb;
                    position = s;
                }

            if (count == 1)
                if (assign(m, position, b) == null)
                    return false;
        }
                
        return true;
    }

// Modify a markup by removing all other values from a square other than the
// specified one. This imposes new constraints on the solution; none of the
// other possible values that the square could have had can now occur at that
// square. This in turn affects the values that other squares sharing a row,
// column or box can take. The assignment is only possible if these additional
// constraints are possible.

    private static int[] assign(int[] m, int i, int b) {
        int n = m[i];

        do {
            int w = n & (-n);   // Extract rightmost set bit of n

            if (w != b) 
                if (!eliminate(m, i, w))
                    return null;
            n -= w;
        } while (n != 0);
        
        return m;
    }

    private static Boolean solved(int[] m) {
        for (int s : m)
            if (bitcounts[s] != 1)
                return false;

        return true;
    }

// The main solver takes as input a markup, that is a "marked up" grid which
// is the output of passing a grid to the markup function. It is essentially
// a vector of bitmasks indicating what values are still possible for a
// given grid square to take.

    public static int[] solve(int[] m) {
        if (m == null) return null;
        if (solved(m)) return m;

        // Determine a square (grid index) which has the minimum number of
        // possible values without being locked down to a single value. You
        // could ignore this step and simply choose any square which still
        // has multiple potential values, but spending a little effort in
        // choosing an appropriate square will have the effect of decreasing
        // the search space and improving the overall performance of the
        // algorithm. There will likely be many squares that could be chosen.

        int min_len = 10, min_i = 0;

        for (int i = 0; i < m.length; i++) {
            int nbits = bitcounts[m[i]];
            if ((nbits > 1) && (nbits < min_len)) {
                min_i = i;
                if (nbits == 2) // Any square with just 2 options is ideal.
                    break;
                min_len = nbits;
            }
        }

        // Go through the possible values of the chosen square, creating a
        // new markup with this value assigned to the square and attempt to
        // solve each of the new resulting markups in turn.

        int b = m[min_i];

        do {
            int v = b & (-b);   // Extract rightmost set bit of b
            int[] n = solve(assign(m.clone(), min_i, v));
            if (n != null)
                return n;
            b -= v;
        } while (b != 0);

        // If no solution is possible given the starting position, then
        // we need to backtrack.

        return null;
    }
}
