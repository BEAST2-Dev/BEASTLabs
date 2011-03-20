package beast.prevalence;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Node;

import beast.core.Description;
import beast.core.StateNode;

@Description("Prevalence list is a linked list with times and actions")
public class PrevalenceList extends StateNode {

	@Override
	public void initAndValidate() {
	}

	ArrayList<Item> m_items = new ArrayList<PrevalenceList.Item>();
	ArrayList<Item> m_storeditems = new ArrayList<PrevalenceList.Item>();
	
	HashMap<Integer,Item> m_nodeIDtoItemMap = new HashMap<Integer, PrevalenceList.Item>();
	HashMap<Integer,Item> m_storedNodeIDtoItemMap = new HashMap<Integer, PrevalenceList.Item>();
	
	// total number of infections and recoveries in the list
	int m_nInfections = 0;
	int m_nRecoveries = 0;
	int m_nStoredInfections = 0;
	int m_nStoredRecoveries = 0;

	enum Action {INFECTED,RECOVERED,NONEVENT};

	/** Item contains info related to an item in the prevalence list 
	 * An Item is immutable, which means a move operation on the PrevalenceList
	 * requires a copy with a new time.
	 * This allows for ease of restoring when a proposed state is
	 * rejected.
	 */
	class Item implements Comparable<Item> {
		public final Action m_action;
		public final double m_fTime;
		public final int m_nNodeID;

		public Item(double fTime, Action action, int nNodeID) {
			m_action = action;
			m_fTime = fTime;
			m_nNodeID = nNodeID;
		}

		public Item(Item other) {
			m_action = other.m_action;
			m_fTime = other.m_fTime;
			m_nNodeID = other.m_nNodeID;
		}
		@Override
		// compares time of the items first of all, then nodeID then Action (RECOVERED<INFECTED)
		// "Note: this class has a natural ordering that is inconsistent with equals." 
		public int compareTo(Item item) {
			if (item.m_fTime > m_fTime) {
				return 1;
			} else 	if (item.m_fTime < m_fTime) {
				return -1;
			}
			// equal times
			if (item.m_nNodeID > m_nNodeID) {
				return 1;
			} else 	if (item.m_nNodeID < m_nNodeID) {
				return -1;
			}
			// equal times and equal node IDs
			if (item.m_action == m_action) {
				return 0;
			} else 	if (m_action == Action.INFECTED) {
				return 1;
			} else {
				return -1;
			}
		}
	} // class Item

	
	public List<Item> getItems() {return m_items;}
	
	public int indexOfNode(int nNodeID) throws Exception {
//		// linear search
//		for (int iTime = 0; iTime < m_items.size(); iTime++) {
//			if (m_items.get(iTime).m_nNodeID == nNodeID) {
//				return iTime;
//			}
//		}
//		return -1;
		// keep a hashmap that maps node numbers to
		// items, then to a binarysearch for the item in the list
		if (m_nodeIDtoItemMap.containsKey(nNodeID)) {
			Item item = m_nodeIDtoItemMap.get(nNodeID);
			int iTime = Arrays.binarySearch(m_items.toArray(), item);
			return iTime;
		}
		throw new Exception("Cannot find item for node ID " + nNodeID + ". " +
				"Choose on of " + m_nodeIDtoItemMap.keySet().toString());
	}
	
	/** insert infection or recovery at time fTime 
	 * return true if successful, false if the time
	 * is already occupied.
	 * If a node in a tree is associated with the item, nNodeID should
	 * be non-negative. Otherwise -1 will do.
	 *  */
	public boolean add(double fTime, Action action, int nNodeID) {
		Item item = new Item(fTime, action, nNodeID);
		int iTime = Arrays.binarySearch(m_items.toArray(), item);
		if (iTime >= 0) {
			return false;
		}
		iTime = -iTime - 1;
		m_items.add(iTime, item);
		if (action == Action.INFECTED) {
			m_nInfections++;
		} else {
			m_nRecoveries++;
		}
		return true;
	}

