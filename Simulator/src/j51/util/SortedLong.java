
package j51.util;

import java.util.Iterator;
import java.util.Set;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author xuyi
 */
public class SortedLong extends java.util.TreeMap
{
    
	public void put(long key, String value)
	{
		Long l = key;
		Object o;
		java.util.ArrayList vector;
		
		vector = (java.util.ArrayList)get(l);
		
		if (vector == null){
			vector = new java.util.ArrayList();
			put(l, vector);
		}
		
		vector.add(value);
	}

	public JTree createTree()
	{
		DefaultMutableTreeNode root = new DefaultMutableTreeNode();
		java.util.ArrayList keys = new java.util.ArrayList();

		Set set = keySet();
		Iterator iter = set.iterator();

		while (iter.hasNext()){
			keys.add(iter.next());
                }
		
		for (int i = 0; i < keys.size() ; i++)
		{
			Long l = (Long)keys.get(keys.size() - 1 -i);
			String s = l+"";
			while (s.length() < 16){
				s = " " + s;
                        }
			
			java.util.ArrayList v = (java.util.ArrayList)get(l);
			if (v.size() > 1)
			{
				DefaultMutableTreeNode node = new DefaultMutableTreeNode(s);
				root.add(node);
			
				for (int j = 0 ; j < v.size() ; j++)
				{
					DefaultMutableTreeNode n1 = new DefaultMutableTreeNode(v.get(j).toString());
					node.add(n1);
				}
			} else {
				DefaultMutableTreeNode node = new DefaultMutableTreeNode(s+" "+v.get(0).toString());
				root.add(node);
			}
		}
			
		return new JTree(root);
	}
	
}
