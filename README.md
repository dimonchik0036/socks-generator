# Socks generator

## Build
```bash
./gradlew :fatJar
```

## Run
```bash
java -jar socksgenerator-fat-1.0-SNAPSHOT.jar <path/to/config>
```

## Command list
* `/generate` - Generate and return new UUID key.  
  * Required `key=<secret_key>`  
  * Optional `comment=<single line comment>`  
* `/stats` - Return list of unused keys and current users.  
  * Required `key=<secret_key>`  
* `/remove` - Remove unused key.  
  * Required `key=<key>`  
* `/auth` - Creates a new account socks, login and password must match the regular expression`^[a-zA-Z0-9\-_]+$`. Returns link to socks for Telegrams after successful execution.  
  * Required `key=<key>`  
  * Required `login=<login>`  
  * Required `password=<password>`  

## Config
* `port` - Application port, example `1080`.  
  * Required  
* `host` - Application host, example `127.0.0.1`.  
  * Required  
* `secret_key` - Secret key for admin requests, example `pass1234`  
  * Required  
* `socks_address` - Socks address, example `8.8.8.8`.  
  * Required  
* `socks_port` - Socks port, example `1029`.  
  * Required  
* `keys_path` - Key storage file path, example `storage/keys.txt`  
  * Required  
* `users_path` - Users storage file path, example `storage/users.txt`  
  * Required  
* `script_path` - Auth script file path, example `./create_user.sh`.  
  * Required  
  * Must return `0` on success, otherwise not `0`  