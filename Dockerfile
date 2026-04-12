FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copier le wrapper Maven et le pom pour cacher les dépendances
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copier le code source
COPY src/ src/

# Port de l'application
EXPOSE 8081

# Lancement en mode dev avec hot-reload (spring-boot-devtools)
CMD ["./mvnw", "spring-boot:run"]
