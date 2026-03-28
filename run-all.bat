start "discovery-server" cmd /c "cd infra\discovery-server && mvn spring-boot:run"
timeout /t 10
start "config-server" cmd /c "cd infra\config-server && mvn spring-boot:run"
timeout /t 10
start "shopping-store" cmd /c "cd commerce\shopping-store && mvn spring-boot:run -Dspring-boot.run.arguments=--debug"
timeout /t 10
start "shopping-cart" cmd /c "cd commerce\shopping-cart && mvn spring-boot:run -Dspring-boot.run.arguments=--debug"
timeout /t 10
start "warehouse" cmd /c "cd commerce\warehouse && mvn spring-boot:run -Dspring-boot.run.arguments=--debug"