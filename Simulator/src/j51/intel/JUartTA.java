/**
 * $Id: JUartTA.java 48 2010-06-23 08:28:23Z mviara $
 */
package j51.intel;

import j51.J51;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

import de.mud.terminal.SwingTerminal;
import de.mud.terminal.vt320;

/**
 * Standard uart with Terminal emulator
 *
 * @author Mario Viara
 * @version 1.00
 */
public class JUartTA extends Uart
{
	private vt320 emulator;
	private SwingTerminal terminal;
	private AbstractAction actionFileSend;
	private JFileChooser   fc = null;

	public JUartTA()
	{
		emulator = new vt320()
		{

			public void write(byte[] b)
			{
				for (int i = 0 ; i < b.length ; i++)
					recvChar(b[i] & 0xff);
			}
		};

		terminal = new SwingTerminal(emulator);
		JMenuBar bar = new JMenuBar();
		bar.add(createMenuFile());
		add(bar,BorderLayout.NORTH);
		add(terminal,BorderLayout.CENTER);
	}

	private void load(String name) throws Exception
	{

		BufferedReader rd = new BufferedReader(new FileReader(name));

		class Loader implements Runnable
		{
			private BufferedReader r;

			Loader(BufferedReader r)
			{
				this.r = r;
			}

			public void run()
			{
				try
				{
					int c;
					
					while ((c = r.read()) != -1)
					{
						recvChar(c & 0xff);
						int count = 0;
						while (cpu.sfrIsSet(SCON,SCON_RI))
						{
							Thread.sleep(10);
							if (++count > 100)
								break;
						}

						if (count >= 100)
							break;
					}
				}
				catch (Exception ignore)
				{
				}

				try
				{
					r.close();
				}
				catch (Exception ignore)
				{
				}
			}
		}

		Thread t = new Thread(new Loader(rd));
		t.start();
	}
	
	private JMenu createMenuFile()
	{
		actionFileSend = new AbstractAction("Send")
		{
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					if (fc == null)
					{
						fc = new JFileChooser();
						fc.setCurrentDirectory(new File("."));
					}
					if (fc.showOpenDialog(J51.getInstance()) == fc.APPROVE_OPTION)
					{
						load(fc.getSelectedFile().getCanonicalPath());
					}
				}
				catch (java.lang.Throwable ex)
				{
					J51.getInstance().messages(ex);
				}
			}
		};

		JMenu menu = new JMenu("File");
		menu.add(actionFileSend);

		return menu;
	}
	
	protected void sendChar(int v)
	{
		byte b[] = new byte[1];
		b[0] = (byte)v;
		emulator.putString(new String(b));

	}
}
