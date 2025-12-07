# WSS WebSocket Server + HTML Client

## Generate keystore:
keytool -genkeypair -alias ws -keyalg RSA -keystore keystore.jks -storepass password -validity 3650

Place keystore.jks in server/ directory.

## Run server:
mvn clean package
mvn exec:java -Dexec.mainClass="com.example.Server"

## Open client:
Open client/index.html in a browser (allow insecure localhost test).