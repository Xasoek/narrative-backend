# EMO Narrative

Backend and visual editor for layered narrative graphs.

## Full stack with Docker

```bash
docker compose up --build
```

Open the editor at http://localhost:3000. The API remains available at
http://localhost:8082 and OpenAPI UI at http://localhost:8082/swagger-ui.html.

The editor starts with the project ID used by `seed_nodes.py`. Use the project
menu in the top bar to open another project by UUID.

## Frontend development

Start the backend on port `8082`, then run:

```bash
cd frontend
npm install
npm run dev
```

Vite serves the editor at http://localhost:5173 and proxies `/api` plus uploaded
narrative assets to the backend.

## Frontend checks

```bash
cd frontend
npm run build
npm run lint
```
