changelog:
	bin/changelog.sh

dev-parser:
	bin/build-dev-parser

deps-clojure:
	cd clojure; lein deps; # Fetching

deps-rust:
	cd rust; cargo build # Fetching rust deps and building.

install: deps-clojure deps-rust dev-parser

release: changelog
