.PHONY: serve
serve:
	docker compose up --build server

.PHONY: language-stdlib-doc
language-stdlib-doc:
	mvn -f ../pom.xml clean install -DskipTests -pl language-stdlib-doc -am

.PHONY: htmltest
htmltest:
	docker compose run --build htmltest
	docker compose down -v
