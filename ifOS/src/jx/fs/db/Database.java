package jx.db;

import jx.db.types.TypeManager;

public interface Database extends jx.zero.Portal {

    /***************************************************************************************/

    /** returns the id of the table for a given table name
     * @param szRelName name of the table
     * @throws CodedException thrown on error ( wrong name, etc. )
     * @return the table
     */
    public Table getTable(String szRelName);

    /** returns a String describing the attributes of a table
     * @param szTableName name of the table
     * @throws CodedException thrown on error ( wrong input, etc. )
     * @return a String object containing the description text
     */
    public String describeTable( String szTableName );

    /** creates a new internal table
     * @param szName name of the new table
     * @param aszNames array of attribute names
     * @param aiSizes array of attribute sizes
     * @param aiTypes array of attribute datatypes
     * @throws CodedException thrown on error ( table name exists, wrong array lengths, etc. )
     */
    public Table createTable(String szName, String[] aszNames, int[] aiSizes, int[] aiTypes );

    /** deletes a table from the database and all its describing data
     * @param szRelName table name
     * @throws CodedException thrown on error ( wrong name, delete error, etc. )
     */
    public void dropTable( final String szRelName );

    /** flushes all modified data to the persistent media
     */
    public void flush();


    /***************************************************************************************/

    public TypeManager getTypeManager();

    public BufferList getTmpStorage( int iDataSize );

    /***************************************************************************************/

    /**
     * @param table table to scan
     * @param index index of table
     */
    public RelationScan getRelationScan(Table table, Index index, Key start, Key end);


    /***************************************************************************************/
    //Registry routines

    public int getRegKeyData(int iKey);
    public boolean regKeyExist(int iKey);
    public void setRegKeyData(int iKey, int iData);

}
