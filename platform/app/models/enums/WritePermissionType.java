package models.enums;

public enum WritePermissionType {

	/**
	 * Grantee is not allowed to write records
	 */
	NONE,
	
	/**
	 * Grantee is allowed to update existing records shared by consent
	 */
	UPDATE_EXISTING,
	
	/**
	 * Grantee is allowed to create records that will be shared by consent
	 */
	CREATE_SHARED,
	
	/**
	 * Combination of UPDATE_EXISTING an CREATE_SHARED
	 */
	UPDATE_AND_CREATE,
	
	/**
	 * Grantee is allowed to write any type of record even if not shared by consent
	 */
	WRITE_ANY;
	
	public boolean isUpdateAllowed() {
		return this == UPDATE_EXISTING || this == UPDATE_AND_CREATE || this == WritePermissionType.WRITE_ANY;
	}
	
	public boolean isCreateAllowed() {
		return this == CREATE_SHARED || this == WRITE_ANY;
	}
	
	public boolean isUnrestricted() {
		return this == WRITE_ANY;
	}
}
