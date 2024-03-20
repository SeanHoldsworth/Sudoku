#!/usr/bin/env python3

# Test harness

import time
from sudoku import markup, solve, display

#grids = [line.strip() for line in open('sudoku.txt')]
#grids = [line.strip() for line in open('sudoku17.txt')]
#grids = [line.strip() for line in open('mepham.txt')]
#grids = [line.strip() for line in open('krazydad.txt')]
#grids = [line.strip() for line in open('shortz301.txt')]
#grids = [line.strip() for line in open('snyder.txt')]
grids = [line.strip() for line in open('top95.txt')]
#grids = [line.strip() for line in open('inkala.txt')]
#grids = [line.strip() for line in open('hard.txt')]
#grids = [line.strip() for line in open('easy.txt')]

start = time.perf_counter()
count = 0
for grid in grids:
    m = markup(grid)

    solution = solve(m)

    if not solution:
        print('No solution for grid', count)

    display(solution)
    count += 1

    for i, c in enumerate(grid):
        if grid[i] != '0':
            assert solution[i] == grid[i]

elapsed = time.perf_counter() - start
print('Solved %d problems in %0.3f secs' % (count, elapsed))
