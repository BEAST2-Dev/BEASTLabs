#!/bin/bash
# Alpha recovery workflow:
#   1. 10 draws of a 1000-nt alignment from a fixed 10-taxon tree under
#      JC + Gamma(6 categories, alpha = 0.5), using SequenceSimulator.
#   2. For each draw, run IQ-TREE with the true tree fixed and the true
#      model class (JC+G) to estimate alpha.
#   3. Print the 10 alpha estimates alongside the true value.
#
# Requires: BEAST 2.7.x (binary `beast`), IQ-TREE (binary `iqtree3`),
# Python 3 for the BEAST->FASTA conversion. Expected total runtime < 30 s.

set -e
BEAST="${BEAST:-beast}"
IQTREE="${IQTREE:-iqtree3}"

for i in $(seq -w 1 10); do
    echo "=== draw $i ==="
    rm -f sim.out.xml
    $BEAST -overwrite -seed ${i##0} simulate.xml > /dev/null 2>&1
    mv sim.out.xml draw_${i}.out.xml
    # BEAST writes <sequence taxon='X' value='ACGT...'/> blocks; convert to FASTA
    python3 -c "
import re
with open('draw_${i}.out.xml') as f: s = f.read()
seqs = re.findall(r\"taxon='([^']+)'\s+value='([^']+)'\", s)
with open('draw_${i}.fasta','w') as f:
    for t,v in seqs: f.write(f'>{t}\n{v}\n')
"
    rm -f draw_${i}.fasta.*
    $IQTREE -s draw_${i}.fasta -te ../tree.nwk -m JC+G -redo -quiet > /dev/null 2>&1
    alpha=$(grep -oE 'Gamma shape alpha: [0-9.]+' draw_${i}.fasta.iqtree | head -1 | awk '{print $NF}')
    echo "  alpha_hat = $alpha"
done

echo ""
echo "=== summary ==="
python3 - <<'PY'
import re, glob, statistics
alphas = []
for f in sorted(glob.glob("draw_*.fasta.iqtree")):
    alphas.append(float(re.search(r'Gamma shape alpha:\s*([0-9.]+)', open(f).read()).group(1)))
print(f"true alpha            : 0.5")
print(f"n replicates          : {len(alphas)}")
print(f"mean alpha_hat        : {statistics.mean(alphas):.4f}")
print(f"sd alpha_hat          : {statistics.stdev(alphas):.4f}")
print(f"min, max alpha_hat    : {min(alphas):.4f}, {max(alphas):.4f}")
print(f"mean bias (hat-true)  : {statistics.mean(alphas)-0.5:+.4f}")
PY
