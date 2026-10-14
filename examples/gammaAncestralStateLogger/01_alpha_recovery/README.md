# Example 1 — Recover the Gamma shape from simulated alignments

A sanity check on BEAST 2's `SequenceSimulator`: generate 10 independent
1000-nucleotide alignments on the same 10-taxon tree under JC +
discrete-Gamma(6 categories, alpha = 0.5), then estimate alpha with
IQ-TREE on each alignment with the true tree held fixed and the true
model class (JC+G).

If the simulator and recovery pipeline are correct, the ten estimates
should bracket 0.5 with no significant bias.

**Note.** This example does not use `AncestralStateLogger` and does not
require the Gamma patch applied in this PR. It verifies the generative
side of the pipeline, so Example 2 can rely on it.

## Files

| file | role |
|---|---|
| `simulate.xml` | SequenceSimulator XML: JC + Gamma(6, alpha = 0.5), 1000 nt |
| `run.sh` | runs 10 draws, calls IQ-TREE on each, prints the summary |
| `../tree.nwk` | the shared 10-taxon tree (total length 2.0 subs/site) |

## Run

```bash
cd examples/gammaAncestralStateLogger/01_alpha_recovery
bash run.sh
```

Needs: BEAST 2.7.x, IQ-TREE (`iqtree3` on `$PATH`), Python 3.
Total runtime about 20 seconds on a laptop.

## Expected output

```
=== draw 01 === alpha_hat = 0.4901
...
=== summary ===
true alpha            : 0.5
n replicates          : 10
mean alpha_hat        : 0.5148
sd alpha_hat          : 0.0383
min, max alpha_hat    : 0.4416, 0.5602
mean bias (hat-true)  : +0.0148
```

(Values from one representative run. Different IQ-TREE builds may give
slightly different numbers, but the mean should sit within one standard
error of 0.5.)
