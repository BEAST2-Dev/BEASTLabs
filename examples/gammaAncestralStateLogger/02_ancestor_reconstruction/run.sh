#!/bin/bash
# Ancestor-reconstruction workflow:
#   1. Simulate leaves at 10 taxa under JC + Gamma(6, alpha = 0.5).
#   2. Inject the simulated sequences into reconstruct.template.xml.
#   3. Run reconstruct.xml: the Gamma-aware AncestralStateLogger samples
#      the ancestor at three internal nodes (shallow / mid / root), 5
#      independent stochastic draws per node, under the same JC+Gamma model.
#   4. Report per-site diversity across the 5 draws at each node — the
#      expected pattern is shallow < mid < root.
#
# Requires: BEAST 2.7.x (binary `beast`) with the patched BEASTLabs
# installed, Python 3. Expected total runtime < 10 s.

set -e
BEAST="${BEAST:-beast}"

# 1. Simulate leaves (seed = 1 for reproducibility)
echo "=== simulating leaves ==="
rm -f sim.out.xml
$BEAST -overwrite -seed 1 simulate.xml > /dev/null 2>&1
test -f sim.out.xml || { echo "ERROR: simulate.xml did not produce sim.out.xml"; exit 1; }

# 2. Inject simulated sequences into reconstruct.template.xml
python3 - <<'PY'
import re
with open("sim.out.xml") as f:
    sim = f.read()
# SequenceSimulator writes <sequence ... taxon='X' value='ACGT...'/>
seqs = dict(re.findall(r"taxon='([^']+)'\s+value='([^']+)'", sim))
assert len(seqs) == 10, f"expected 10 sequences, got {len(seqs)}"
block = "\n".join(
    f'    <sequence spec="beast.base.evolution.alignment.Sequence" taxon="{t}" value="{seqs[t]}"/>'
    for t in sorted(seqs, key=lambda s: int(s[1:]))
)
with open("reconstruct.template.xml") as f:
    tpl = f.read()
with open("reconstruct.xml","w") as f:
    f.write(tpl.replace("__PLACEHOLDER_LEAVES__", block))
print("injected 10 leaf sequences into reconstruct.xml")
PY

# 3. Run the reconstruction
echo ""
echo "=== reconstructing ancestors with JC+Gamma(6) ==="
rm -f reconstruct.xml.state reconstruct.shallow.log reconstruct.mid.log reconstruct.root.log
$BEAST -overwrite -seed 1 reconstruct.xml > /dev/null 2>&1

# 4. Diversity summary
echo ""
echo "=== per-site diversity across 5 draws ==="
python3 - <<'PY'
def diversity(path):
    with open(path) as f:
        f.readline()
        rows = [l.strip().split() for l in f if l.strip()]
    nsites = len(rows[0]) - 1
    nvar = sum(1 for i in range(1, nsites+1) if len({r[i] for r in rows}) > 1)
    return nsites, nvar, len(rows)

for name, path in [("shallow (t5,t2)", "reconstruct.shallow.log"),
                   ("mid     (t5,t2,t3,t6)", "reconstruct.mid.log"),
                   ("root    (all 10 taxa)", "reconstruct.root.log")]:
    sites, var, draws = diversity(path)
    pct = 100 * var / sites
    print(f"  {name:25s} draws={draws}  variable sites = {var}/{sites} ({pct:.1f}%)")
PY

echo ""
echo "Done. See reconstruct.{shallow,mid,root}.log for the 5 ancestral-sequence"
echo "draws at each node (one row per MCMC state, integer-encoded 0=A 1=C 2=G 3=T)."
