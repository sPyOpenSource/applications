/**
 * $Id: LPC900Constants.java 48 2010-06-23 08:28:23Z mviara $
 */
package j51.philips;

/**
 * Constants relative to LPC900.
 *
 * @author Mario Viara
 * @version 1.00
 */
public interface LPC900Constants
{
	static public final int FMCON	= 0xe4;
	static public final int FMDATA	= 0xe5;
	static public final int FMADRL	= 0xe6;
	static public final int FMADRH	= 0xe7;
	static public final int DEECON	= 0xf1;
	static public final int DEEDAT  = 0xf2;
	static public final int DEEADR  = 0xf3;
	static public final int AUXR1	= 0xa2;

	// Reserved always 0
	static public final int AUXR1_0	= 0x02;
	
	// Select DPTR
	static public final int AUXR1_DPS = 0x01;
	
	// Software reset
	static public final int AUXR1_SRST = 0x08;

	static public final int WDCON	= 0xa7;
	static public final int IE_WD	=0x40;

	// 1 Clock 400Khz , 0 Clock CCLK
	static public final int WDCON_WDCLK	= 0x01; 
	static public final int WDCON_WDTOF	= 0x02;
	// 1 WD running
	static public final int WDCON_WDRUN	= 0x04;

	static public final int WDL	= 0xC1;
	static public final int WDFEED1 = 0xC2;
	static public final int WDFEED2 = 0xC3;

	static public final int FLASH_MISC = 0xfff0;

	static public final int UCFG1	= 0x00;
	// 1 Watch dog reset enable
	static public final int UCFG1_WDTE = 0x80;
	// 1 Watch dog safety enable
	static public final int UCFG1_WDSE = 0x10;

	static public final int BOOTV	= 0x02;
	static public final int BOOTSTAT= 0x03;
	static public final int SEC0	= 0x08;
	static public final int SEC1	= 0x09;
	static public final int SEC2	= 0x0A;
	static public final int SEC3	= 0x0B;
	static public final int SEC4	= 0x0C;
	static public final int SEC5	= 0x0D;
	static public final int SEC6	= 0x0E;
	static public final int SEC7	= 0x0F;
}

