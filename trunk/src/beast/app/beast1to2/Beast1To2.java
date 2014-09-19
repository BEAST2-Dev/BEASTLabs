
/*
 * File Beast1To2.java
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
package beast.app.beast1to2;

import javax.xml.transform.TransformerException;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Conversion of Beast version 1 xml files to Beast 2.0 xml.
 * Usage: Beast1To2 beast1.xml
 * Output is written to stdout, so use
 * Beast1To2 beast1.xml > beast2.xml
 * to get output in beast2.xml.
 *
 *
 * NB: current limitations Only alignments are converted.
 */
public class Beast1To2 {
	final static String BEAST1TO2_XSL_FILE = "beast/app/beast1to2/beast1To2.xsl";

	String m_sXSL;

	public Beast1To2(String [] args) throws Exception {
        String absolute = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        Path xslPath = Paths.get(absolute, BEAST1TO2_XSL_FILE);

        System.out.println((Files.exists(xslPath) ? "Loading xsl file : " : "Cannot find xsl file : ") + xslPath.toString());

		BufferedReader fin = new BufferedReader(new FileReader((args.length < 2 ? xslPath.toString() : args[1])));
		StringBuffer buf = new StringBuffer();
		while (fin.ready()) {
			buf.append(fin.readLine());

		}
		m_sXSL = buf.toString();
	}

	/** applies beast 1 to t2 XSL conversion script (specified in m_sXSL)
	 */
	public static void main(String [] args) throws TransformerException {
		try {
			String sBeast1 = args[0];
            Path beast1xml = Paths.get(sBeast1);

            Beast1To2 b = new Beast1To2(args);
			StringWriter strWriter = new StringWriter();
            BufferedReader xmlInput = Files.newBufferedReader(beast1xml, Charset.defaultCharset());
			javax.xml.transform.Source xmlSource =
	            new javax.xml.transform.stream.StreamSource(xmlInput);
			Reader xslInput =  new StringReader(b.m_sXSL);
		    javax.xml.transform.Source xsltSource =
	            new javax.xml.transform.stream.StreamSource(xslInput);
		    javax.xml.transform.Result result =
	            new javax.xml.transform.stream.StreamResult(strWriter);
		    // create an instance of TransformerFactory
		    javax.xml.transform.TransformerFactory transFact = javax.xml.transform.TransformerFactory.newInstance();
		    javax.xml.transform.Transformer trans = transFact.newTransformer(xsltSource);

		    trans.transform(xmlSource, result);
//		    System.out.println(strWriter.toString());

            Path beast2xml = Paths.get(beast1xml.getParent().toString(), beast1xml.getFileName().toString().replace(".xml", "") + ".beast2.xml");
            try (BufferedWriter writer = Files.newBufferedWriter(beast2xml, Charset.defaultCharset())) {
                writer.write(strWriter.toString());
            } catch (IOException x) {
                System.err.format("IOException: %s%n", x);
            }
            System.out.println("Convert BEAST 1 xml " + beast1xml.getFileName() + " to BEAST 2 xml " + beast2xml.getFileName().toString());

		} catch (Exception e) {
			e.printStackTrace();
		}
	} // main

} // class Beast1To2
