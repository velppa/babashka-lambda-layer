build:
	docker build --target BUILDER -t babashka-lambda-archiver .
	docker rm build || true
	docker create --name build babashka-lambda-archiver
	docker cp build:/var/task/babashka-runtime.zip babashka-runtime.zip

publish-layer: build
	aws lambda publish-layer-version \
		--layer-name babashka-runtime \
		--compatible-runtimes provided.al2 \
		--zip-file fileb://babashka-runtime.zip
