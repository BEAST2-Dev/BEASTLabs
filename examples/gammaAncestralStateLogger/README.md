# Gamma-aware AncestralStateLogger — worked examples

Two self-contained examples that exercise the discrete-Gamma support
added to `beastlabs.evolution.likelihood.AncestralStateLogger` in this
PR.

## Shared setup

`tree.nwk` — a 10-taxon tree generated with `ape::rtree(10)` under
`set.seed(42)` and rescaled to total length 2.0 subs/site. Both examples
simulate on this tree and both reconstruct against it.

The simulation model is JC + discrete-Gamma with 6 categories and shape
alpha = 0.5. JC is used (rather than GTR) so the examples are small and
unambiguous; the Gamma handling is where this PR's change lives.

## Example 1 — Alpha recovery

`01_alpha_recovery/` — simulates 10 independent 1000-nt alignments and
estimates alpha with IQ-TREE on each, with the true tree held fixed and
the true model class (JC+G) fixed. Checks that the simulation side of
the pipeline is self-consistent: the mean of the ten alpha estimates
should sit within one standard error of 0.5.

This example does **not** touch `AncestralStateLogger` and does not
require the PR's patch. It serves as a prerequisite for Example 2.

Requires: BEAST 2.7.x, IQ-TREE 3.x on `$PATH`, Python 3.

## Example 2 — Ancestor reconstruction

`02_ancestor_reconstruction/` — simulates one 1000-nt alignment on the
tree, then uses `AncestralStateLogger` to sample ancestral sequences at
three internal nodes under the **same** JC + Gamma(6, alpha = 0.5)
model — 5 stochastic draws per node. The reconstruction XML sets
`gammaCategoryCount="6"`; before this PR, that would throw at the first
logger call.

Reports per-node per-site diversity across the 5 draws, which reflects
the posterior ancestral-state uncertainty — larger when fewer leaves
inform the ancestor and when more sites fall into fast-rate categories.

Requires: BEAST 2.7.x **with this PR's patch**, Python 3.

## Running

Each subfolder has its own `run.sh` and `README.md` with the exact
commands and expected output. If the `beast` or `iqtree3` binaries are
not on `$PATH`, set `BEAST=/full/path/to/beast` or
`IQTREE=/full/path/to/iqtree3` in the environment.
