# Example 2 — Ancestor reconstruction under JC + Gamma

End-to-end demonstration of the Gamma-aware AncestralStateLogger that
this PR adds. On a fixed 10-taxon tree:

1. **Simulate** leaf sequences under JC + Gamma(6, alpha = 0.5), 1000 nt.
2. **Reconstruct** ancestors at three internal nodes using
   `AncestralStateLogger` under the **same** JC + Gamma(6, alpha = 0.5)
   model — 5 independent stochastic draws per node.

Before this PR, step 2 would die with
`RuntimeException: "Gamma rate heterogeneity or proportion invariant is
not supported yet"` on its first call because
`siteModel.getCategoryCount() > 1`. After this PR it just runs.

## Files

| file | role |
|---|---|
| `simulate.xml` | SequenceSimulator, JC + Gamma(6, alpha = 0.5), 1000 nt |
| `reconstruct.template.xml` | template XML with a `__PLACEHOLDER_LEAVES__` marker |
| `run.sh` | runs the simulation, injects leaves into the template, runs the reconstruction, reports per-node stochastic diversity |
| `../tree.nwk` | the shared 10-taxon tree |

The three loggers target:
- a 2-leaf clade `(t5, t2)` — small taxonset, deep-ish split
- a 4-leaf clade `(t5, t2, t3, t6)` — larger taxonset, well constrained
- the root (all 10 taxa)

## Run

```bash
cd examples/gammaAncestralStateLogger/02_ancestor_reconstruction
bash run.sh
```

Needs: BEAST 2.7.x with the Gamma-patched BEASTLabs, Python 3.
Expected total runtime about 5 seconds.

## Expected output

```
=== simulating leaves ===
=== reconstructing ancestors with JC+Gamma(6) ===
=== per-site diversity across 5 draws ===
  shallow (t5,t2)           draws=5  variable sites = 102/1000 (10.2%)
  mid     (t5,t2,t3,t6)     draws=5  variable sites =   9/1000  (0.9%)
  root    (all 10 taxa)     draws=5  variable sites = 242/1000 (24.2%)
```

(Representative numbers from one run with `-seed 1`.)

The root carries the most posterior uncertainty, as expected — it is
furthest from every leaf under JC + Gamma. The `(t5, t2)` cherry carries
noticeable per-site uncertainty too: the two taxa are 0.21 subs/site
apart along their path, and with `alpha = 0.5` a non-trivial fraction of
sites fall into fast-rate categories where the MRCA state is weakly
pinned. The `(t5, t2, t3, t6)` clade is paradoxically **more**
constrained than the cherry despite being deeper, because four leaves
inform the ancestor instead of two — a nice reminder that "shallow" in
tree position does not mean "well pinned".

## What the output files look like

`reconstruct.{shallow,mid,root}.log` — one row per MCMC state logged
(5 draws at states 0, 100000, 200000, 300000, 400000), columns are the
ancestral nucleotides at each site, integer-encoded `0=A, 1=C, 2=G,
3=T`. Posterior probabilities per site are recovered by taking column
frequencies across many draws (increase `chainLength` and keep
`logEvery` to get more draws).
