ROOT := $(shell pwd)
TEST_WIKI := /Users/tees/Sync/wiki

c:
	@cargo build


# install deps for mac
install-mac:
	brew install upx

clean_all:
	@rm -rf $(TEST_WIKI)/_firn

clean_site:
	@rm -rf $(TEST_WIKI)/_firn/_site

# Firn commands

new: c clean_all
	@./target/debug/firn new -d  $(TEST_WIKI)

build: c clean_site
	@./target/debug/firn build -d $(TEST_WIKI)

serve: c clean_site
	./target/debug/firn serve -p 8081 -d $(TEST_WIKI)

# new + build
nb: new build


## release stuff

build_release:
	cargo build --release
	strip target/release/firn
	ls -la -h target/release

build_release_linux:
	cross build --target x86_64-unknown-linux-gnu --release

build_for_gh_release: build_release build_release_linux
	mkdir -p target/gh_out
	zip -j firn_x86_64-apple-darwin target/release/firn
	zip -j firn-x86_64-unknown-linux-gnu target/x86_64-unknown-linux-gnu/release/firn
	mv firn_x86_64-apple-darwin.zip target/gh_out
	mv firn-x86_64-unknown-linux-gnu.zip target/gh_out

build_time: c clean_site
	time ./target/debug/firn build -d $(TEST_WIKI)

install: c
	cp target/debug/firn /usr/local/bin

flamegraph:
	flamegraph --root ./target/debug/firn build -d $(TEST_WIKI)
