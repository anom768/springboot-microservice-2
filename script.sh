# keycloak
docker run -p 8181:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:18.0.0 start-dev

# zipkin
docker run -d -p 9411:9411 openzipkin/zipkin

# kafka docker compose
docker compose up -d

# build default dockerfile
docker build -t apigateway-dockerfile .

# build name dockerfile
docker build -t apigateway-layered -f Dockerfile.layered .