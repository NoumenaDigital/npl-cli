.PHONY: serve
serve:
	docker compose up --build server

.PHONY: htmltest
htmltest:
	docker compose run --build htmltest
	docker compose down -v
