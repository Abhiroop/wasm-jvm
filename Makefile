.PHONY: build run clean

build:
	mvn compile

run:
	mvn exec:java -Dexec.mainClass="Main"

clean:
	mvn clean
