package jx.fs.db.types;


public interface TypeManager /*extends jx.zero.Portal*/ {

    public static final int ERR_UNSUPPORTED_DID = 0;

    //standard types, do not modify!
    public static final int DATP_DID_INT = 0;
    public static final int DATP_DID_STR = 1;

    public static final String DATP_DNAME_INT = "int";
    public static final String DATP_DNAME_STR = "str";

    /** returns an object that implements the DbComparator interface, used to compare byte arrays of iDID type.
     * @param typeID datatype id
     * @throws CodedException thrown, if an error occurs
     * @return Comparator object
     */
    public DbComparator getComparator(int typeID);


    /** returns a DbConverter objects for the datatype of the attribute on the given iPos
     * @param typeID position of the attribute in the tuple ( number, not offset!)
     * @throws CodedException thrown on error ( wrong iPos )
     * @return DbConverter objects for the datatype of the attribute on the given iPos
     */
    public DbConverter getConverter(int typeID);


    public int[][] getTypeInfos();

    public String getTypeName( int typeID );

}
