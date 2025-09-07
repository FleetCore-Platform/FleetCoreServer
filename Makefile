.PHONY: install clean format lint test run

default: clean format run

install:
	./mvnw install

clean:
	rm -rf target

format:
	./mvnw spotless:apply

lint:
	./mvnw verify

test:
	./mvnw test

run:
	./mvnw quarkus:dev