	public boolean add(int iTime, double fTime, Action action, int nNodeID) {
		Item item = new Item(fTime, action, nNodeID);
		m_items.add(iTime, item);
		if (nNodeID >= 0) {
			m_nodeIDtoItemMap.put(nNodeID, item);
		}
		if (action == Action.INFECTED) {
			m_nInfections++;
		} else {
			m_nRecoveries++;
		}
		return true;
	}
	
	/** remove item at time fTime,
	 * Throws exception if deletion failed 
	 * (if there is no item at time fTime or an item associated 
	 * with a tree node is attempted to be deleted) **/
	public void delete(int iTime) throws Exception {
		if (iTime >= 0 && iTime < m_items.size()) {
			Item item = m_items.get(iTime); 
			if (item.m_nNodeID >= 0) {
				throw new Exception("Cannot delete an item associated with a tree node. " +
						"Perhaps you want to use move() instead");
			}
			m_items.remove(iTime);
			if (item.m_action == Action.INFECTED) {
				m_nInfections--;
			} else {
				m_nRecoveries--;
			}
		}
		throw new Exception("Cannot delete an item outside the range of the list");
	}
	
	/** move entry at index iTem to fTargetTime.
	 * throws Exception if iTime is outside range of list. **/
	public void move(int iTime, double fTargetTime) throws Exception {
		if (iTime >= 0 && iTime < m_items.size()) {
			Item item = m_items.get(iTime);
			m_items.remove(iTime);
			Item movedItem = new Item(fTargetTime, item.m_action, item.m_nNodeID);
			iTime = Arrays.binarySearch(m_items.toArray(), movedItem);
			if (iTime < 0) {
				iTime = -iTime - 1;
			}
			m_items.add(iTime, movedItem);

			if (item.m_nNodeID >= 0) {
				m_nodeIDtoItemMap.remove(item.m_nNodeID);
				m_nodeIDtoItemMap.put(item.m_nNodeID, movedItem);
			}
		
		}
		throw new Exception("Cannot move an item outside the range of the list");
	}
	
	
	/** return highest time in prevalence  list **/
	public double startTime() {
		return m_items.get(m_items.size()-1).m_fTime;
	}
	/** return number of items in prevalence list **/
	int getSize() {return m_items.size();}
	Item get(int iTime) {return m_items.get(iTime);}
	
	/** return true if item at index iTime is linked to a tree node **/
	public boolean isLinked(int iTime) {
		Item item = m_items.get(iTime);
		return item.m_nNodeID >= 0;
	}

	/** @return true if there is at least on item in the list that is not
	 * associated with a tree node (with the exception of the first item
	 * in the list).
	 */
	public boolean hasDeletables() {
		if (m_items.size() - m_nodeIDtoItemMap.size() > 1) {
			return true;
		}
		return false;
	}
	
	/********************************************************************************/
	/** standard StateNode methods follow **/
	@Override
	public void assignFrom(StateNode other) {
		PrevalenceList list = (PrevalenceList) other;
		m_nInfections = list.m_nInfections;
		m_nRecoveries = list.m_nRecoveries;
		List<Item> otherItems = list.m_items;
//		int i = 0;
//		int j = 0;
//		while (i < otherItems.size() || j < m_items.size()) {
//			if (i == otherItems.size()) {
//				// we are at the end of the otherItems list
//				m_items.remove(j);
//			} else if (j == m_items.size()) {
//				// we are at the end of the m_items list
//				m_items.add(otherItems.get(i));
//				i++;
//				j++;
//			} else {
//				int c = otherItems.get(i).compareTo(m_items.get(j));
//				if (c == 0) {
//					// both lists are equal
//					i++;
//					j++;
//				} else if (c < 0) {
//					// item from otherItems must be inserted
//					m_items.add(j, otherItems.get(i));
//					i++;
//					j++;
//				} else {
//					// item from m_items must be deleted
//					m_items.remove(j);
//				}
//			}
//		}
		m_items.clear();
		m_items.addAll(otherItems);
		m_nodeIDtoItemMap.clear();
		m_nodeIDtoItemMap.putAll(list.m_nodeIDtoItemMap);
	}

