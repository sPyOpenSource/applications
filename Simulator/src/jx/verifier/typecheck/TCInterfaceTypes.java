package jx.verifier.typecheck;

import jx.verifier.VerifyException;
import java.util.Vector;
import java.util.Enumeration;
import jx.classfile.ClassData;
import jx.compiler.ClassFinder;

public class TCInterfaceTypes extends TCTypes {
    private Vector interfaces = new Vector(1); //vector of all interfaces that are implemented
    public Vector getInterfaces() {return interfaces;}
    
    private static ClassFinder classFinder = null;
    public static void setClassFinder(ClassFinder classFinder) {
	TCInterfaceTypes.classFinder = classFinder;
    }
    
    private TCInterfaceTypes(Vector interfaces) {
	super(INTERFACE_REF);
	this.interfaces=interfaces;
    }
    
    public TCInterfaceTypes(String intName) {
	super(INTERFACE_REF);
	if (intName.charAt(0)=='[') {
	    throw new Error("Internal Error: Trying to create a TCInterfaceTypes object for an array!\nType: " + intName);
	}
	interfaces = getSuperInterfaces(intName);
	interfaces.addElement(intName);
    }
    public TCInterfaceTypes(String[] intNames) {
	super(INTERFACE_REF);
        for (String intName : intNames) {
            addUnique(interfaces, intName);
            addUnique(interfaces, getSuperInterfaces(intName));
        }
    }
    /** adds element to target (Vector of Strings), if target does not already contain that string.
     */
    private void addUnique(Vector target, String element) {
	for (Enumeration enume = target.elements(); enume.hasMoreElements(); ) {
	    if (((String)enume.nextElement()).equals(element))
		return; // element found --> dont add again
	}
	target.addElement(element);
    }
    /** adds each element of source to target, if not already contained (both Vectors of String)
     */
    private void addUnique(Vector target, Vector source) {
	for (Enumeration enume = source.elements(); enume.hasMoreElements(); )
	    addUnique(target, (String)enume.nextElement());
    }

    @Override
    public String toString() {
	StringBuilder ret = new StringBuilder("");
	for (Enumeration ifaces = interfaces.elements();
	     ifaces.hasMoreElements();) {
	    ret.append(ifaces.nextElement().toString()).append("*");
	}
	return ret.toString();
    }

    /* FOUND.consistentWith(EXPECTED) */
    public void consistentWith(TCInterfaceTypes other) throws VerifyException {
	//expected oInt, found this --> this must implement all interfaces that oInt implements!
    OUTER_FOR: 
	for (Enumeration oINames = other.getInterfaces().elements();
	     oINames.hasMoreElements();
	     )
	    { 
		String actOI = (String) oINames.nextElement();
		for (Enumeration tINames = interfaces.elements();
		     tINames.hasMoreElements();) {
		    String actTI = (String)tINames.nextElement();
		    if (actOI.equals(actTI)){
			continue OUTER_FOR;
		    }
		}
		//if this point is reached, no appropriate interface was fount in this --> error
		throw  new VerifyException("  Inconsistent Types: Found " + 
					   this + 
					   ", Expected " + 
					       other);
	    }
    }

    // useless for normal Types. Necessary for Object Types
    public TCInterfaceTypes merge(TCInterfaceTypes other) throws VerifyException {
	    Vector result = new Vector(2);
	    //merge into result
	    //add all interfaces to result that are int both interfaces
	    for (Enumeration tInt = interfaces.elements();
		 tInt.hasMoreElements();
		 ) {
		String actTInt = (String)tInt.nextElement();
		for (Enumeration oInt = other.getInterfaces().elements();
		     oInt.hasMoreElements();
		     ) {
		    if (actTInt.equals((String)oInt.nextElement())) {
			result.addElement(actTInt);
			break;
		    }
		}
	    }

	    return new TCInterfaceTypes(result);

    }
	
    //get all interfaces and superinterfaces 
    public Vector getSuperInterfaces(String iFace) {
	Vector ret = new Vector(2);
	ClassData cData = classFinder.findClass(iFace);
	String[] ints = cData.getInterfaceNames();
        for (String inf : ints) {
            ret.addElement(inf);
            for (Enumeration supInts = getSuperInterfaces(inf).elements(); supInts.hasMoreElements();) {
                ret.addElement(supInts.nextElement());
            }
        }
	return ret;
	
    }
    @Override
    public boolean equals(TCTypes other) {
	if (other == null) {
            return interfaces.isEmpty();
	} else 	return other instanceof TCInterfaceTypes;
    }
}
