.PHONY: clean test run package

default: clean test run

clean:
	rm -rf target

test:
	./mvnw test -Ptest

run:
	./mvnw quarkus:dev