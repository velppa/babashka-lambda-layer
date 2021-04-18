FROM babashka/babashka:0.2.13 as BABASHKA

FROM clojure:openjdk-16-tools-deps-slim-buster as BUILDER
RUN apt-get update && apt-get install -y zip curl
WORKDIR /var/task

RUN mkdir bin
COPY --from=BABASHKA /usr/local/bin/bb bin/bb

ENV GITLIBS=".gitlibs/"
COPY bootstrap bootstrap

COPY deps.edn deps.edn

RUN cd bin && \
    curl -L -o pod-babashka-aws.zip https://github.com/babashka/pod-babashka-aws/releases/download/v0.0.5/pod-babashka-aws-0.0.5-linux-amd64.zip && \
    unzip pod-babashka-aws.zip && \
    rm pod-babashka-aws.zip

RUN clojure -Sdeps '{:mvn/local-repo "./.m2/repository"}' -Spath > cp
COPY src/ src/

RUN ./bin/bb -cp $(cat cp) -m lambda.core --uberscript core.clj
RUN echo "#!/usr/bin/env bb" >> bin/babashka && \
    cat core.clj >> bin/babashka && \
    chmod +x bin/babashka

RUN zip -q -r babashka-runtime.zip bin/ bootstrap
