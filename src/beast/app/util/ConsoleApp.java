package beast.app.util;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javax.swing.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import beast.app.BEASTVersion2;
import beast.core.util.Log;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;


public class ConsoleApp extends JFrame {

	private static final long serialVersionUID = 1L;
	private final JFXPanel jfxPanel = new JFXPanel();
	private WebEngine engine;
	static String title = "BEAST " + new BEASTVersion2().getVersionString();

	private final JPanel panel = new JPanel(new BorderLayout());

	public ConsoleApp() {
		super();
	}

	public ConsoleApp(String title, String header, Object o) {
		ConsoleApp.title = ConsoleApp.title + " " + title;
		ConsoleApp.main(new String[] {});
	}

	public void initComponents() {
		createScene();

		PrintStream p1 = new PrintStream(new BOAS("color:blue"));
		PrintStream p2 = new PrintStream(new BOAS("color:red"));
		PrintStream p3 = new PrintStream(new BOAS("color:green"));
		System.setOut(p1);
		System.setErr(p2);
		Log.err = p2;
		Log.warning = p2;
		Log.info = p1;
		Log.debug = p3;
		Log.trace = p3;

		// Log.info.println("BEAST " + new BEASTVersion().getVersionString());

		new Thread() {
			public void run() {
				try {
					sleep(2000);
					// clear backlog if any
					logToView(null, null);
				} catch (InterruptedException e) {
				}
			};
		}.start();

		panel.add(jfxPanel, BorderLayout.CENTER);

		getContentPane().add(panel);

		setPreferredSize(new Dimension(1024, 600));
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setTitle(title);

	}

	private void createScene() {

		Platform.runLater(new Runnable() {
			@Override
			public void run() {

				WebView view = new WebView();
				engine = view.getEngine();

				StringBuilder script = new StringBuilder().append("<html>");
				script.append("<head>");
				script.append("   <script language=\"javascript\" type=\"text/javascript\">");
				script.append("       function toBottom(){");
				script.append("           window.scrollTo(0,document.body.scrollHeight);");
				script.append("       }");
				script.append("   </script>");
				script.append("</head>");
				script.append("<body onload='toBottom()'>");
				script.append("<div id='content' style='color:#0000D0;padding:0;border:0;margin:0;'> "
						+ "<pre id='pre'></pre></div></body></html>");
				engine.loadContent(script.toString());

				jfxPanel.setScene(new Scene(view));
			}
		});
	}


	class Message {
		public Message(String data, String style) {
			this.data = data;
			this.style = style;
		}

		String data;
		String style;
	};

	List<Message> backLog = new ArrayList<>();

	void logToView(String _data, String _style) {
		// new Runnable() {
		// public void run() {
		Platform.runLater(new Runnable() {
			public void run() { /* your code here */
				Document doc = engine.getDocument();
				if (_style != null) {
					backLog.add(new Message(_data, _style));
				}
				if (doc == null) {
					return;
				}
				for (Message msg : backLog) {
					String data = msg.data;
					String style = msg.style;
					Element newLine = doc.createElement("DIV");
					newLine.setAttribute("style", "padding: 0 0 0 0;" + style);
					newLine.appendChild(doc.createTextNode(data));
					Element el = doc.getElementById("pre");
					el.appendChild(newLine);
					// el.appendChild(doc.createElement("BR"));
				}
				engine.executeScript("toBottom()");
				backLog.clear();
			}
		});
		// }
		// };

	}

	/** logging with colour **/
	class BOAS extends ByteArrayOutputStream {
		String style;
		StringBuilder buf = new StringBuilder();

		BOAS(String style) {
			this.style = style;
		}

		@Override
		public synchronized void write(byte[] b, int off, int len) {
			super.write(b, off, len);
			log(b, off, len);
		};

		@Override
		public synchronized void write(int b) {
			super.write(b);
			log(b);
		};

		@Override
		public void write(byte[] b) throws java.io.IOException {
			super.write(b);
			log(b);
		};

		private void log(byte[] b, int off, int len) {
			for (int i = off; i < len; i++) {
				log(b[i]);
			}
		}

		private void log(int b) {
			if (b == '\n') {
				logToView(buf.toString(), style);
				buf = new StringBuilder();
			} else {
				buf.append((char) b);
			}

		}

		private void log(byte[] b) {
			for (byte i : b) {
				if (i == 0) {
					return;
				}
				log(i);
			}
		}

		@Override
		public void flush() throws java.io.IOException {
			super.flush();
		};

		@Override
		public void close() throws IOException {
			super.close();
		}
	};

	public static void main(String[] args) {
		ConsoleApp browser = new ConsoleApp();
		browser.initComponents();
		browser.setTitle(title);
		browser.setVisible(true);
		Log.info.println("ok");
	}
}
