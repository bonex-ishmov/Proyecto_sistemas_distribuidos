# =====================================================================
# ms-clientes/Dockerfile  --  imagen del microservicio (docker-compose.yml de la raíz)
# Dos etapas: 1) compila el jar con el Maven wrapper del módulo (JDK),
#             2) solo corre el jar (JRE, imagen más liviana, sin Maven ni fuentes).
# Ningún secreto entra en la imagen: application-local.properties está en el
# .dockerignore y la configuración llega por variables de entorno (ver compose).
# =====================================================================
# --platform=$BUILDPLATFORM: esta etapa corre en la arquitectura de la máquina que
# construye (x86 en un PC), aunque la imagen final sea para ARM. El .jar es
# bytecode, igual en cualquier arquitectura, así que no hace falta emular ARM.
FROM --platform=$BUILDPLATFORM eclipse-temurin:25-jdk AS compilacion
WORKDIR /app
# Primero solo el pom: así las dependencias quedan en una capa propia y no se
# vuelven a descargar cuando únicamente cambia el código.
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src src
# Sin tests: se corren aparte con ./mvnw test; aquí solo se empaqueta.
RUN ./mvnw -q package -DskipTests

# Sin --platform: Docker toma la variante de la arquitectura destino
# (linux/amd64 o linux/arm64, según --platform de docker buildx build).
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=compilacion /app/target/*.jar app.jar
# Dentro del contenedor todos escuchan en 8080 (SERVER_PORT en el compose).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
