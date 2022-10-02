run: assemble
	cd static-site ; jekyll serve

assemble:
	./gradlew bundleJs
	cp -r demo-camera/build/distributions static-site/js/
	cp -r demo-dance/build/distributions static-site/js/
	cp -r demo-imgui-light/build/distributions static-site/js/
	cp -r demo-physic/build/distributions static-site/js/
	cp -r demo-shader/build/distributions static-site/js/
	cp -r demo-text/build/distributions static-site/js/
	cp -r demo-threed/build/distributions static-site/js/
	cp -r demo-twod/build/distributions static-site/js/

deploy: assemble
	cd static-site ; jekyll build
