FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:4a2340345d8af038d57c0eb81638accb07023b3a06ff301d35689107c6ae7fb7
ENV TZ="Europe/Oslo"
COPY build/libs/app.jar app.jar
CMD ["-jar","app.jar"]