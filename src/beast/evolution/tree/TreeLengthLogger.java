/*
 * Copyright (C) 2013 Tim Vaughan <tgvaughan@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package beast.evolution.tree;

import java.io.PrintStream;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.Loggable;
import beast.core.Input.Validate;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;


/**
 *
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("Logger to report total length of all branches on tree.")
public class TreeLengthLogger extends CalculationNode implements Loggable, Function {
    
    public Input<Tree> treeInput = new Input<Tree>("tree",
            "Tree to report total branch length of.",
            Validate.REQUIRED);

    @Override
    public void initAndValidate() { }
    
    @Override
    public void init(PrintStream out) throws Exception {
        Tree tree = treeInput.get();
        if (getID() == null || getID().matches("\\s*")) {
            out.print(tree.getID() + ".length\t");
        } else {
            out.print(getID() + "\t");
        }
    }

    @Override
    public void log(int nSample, PrintStream out) {
        Tree tree = treeInput.get();
        out.print(getSubTreeLength(tree.getRoot()) + "\t");
    }
    
    /**
     * Calculate total length of branches in sub-tree of node.
     * 
     * @param node
     * @return total branch length
     */
    private double getSubTreeLength(Node node) {
        double length = 0;
        for (Node child : node.getChildren()) {
            length += node.getHeight()-child.getHeight();
            length += getSubTreeLength(child);
        }
        
        return length;
    }

    @Override
    public void close(PrintStream out) {
    }

    @Override
    public int getDimension() {
        return 1;
    }

    @Override
    public double getArrayValue() {
        return getSubTreeLength(treeInput.get().getRoot());
    }

    @Override
    public double getArrayValue(int iDim) {
        return getSubTreeLength(treeInput.get().getRoot());
    }
    
}
