package models;

import java.util.Collections;
import java.util.Set;

import utils.collections.Sets;

/**
 * Profiling information for an account. Just for debugging and optimizations
 *
 */
public class AccountStats {

	public final static Set<String> ALL = Collections.unmodifiableSet(Sets.create("numConsentsOwner","numConsentsAuth","numOwnStreams","numOtherStreams","numUserGroups"));
	
	public long numConsentsOwner;
	
	public long numConsentsAuth;
	
	public long numOwnStreams;
	
	public long numOtherStreams;
	
	public int numUserGroups;
	
	
}
