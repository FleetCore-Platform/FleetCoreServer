.PHONY: clean test run install package

default: clean install

install:
	./mvnw install

clean:
	rm -rf target

test:
	./mvnw test -Ptest

run:
	./mvnw quarkus:dev