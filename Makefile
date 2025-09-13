.PHONY: install clean format lint test run docker-build

default: clean format run

clean:
	rm -rf target

install: clean
	./mvnw install

install-native: clean
	./mvnw install -Pnative

format:
	./mvnw spotless:apply

lint:
	./mvnw verify

test:
	./mvnw test

run:
	./mvnw quarkus:dev

docker-build:
ifeq ($(N),1)
	$(MAKE) install-native
	docker build -t fleetcoreserver:latest -f src/main/docker/Dockerfile.native .
else
	$(MAKE) install
	docker build -t fleetcoreserver:latest -f src/main/docker/Dockerfile.jvm .
endif