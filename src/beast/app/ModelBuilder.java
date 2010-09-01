/*
* File ModelBuilder.java
*
* Copyright (C) 2010 Remco Bouckaert remco@cs.auckland.ac.nz
*
* This file is part of BEAST2.
* See the NOTICE file distributed with this work for additional
* information regarding copyright ownership and licensing.
*
* BEAST is free software; you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
*  BEAST is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with BEAST; if not, write to the
* Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
* Boston, MA  02110-1301  USA
*/
package beast.app;


import beast.util.Randomizer;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.*;


/**
 * Program for drawing BEAST 2.0 models.
 * This is a bit of a clutch... but potentially useful.
 * <p/>
 * *
 */

public class ModelBuilder extends JPanel {
    /** for serialisation */
    static final long serialVersionUID = 1L;

    public static void main(String args[]) {
    	Randomizer.setSeed(127);
        JFrame f = new JFrame("Model Builder");
        beast.app.draw.ModelBuilder drawTest = new beast.app.draw.ModelBuilder();
        drawTest.init();
        JMenuBar menuBar = drawTest.makeMenuBar();
        f.setJMenuBar(menuBar);

        f.add(drawTest.m_jTbTools, BorderLayout.NORTH);
        f.add(drawTest, BorderLayout.CENTER);

        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        java.net.URL tempURL = ClassLoader
                .getSystemResource(beast.app.draw.ModelBuilder.ICONPATH + "/GenerationD.png");
        try {
            f.setIconImage(ImageIO.read(tempURL));
        } catch (Exception e) {
            e.printStackTrace();
            // ignore
        }
        if (args.length > 0) {
            drawTest.m_doc.loadFile(args[0]);
            drawTest.setDrawingFlag();
        }
        f.setSize(600, 800);
        f.setVisible(true);
    } // main
}
