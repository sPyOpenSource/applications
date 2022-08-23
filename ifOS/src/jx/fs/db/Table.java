package jx.fs.db;

public interface Table extends jx.zero.Portal  {
    /** creates a new index for a table
     * @param iType specifies the type of the index
     * @param bUnique unique or non unique index
     * @param iAttrMap positions of the attributes of the table which are indexed
     * @param cTD tuple descriptor of the table which should contain the index
     * @throws CodedException thrown on error ( wrong TD, etc.)
     */
    public Index createIndex(int iType, boolean bUnique, int[] iAttrMap);


    /** creates a new tuple and returns a TupleWriter referencing it. The caller must call TupleWriter.close after modifying the tuple
     * @throws CodedException thrown on error ( wrong TD, write error, etc. )
     * @return TupleWriter object allowing modifications of the new tuple
     */
    public TupleWriter createTuple();
    /** returns a TupleWriter object referencing a tuple for modification
     * @param cSetNumReader SetNumberReader object containing the address of the tuple
     * @throws CodedException thrown on error ( wrong SetNumber, etc. )
     * @return returns a TupleWriter object, allowing modification of the tuple
     */
    public TupleWriter modifyTuple(SetNumberReader cSetNumReader);
    /** deletes a tuple from the persistent storage
     * @param cSetNumReader address of the tuple
     * @throws CodedException thrown on error ( wrong tuple address, etc )
     */
    public void deleteTuple(SetNumberReader cSetNumReader);

    /** returns a TupleReader reference to a tuple. The caller can use this reference in order to read the contents of the tuple
     * @param cSetNumReader address of the tuple
     * @throws CodedException thrown on error ( wrong address, etc. )
     * @return returns a TupleReader object pointing to the tuple
     */
    public TupleReader readTuple(SetNumberReader cSetNumReader );

    public TupleDescriptor getTupleDescriptor();

    public String getName();

    public Index[] getAllTableIndexes();
}
