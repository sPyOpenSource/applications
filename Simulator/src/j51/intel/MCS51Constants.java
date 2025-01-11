/**
 * $Id: MCS51Constants.java 45 2010-06-22 20:53:26Z mviara $
 */
package j51.intel;

/**
 * Constants for MCS51 microprocessor family.
 *
 * @author Mario Viara
 * @version 1.00
 */
public interface MCS51Constants
{
	/**
	 * Internal register
	 */
	static public final int ACC	=	0xe0;
	static public final int B	=	0xF0;
	static public final int PSW	=	0xd0;
	static public final int PSW_CY	=	0x80;
	static public final int PSW_AC	=	0x40;
	static public final int PSW_F0	=	0x20;
	static public final int PSW_RS1	=	0x10;
	static public final int PSW_RS0	=	0x08;
	static public final int PSW_OV	=	0x04;
	static public final int PSW_F1	=	0x02;
	static public final int PSW_P	=	0x01;

	static public final int SP	=	0x81;
	static public final int DPL	=	0x82;
	static public final int DPH	=	0x83;

	/**
	 * I/O Port
	 *
	 * Any port (Px) have 2 bit to register (PxM1,PxM2) to
	 * define  the mode.
	 * 
	 * PxM1		PxM2
	 * 0		0		Quasi bidirectional
	 * 0		1		Push pull
	 * 1		0		Input only
	 * 1		1		Open drain.
	 */
	static public final int P0	= 0x80;
	static public final int P0M1	= 0x84;
	static public final int P0M2	= 0x85;
	
	static public final int P1	= 0x90;
	static public final int P1M1	= 0x91;
	static public final int P1M2	= 0x92;
	
	static public final int P2	= 0xA0;
	static public final int P2M1	= 0xA4;
	static public final int P2M2	= 0xA5;
	
	static public final int P3	= 0xB0;
	static public final int P3M1	= 0xB1;
	static public final int P3M2	= 0xB2;

	/*
	 * Serial port
	 */
	static public final int SCON	= 0x98;
	static public final int SCON_TI	= 0x02;
	static public final int SCON_RI	= 0x01;
	static public final int SBUF	= 0x99;

	/**
	 * Timer
	 */
	static public final int TCON	= 0x88;
	static public final int TCON_TF1= 0x80;
	static public final int TCON_TR1= 0x40;
	static public final int TCON_TF0= 0x20;
	static public final int TCON_TR0= 0x10;

	static public final int TMOD		= 0x89;
	static public final int TMOD_GATE1	= 0x80;
	static public final int TMOD_C_T1	= 0x40;
	static public final int TMOD_T1_M1	= 0x20;
	static public final int TMOD_T1_M0	= 0x10;
	static public final int TMOD_GATE0	= 0x08;
	static public final int TMOD_C_T0	= 0x04;
	static public final int TMOD_T0_M1	= 0x02;
	static public final int TMOD_T0_M0	= 0x01;
	static public final int TL0		= 0x8a;
	static public final int TL1		= 0x8b;
	static public final int TH0		= 0x8c;
	static public final int TH1		= 0x8d;

	/**
	 * Interrupt enable
	 */
	static public final int IE		= 0xA8;
	static public final int IE_EA		= 0x80;
	static public final int IE_EC		= 0x40;
	static public final int IE_ET2		= 0x20;
	static public final int IE_ES		= 0x10;
	static public final int IE_ET1		= 0x08;
	static public final int IE_EX1		= 0x04;
	static public final int IE_ET0		= 0x02;
	static public final int IE_EX0		= 0x01;
}
