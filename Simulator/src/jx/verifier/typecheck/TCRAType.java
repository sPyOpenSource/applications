package jx.verifier.typecheck;

import jx.verifier.VerifyException;

//Type for return addresses
public class TCRAType extends TCTypes {
    private final int addressValue;
    
    public int getAddress() { return addressValue;}
    public TCRAType(int address) {
	super(RETURN_ADDR);
	addressValue = address;
    }
    @Override
    public String toString() {
	return "RETURN_ADDRESS for Subroutine at " + Integer.toHexString(addressValue);
    }
    /* FOUND.consistentWith(EXPECTED) */
    @Override
    public void consistentWith(TCTypes other) throws VerifyException {
	if (other.getType() == ANY_REF ||
	    (other instanceof TCRAType) &&
	    (((TCRAType)other).getAddress() == addressValue)
	    )
	    return;

	throw new VerifyException("Inconsistent Types: Expected " + other +
				  " found " + this);
    }
}
