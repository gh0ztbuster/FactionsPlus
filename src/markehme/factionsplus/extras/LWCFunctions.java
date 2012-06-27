package markehme.factionsplus.extras;

import java.util.*;

import markehme.factionsplus.*;

import org.apache.commons.lang.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.griefcraft.model.*;
import com.griefcraft.util.*;
import com.massivecraft.factions.*;

public class LWCFunctions {
	private static LWC lwc;
	private static LWCModule lwcMod=new LWCModule();
 
	public static void integrateLWC(LWCPlugin plugin) {
		lwc = plugin.getLWC();
//TODO: better way of keeping track of lwc for onDisable
		lwc.getModuleLoader().registerModule( FactionsPlus.instance, lwcMod);
	}
	
	public static void disableModules() {
		if (null != lwc) {
			lwc.getModuleLoader().removeModules(  FactionsPlus.instance );
		}
	}
	
	
	//TODO: maybe also prevent non-faction members from LWC locking stuff in our faction land
	
	//if this is true it will only detect&remove those in Material marked as TileEntity
//	private final static boolean useFastWay=false; nvm don't wanna implement both ways
	
	private final static Material[] protectionsTypesToRemove={
		 Material.CHEST //is TileEntity
		,Material.FURNACE //is TileEntity
		,Material.BURNING_FURNACE // unsure if it's TileEntity
		,Material.WALL_SIGN //is TileEntity
		,Material.SIGN_POST //is TileEntity
		,Material.DISPENSER //is TileEntity
		
		,Material.WOODEN_DOOR //is NOT TileEntity
		,Material.IRON_DOOR_BLOCK //is NOT TileEntity
		,Material.TRAP_DOOR //is NOT TileEntity
//		,Material.SIGN //is NOT TileEntity but also redundant because WALL_SIGN+SIGN_POST should detect all signs
		
		//meaning BlockState[] blocks = chunk.getTileEntities(); will only return those marked as TileEntity
		//for more tile entities see this: http://www.minecraftwiki.net/wiki/Tile_entity
	};
	
	
	/**
	 * Clears all LWC locks for the chunk at passed location
	 * except the locks that are of players in the same faction as passed fPlayer
	 * 
	 * @param facLocation
	 *            the FLocation of the faction (chunk-coords basically)
	 * @param fPlayer
	 * @return number of protections removed,<br />
	 *         0 or less means none were found/removed
	 * @throws Exception
	 *             if something failed (tipically this won't happen)
	 */
	public static int clearLocks( FLocation facLocation, FPlayer fPlayer ) throws Exception {
		World world = facLocation.getWorld();
		if ( null == world ) {
			// for some reason if world wasn't loaded (if it can ever happen, unsure)
			throw new Exception( "world wasn't loaded or something ?" );
		}
		Chunk chunk = facLocation.getWorld().getChunkAt( (int)facLocation.getX(), (int)facLocation.getZ() );
		// done: make sure chunk is loaded if not load if fail abort claim
		if ( !world.isChunkLoaded( chunk ) ) {
			world.loadChunk( chunk );
			if ( !chunk.isLoaded() ) {
				// still not loaded?
				throw new Exception( "failed to forceload chunk" );
			}
		}
		// chunk should be loaded if we're here, but supposedly some other thread could unload it though (? unsure) ignoring
		// this possibility
		
		// ---------------
		int numberOfRemovedProtections = 0;
		// parse each block(in the chunk) and if it's of protectionsTypesToRemove then remove the protection from it
		for ( int x = 0; x < 16; x++ ) {
			for ( int z = 0; z < 16; z++ ) {
				for ( int y = 0; y < 256; y++ ) {
					Block block = chunk.getBlock( x, y, z );
					Material type = block.getType();
					if ( type == Material.AIR ) {
						continue;// ignore all air blocks
					}
					if ( isProtectionTypeToRemove( type ) ) {
						// if the chunk contents never get lost somehow then we don't need to cache(in a list) all protected
						// blocks
						// so we can thus remove the protection here while parsing every block of the chunk
						Protection protectedBlock = lwc.findProtection( block );
						if ( null != protectedBlock ) {
							// there is a lock for that block ie. it's a chest
							FPlayer fpOwner = FPlayers.i.get( protectedBlock.getOwner() );
							if ( !fPlayer.getFaction().getFPlayers().contains( fpOwner ) ) {
								// protection owner is not in the faction? then clear the lock
								// only if the owner of the protected block is not in the same faction as fPlayer
								// only then remove the lwc protection from that block (ie. chest)
								protectedBlock.remove();
								numberOfRemovedProtections++;
							}
						}
					}
				}
			}
		}
		return numberOfRemovedProtections;
	}
	
	
	private static boolean isProtectionTypeToRemove( Material type ) {
		return Utilities.isReferenceInArray( type, protectionsTypesToRemove );
	}


	public static boolean checkInTerritory(Player p, Block b) {
		FPlayer fp = FPlayers.i.get(p);
		FLocation floc = new FLocation(b.getLocation());
		Faction owner = Board.getFactionAt(floc);

		if(!owner.isNone() && owner != fp.getFaction()){
			fp.sendMessage("You can only create locks in your own territory!");
			return false;
		}
		return true;
	}


	public static int clearLocksCommand(Player name, Location loc) {
		FPlayer fp = FPlayers.i.get(name);
		if(!FactionsPlus.permission.has(name, "factionsplus.clearlwclocks")) {
			name.sendMessage(ChatColor.RED + "No Permission!");
			return -1; //-1 return signifies lack of permissions.
		}
		Chunk chunk = loc.getChunk();
		FLocation floc = new FLocation(loc);
		Faction owner = Board.getFactionAt(floc);

		if(!owner.isNone() && owner != fp.getFaction()){
			fp.sendMessage("You can only clear locks in your own territory!");
			return -1;
		}
		int numberOfRemovedProtections = 0;
		for ( int x = 0; x < 16; x++ ) {
			for ( int z = 0; z < 16; z++ ) {
				for ( int y = 0; y < 256; y++ ) {
					Block block = chunk.getBlock( x, y, z );
					Material type = block.getType();
					if ( type == Material.AIR ) {
						continue;
					}
					if ( isProtectionTypeToRemove( type ) ) {

						Protection protectedBlock = lwc.findProtection( block );
						if ( null != protectedBlock ) {
							FPlayer fpOwner = FPlayers.i.get( protectedBlock.getOwner() );
							if ( !fp.getFaction().getFPlayers().contains( fpOwner ) ) {
								protectedBlock.remove();
								numberOfRemovedProtections ++;
							}
						}
					}
				}
			}
		}
		return numberOfRemovedProtections;
	}


}
