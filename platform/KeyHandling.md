# Protection of user private keys

# Definitions

## Operation "split K into K_part1, K_part2"

random(key) = random byte sequence with same length as provided key

a xor b = Computes bytewise a XOR b

Note that (a xor b) xor b = a

For any given key K you can do:

K_part1 = random(K)
K_part2 = K xor K_part1

Now you have a pair (K_part1, K_part2) which can reconstruct K:

K = K_part2 xor K_part1

But with K_part1 or K_part2 alone you cannot reconstruct K.


## List of scenarios
- User Registration (USER-REG)
- User login (USER-LOGIN)
- API Request with session (REQUEST)
- User logout (USER-LOGOUT)
- App Instance : Prepare refresh token (REFRESH-PREPARE)
- App Instance : Use refresh token (REFRESH-USE)
- Prepare key recovery (RECOVERY-PREPARE)
- Start key recovery (RECOVERY-START)
- Finalize key recovery (RECOVERY-FINALIZE)
 
## User Registration (USER-REG):
### execute on client: registration page
- Generate key pair (privateKey, publicKey)
- username := Have user enter username
- password := Have user enter password 
- password_hash := hash(password)
- priv_pw := encrypt(privateKey, password)
- also execute RECOVERY-PREPARE

### transfer to server
- send username, publicKey, password_hash, priv_pw
- do NOT send: password, privateKey

### execute on server: create user
- password_hash_hash := hash(password_hash)
- store in DB: username as User.email
- store in DB: password_hash_hash as User.password
- store in DB: publicKey as User.publicExtKey
- store in DB: priv_pw as KeyInfoExtern.privateKey

### execute on server: prepare first login
- Generate key pair (server_public, server_private_in_memory_only)
- Execute “KeyManager.newFutureLogin” which does:
- aesKey := generate an aes key
- FutureLogin.user = current user id
- FutureLogin.intPart = encrypt server_private_in_memory_only with aesKey
- FutureLogin.extPartEnc = encrypt aesKey with User.publicExtKey
- store in DB: server_public as User.publicKey, FutureLogin
- do NOT store in DB: server_private_in_memory_only

## Login (USER-LOGIN)
### execute on client : login page
- username := Have user enter username
- password := Have user enter password
- password_hash := hash(password)

### send to server
- send username, password_hash

### execute on server : password verification
- password_hash_hash := hash(password_hash)
- verify password_hash_hash in DB (field User.password)

### send to client
- send KeyInfoExtern.privateKey, FutureLogin.extPartEnc

### execute on client : "login challenge"
- privateKey := decrypt KeyInfoExtern.privateKey with password
- aesKey := decrypt FutureLogin.extPartEnc with privateKey

### send to server
- send aesKey

### execute on server : reconstruct server private key
- server_private_in_memory_only := decrypt FutureLogin.intKey with aesKey
- execute “KeyManager.newFutureLogin” again (see USER-REG)

### execute on server: persist current session
- split server_private_in_memory into passkey, PersistedSession.splitKey
- use passkey as part of sessionToken, but do not store passkey or complete sessionToken
- store in DB: PersistedSession
- do NOT store in DB: passkey, sessionToken

### send to client
- send: sessionToken

## API Request (REQUEST)

### send to server
- send: session_token, other request data

### execute on server: reconstruct server private key
- Extract passKey from session_token
- server_private_in_memory := PersistedSession.splitKey XOR passKey
- use server_private_in_memory for request, but don’t store

## User logout (USER-LOGOUT)
### execute on server
- delete from db: PersistedSession.splitKey

## App Instance : Prepare refresh token (REFRESH-PREPARE)
- do after USER-LOGIN
- for historical reasons the process is more difficult than actually needed. The steps labeled A1,A2,A3 have basically the same purpose as B1,B2,B3. The process could be reduced to A1,B2,B3 (where B2,B3 use the split key from A1)

### execute on server
- Generate key pair (app_public_key, app_private_key_in_memory)
- create access permission set for app. Grant the app instance access using app_public_key.
- (A1) split server_private_in_memory into key1, key2
- (A2) Store key1 as KeyInfo.privateKey with KeyInfo.id=X
- (A3) Store (X,key2) encrypted into access permission set
- (B1) split app_private_key_in_memory into Keyinfo.privateKey, key_part
- (B2) store in DB: Keyinfo.privateKey with KeyInfo.id=AppInstance.id
- (B3) use in refresh token: key_part
- use in authentication token: key_part


## App Instance : Use authentication/refresh token (REFRESH-USE)
- input token (refresh token or authentication token)

### execute on server
- extract key_part from token
- app_private_key_in_memory := key_part XOR KeyInfo.privateKey (with KeyInfo.id=AppInstance.id)
- load access permission set, get (X,key2) from access permission set
- server_private_key_in_memory := key2 XOR KeyInfo.privateKey (with KeyInfo.id=X)
- if token is refresh token : execute REFRESH-PREPARE
- if token is authentication token: process request

## Prepare key recovery (RECOVERY-PREPARE)
- precondition: each midata admin that should be able to do key recovery has a public/private key pair (recovery_public, recovery_private)
- his recovery_private key is only on his own computer (outside of the platform)
- the recovery_public key is stored in midata. The collection of all recovery_public keys are available to the browser

### execute on client : (as part of registration)
- split privateKey (from registration) using shamirs secret sharing into one part for each admin 
- encrypt each part with one recovery_public key 

### send to server
- send: all encrypted key parts

## Start key recovery (RECOVERY-START)

### execute on client
- ask for new password
- generate new keys as during USER-REGISTRATION (steps: registration page and create user)
- generate new recovery information as during RECOVERY-PREPARE

### execute on server
- copy the old recovery data to the active recovery proccesses
- save the new key information and recovery data for later

## Finalize key recovery (RECOVERY-FINALIZE)
- at least two midata admins need to decrypt a recovery key part with their recovery_private key on their local machiene and enter the decrypted key part back into midata

### send to client (of admin)
- send decrypted key parts to client
- send FutureLogin.extPartEnc to client

### execute on client
- use shamirs secret sharing to reconstruct privateKey
- aesKey := decrypt FutureLogin.extPartEnc with privateKey

### send to server
- send aesKey

### execute on server : replace keys
- server_private_in_memory_only := decrypt FutureLogin.intKey with aesKey
- replace old key information with new key information generated during RECOVERY-START
- execute “KeyManager.newFutureLogin” again (see USER-REG)