	@Override
	public void assignFromFragile(StateNode other) {
		PrevalenceList list = (PrevalenceList) other;
		m_nInfections = list.m_nInfections;
		m_nRecoveries = list.m_nRecoveries;
		m_items.clear();
		m_items.addAll(list.m_items);
		m_nodeIDtoItemMap.clear();
		m_nodeIDtoItemMap.putAll(list.m_nodeIDtoItemMap);
	}

	@Override
	public void assignTo(StateNode other) {
		PrevalenceList list = (PrevalenceList) other;
		list.m_nInfections = m_nInfections;
		list.m_nRecoveries = m_nRecoveries;
		list.m_items.clear();
		list.m_items.addAll(m_items);
		list.m_nodeIDtoItemMap.clear();
		list.m_nodeIDtoItemMap.putAll(m_nodeIDtoItemMap);
	}

	@Override
	public StateNode copy() {
		PrevalenceList list = new PrevalenceList();
		assignTo(list);
		return list;
	}

	@Override
	public void fromXML(Node node) {
		// TODO Auto-generated method stub

	}

	@Override
	/** scale only those items associated with a node and leave the rest **/
	public int scale(double fScale) throws Exception {
		Set<Integer> nodes = m_nodeIDtoItemMap.keySet();
		for (Integer nNodeID : nodes) {
			int iTime = indexOfNode(nNodeID);
			Item item = m_items.get(iTime); 
			move(iTime, item.m_fTime * fScale);
		}
		return nodes.size();
	}

	@Override
	public void setEverythingDirty(boolean isDirty) {
	}

	/** Loggable interface methods **/
	@Override
	public void close(PrintStream out) {
	}

	@Override
	public void init(PrintStream out) throws Exception {
	}

	@Override
	public void log(int nSample, PrintStream out) {
	}

	/** Valuable interface methods **/
	@Override
	public double getArrayValue() {
		return 0;
	}

	@Override
	public double getArrayValue(int iDim) {
		return 0;
	}

	@Override
	public int getDimension() {
		return 0;
	}

	@Override
	public void restore() {
		int tmp = m_nStoredInfections; m_nStoredInfections = m_nInfections; m_nInfections = tmp;
		tmp = m_nStoredRecoveries; m_nStoredRecoveries = m_nRecoveries; m_nRecoveries = tmp;

		ArrayList<Item> tmp2 = m_storeditems;
		m_storeditems = m_items;
		m_items = tmp2;
		
		HashMap<Integer,Item> tmp3 = m_storedNodeIDtoItemMap;
		m_storedNodeIDtoItemMap = m_nodeIDtoItemMap;
		m_nodeIDtoItemMap = tmp3;
	}

	@Override
	protected void store() {
		int tmp = m_nStoredInfections; m_nStoredInfections = m_nInfections; m_nInfections = tmp;
		tmp = m_nStoredRecoveries; m_nStoredRecoveries = m_nRecoveries; m_nRecoveries = tmp;

		// TODO: this looks inefficient. If this show up during 
		// profiling, perhaps fix this...
		m_storeditems.clear();
		for (Item item : m_items) {
			Item copy = new Item(item); 
			m_storeditems.add(copy);
		}
		
		m_storedNodeIDtoItemMap.clear();
		for (Integer key : m_nodeIDtoItemMap.keySet()) {
			Item item = m_nodeIDtoItemMap.get(key);
			Integer i = m_items.indexOf(item);
			m_storedNodeIDtoItemMap.put(key, m_storeditems.get(i));
		}
	}

}
