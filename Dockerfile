FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:17-jre-alpine
# Free, open-source OCR for reading recipe screenshots in /addfood. Alpine ships the English model; the
# Persian one comes from the official tessdata_fast repo, pinned to a release and checksum.
RUN apk add --no-cache tesseract-ocr tesseract-ocr-data-eng \
    && wget -q -O /usr/share/tessdata/fas.traineddata \
       https://github.com/tesseract-ocr/tessdata_fast/raw/4.1.0/fas.traineddata \
    && echo "db1c0a91208aff00d3cf1ed2c1d23f76419afd5f024688b4f71adc3f2ce4a505  /usr/share/tessdata/fas.traineddata" \
       | sha256sum -c -
WORKDIR /data
COPY --from=build /build/target/dishcover-0.0.1-SNAPSHOT.jar /app/dishcover.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/dishcover.jar"]
