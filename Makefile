run: assemble
	jekyll serve

assemble:
	./gradlew bundleJs
	cp -r web/build/distributions js/

deploy: assemble
	jekyll build
