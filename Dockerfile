From ubuntu:focal

#non-interactive install for tzdata
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y tzdata
ENV TZ=Asia/Tokyo

#install java
RUN apt-get update && apt-get install -y default-jre
