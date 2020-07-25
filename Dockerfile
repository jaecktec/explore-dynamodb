FROM oracle/graalvm-ce:20.1.0-java11 as graalvm
RUN gu install native-image

COPY . /home/app/explore-dynamodb
WORKDIR /home/app/explore-dynamodb

RUN native-image --no-server --report-unsupported-elements-at-runtime -cp build/libs/explore-dynamodb-*-all.jar

FROM frolvlad/alpine-glibc
RUN apk update && apk add libstdc++
EXPOSE 8080
COPY --from=graalvm /home/app/explore-dynamodb/explore-dynamodb /app/explore-dynamodb
ENTRYPOINT ["/app/explore-dynamodb"]
