.PHONY: build run clean

build:
	cd iou-contract && cargo build --target wasm32-unknown-unknown --release
	cp iou-contract/target/wasm32-unknown-unknown/release/iou.wasm ./iou.wasm
	mvn compile

run: build
	mvn exec:java -Dexec.mainClass="Main"

clean:
	cd iou-contract && cargo clean
	mvn clean
	rm -f iou.wasm
