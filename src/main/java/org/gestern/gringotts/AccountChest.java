package org.gestern.gringotts;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Represents a storage unit for an account.
 * 
 * @author jast
 *
 */
public class AccountChest {

	private final Logger log = Bukkit.getLogger();
    
    private final DAO dao = DAO.getDao();

    /** Sign marking the chest as an account chest. */
    public final Sign sign;
    
    /** Account this chest belongs to. */
    public final Account account;

    /** Non-static utility methods. */
    private final Util util = new Util();

    /**
     * 
     * @param sign
     * @param account
     */
    public AccountChest(Sign sign, Account account) {
    	if (sign == null || account == null)
    		throw new IllegalArgumentException("null arguments to AccountChest() not allowed. args were: sign: " + sign + ", account: " + account);
        this.sign = sign;
        this.account = account;
    }
    
    /**
     * The actual "chest" containing this account chest's stuff.
     * @return
     */
    private InventoryHolder chest() {
    	Block storage = sign.getBlock().getRelative(BlockFace.DOWN);
    	if (validContainer(storage.getType()))
    		return ((InventoryHolder)storage.getState());
    	else
    		return null;
    }
    
    /** 
     * Location of the storage block of this account chest.
     * @return
     */
    private Location chestLocation() {
    	Block storage = sign.getBlock().getRelative(BlockFace.DOWN);
    	return storage.getLocation();
    }
    
    /**
     * Get inventory of this account chest.
     * @return inventory of this accountchest, if any. otherwise null.
     */
    private Inventory inventory() {
    	InventoryHolder chest = chest();
    	return (chest != null)? chest.getInventory() : null;
    }
    
    /**
     * Test if this chest is valid, and if not, removes it from storage.
     * @return true if valid, false if not and was removed from storage.
     */
    private boolean updateValid() {
    	if (!valid()) {
    		log.info("[Gringotts] Destroying orphaned vault: " + this);
    		destroy();
    		return false;
    	}
    	else return true;
    }

    /**
     * Return balance of this chest.
     * @return balance of this chest
     */
    public long balance() {
    	
    	if (!updateValid())
    		return 0;
    		
    	Inventory inv = inventory();
    	if (inv==null) return 0;
    	
        return util.balanceInventory(inv);
    }

    /**
     * Return the capacity of this chest.
     * @return capacity of this chest
     */
    public long capacity() {
    	
    	if (!updateValid())
    		return 0;

    	Inventory inv = inventory();
    	if (inv==null) return 0;
    	
    	return util.capacityInventory(inv);
    }

    /**
     * Attempts to add given amount to this chest. 
     * If the amount is larger than available space, the space is filled and the actually
     * added amount returned.
     * @return amount actually added
     */
    public long add(long value) {
    	
    	if (!updateValid())
    		return 0;
    	
    	Inventory inv = inventory();
    	if (inv==null) return 0;
    	
        return util.addToInventory(value, inv);
    }

    /**
     * Attempts to remove given amount from this chest.
     * If the amount is larger than available items, everything is removed and the number of
     * removed items returned.
     * @param value
     * @return amount actually removed from this chest
     */
    public long remove(long value) {

    	if (!updateValid())
    		return 0;
    	
    	Inventory inv = inventory();
    	if (inv==null) return 0;
    	
    	return util.removeFromInventory(value, inv);
    }
    
    /**
     * Checks whether this chest is currently a valid vault.
     * It is consideren valid when the sign block contains [vault] on the first line,
     * a name on the third line and has a chest below it.
     * 
     * @return true if the chest can be considered a valid vault
     */
    public boolean valid() {
    	// is it still a sign?
    	if ( ! Util.isSignBlock(sign.getBlock()) ) 
    		return false;
    	
    	String[] lines = sign.getLines();
    	if ( ! ("[vault]".equalsIgnoreCase(lines[0]) || "[faction vault]".equalsIgnoreCase(lines[0]) )) return false;
    	if ( lines[1] == null || lines[2].length() == 0) return false;
  
    	if (chest() == null) return false;
    	
    	return true;
    }

    /**
     * Triggered on destruction of physical chest or sign
     * @return Blocks belonging to this account chest.
     */
    public void destroy() {
    	dao.destroyAccountChest(this);
        sign.getBlock().breakNaturally();
    }
    
    @Override
    public String toString() {
    	Location loc = sign.getLocation();
    	return "[vault] " 
    			+ loc.getBlockX() + ", "
    			+ loc.getBlockY() + ", "
    			+ loc.getBlockZ() + ", "
    			+ loc.getWorld();    			
    }

    /**
     * Connected chests that comprise the inventory of this account chest.
     * @return
     */
    private Chest[] connectedChests() {
        Inventory inv = inventory();
        if (inv == null)
        	return new Chest[0];
        
        if (inv instanceof DoubleChestInventory) {
            DoubleChestInventory dinv = (DoubleChestInventory)inv;
            Chest left = (Chest)(dinv.getLeftSide().getHolder());
            Chest right = (Chest)(dinv.getRightSide().getHolder());

            return new Chest[] {left, right};
        } else {
        	InventoryHolder invHolder = inv.getHolder();
        	if (invHolder instanceof Chest)
        		return new Chest[] {(Chest)(inv.getHolder())};
        }
        
        return new Chest[0];
    }


    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + sign.getLocation().hashCode();
        return result;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        AccountChest other = (AccountChest) obj;
        return sign.getLocation().equals(other.sign.getLocation());
    }

    /**
     * Determine whether the chest of another AccountChest would be connected to this chest.
     * @param chest
     * @return
     */
	public boolean connected(AccountChest chest) {
		
		// no valid account chest anymore -> no connection
		if (! updateValid())
    		return false;
		
		// no double chest -> no connection possible
		if (! (inventory() instanceof DoubleChestInventory))
			return false;
			
		Location myLoc = chestLocation();
		for (Chest c : chest.connectedChests())
			if (c.getLocation().equals(myLoc))
				return true;
		
		return false;
	}

	public Account getAccount() {
		return account;
	}
	
	public static boolean validContainer(Material material) {
		switch (material) {
		case CHEST:
		case DISPENSER:
		case FURNACE:
			return true;
		default:
			return false;
		}
	}

}