ROOT := $(shell pwd)
TEST_WIKI := /Users/tees/Sync/wiki

c:
	@cargo build

clean_all:
	@rm -rf $(TEST_WIKI)/_firn

clean_site:
	@rm -rf $(TEST_WIKI)/_firn/_site

# Firn commands

new: c clean_all
	@./target/debug/firn2 new -d  $(TEST_WIKI)

build: c clean_site
	@./target/debug/firn2 build -d $(TEST_WIKI)

serve: c clean_site
	./target/debug/firn2 serve -p 8081 -d $(TEST_WIKI)

# new + build
nb: new build

build_release:
	cargo build --release
	strip target/release/firn2
	ls -la -h target/release

build_time: c clean_site
	time ./target/debug/firn2 build -d $(TEST_WIKI)

install: c
	cp target/debug/firn2 /usr/local/bin

flamegraph:
	flamegraph --root ./target/debug/firn2 build -d $(TEST_WIKI)
