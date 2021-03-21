run:
	jekyll serve

assemble:
	./gradlew bundle-js
	cp -r build/distributions js/

deploy: assemble
	jekyll build
