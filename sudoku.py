# The grid is represented as an array of length 81.

# 00 01 02 | 03 04 05 | 06 07 08
# 09 10 11 | 12 13 14 | 15 16 17 
# 18 19 20 | 21 22 23 | 24 25 26
# ------------------------------
# 27 28 29 | 30 31 32 | 33 34 35
# 36 37 38 | 39 40 41 | 42 43 44
# 45 46 47 | 48 49 50 | 51 52 53
# ------------------------------
# 54 55 56 | 57 58 59 | 60 61 62
# 63 64 65 | 66 67 68 | 69 70 71
# 72 73 74 | 75 76 77 | 78 79 80

# Offsets into this grid, or the related markup structure (see below), are
# termed squares. In a grid squares either have a fixed value (1 to 9) or
# are empty (have value 0). In a markup, a square has one or more possible
# values. Having more than one value indicates that the value of the square
# has to be one of the listed values.

# Offsets into the grid of each of the 9 row, columns and boxes.

rows = tuple(range(i, i + 9) for i in range(0, 81, 9))
cols = tuple(range(i, 81, 9) for i in range(9))

boxes = tuple((i, i + 1, i + 2, i + 9, i + 10, i + 11, i + 18, i + 19, i + 20)
    for i in (0, 3, 6, 27, 30, 33, 54, 57, 60))

# Provide a mapping from grid index to a row, column, box tuple which
# contain the indexed square.

units = tuple((
    rows[i // 9],
    cols[i % 9],
    boxes[3 * (i // 27) + (i % 9) // 3]
) for i in range(81))

# Area of Effect. The neighborhood of a square at given grid index. This is
# the set of all squares in the same row, column or box as the index into
# aoe but doesn't include the square itself.

aoe = tuple((set(x for y in units[i] for x in y) - set([i])) for i in range(81))

fullset = set('123456789')

def display(m):
    for i, v in enumerate(m):
        print(v, end='')
        if (i + 1) % 9 == 0: print()
    print()

# Take a grid an replace all empty squares with the possible values it could
# have so as not to contradict existing squares with defined values. The
# value returned is conceptually an array of sets of possible values for
# each square in the grid, but the sets are represented by strings.

# Note that the solve function and its helper functions deal in terms of this
# 'markup' format rather than the original grid.

def markup(grid):
    return [''.join(fullset - set([grid[j] for j in aoe[i]]))
        if grid[i] == '0' else grid[i] for i in range(81)]

# Check to see if the grid is consistent in the sense that there are no
# duplicate values in each row, column and box making up the grid.

def quick_check(grid):
    for rcb in (rows, cols, boxes):
        for s in rcb:
            t = [grid[i] for i in s if grid[i] != '0']
            if len(t) != len(set(t)):
                return False

    return True
 
# This is the main part of the solver. It takes a markup, a square and a
# a value which has been assigned to that square. It then updates the
# markup to remove that value as a possible value for the square.

def eliminate(m, i, v):
    # If removing a possible value would leave no other possibilities then
    # the markup represents an impossible solution.

    if v not in m[i]: return m
    w = m[i] = m[i].replace(v, '')
    if not w: return None

    # If removing a value means that a square now has only a single possible
    # value, then that value can't appear as a possible value for any other
    # square in the original square's neighborhood.

    if len(w) == 1:
        if not all(eliminate(m, j, w) for j in aoe[i]):
            return None

    # Check that the eliminated value  is still a possibility for at least
    # one other square in the original square's row, column and box.

    for unit in units[i]:   # row, column and box
        places = [j for j in unit if v in m[j]]
        if not places: return None

        # If the eliminated value can now only appear in a single square,
        # then impose that new constraint.

        if len(places) == 1:
            if not assign(m, places[0], v):
                return None

    return m

# Create a new markup from an existing one removing all other values from a
# square other than a specified value. This imposes new constraints on the
# solution, that none of the other possible values that the square could have
# had can occur at that square. This has implications for possible values that
# other squares sharing a row, column or box can take. The assignment is only
# possible if these additional constraints are possible.

def assign(m, i, v):
    if all(eliminate(m, i, w) for w in m[i] if w != v):
        return m

    return None

def solved(m):
    return all(len(s) == 1 for s in m)

# The main solver takes as input a markup, a "marked up" grid, that is a the
# output of passing a a grid to the markup function.

def solve(m):
    if not m: return None
    if solved(m): return m

    # Determine a square (grid index) which has the minimum number of
    # possible values without being locked down to a single value. There
    # will likely be many squares that could be chosen here.

    #_, min_i = min((len(s), i) for i, s in enumerate(m) if len(s) > 1)

    min_len = 9
    for i, s in enumerate(m):
        if 1 < len(s) < min_len:
            min_i = i
            if len(s) == 2:
                break
            min_len = len(s)

    # Go through the possible values of the chosen square, creating a grid
    # with this value assigned to the square and attempting to solve the
    # new grid.

    for v in m[min_i]:
        n = solve(assign(m[:], min_i, v))
        if n: return n

    # If no solution is possible given the starting position, then backtrack.

    return None

