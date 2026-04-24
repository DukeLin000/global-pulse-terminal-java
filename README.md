# global-pulse-terminal-java

Spring Boot backend for `global-pulse-terminal` Next.js frontend integration.

## Run locally

```bash
./gradlew bootRun
```

Backend default URL:

- `http://localhost:8080`
- API base: `http://localhost:8080/api/v1`

## Frontend env (`.env.local`)

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

## API endpoints

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/health` | health check |
| GET | `/api/v1/terminal/snapshot` | aggregated terminal snapshot |
| GET | `/api/v1/news` | news list with region/q/limit/cursor/since |
| GET | `/api/v1/conflicts` | conflict list with region/minScore/sort |
| GET | `/api/v1/live-sources` | live news sources |
| GET | `/api/v1/globe/conflict-zones` | globe hotspots |
| GET | `/api/v1/globe/routes` | globe routes by mode |
| GET | `/api/v1/alerts` | alert list |
| POST | `/api/v1/alerts/ack` | acknowledge an alert |
| GET | `/api/v1/stream` | SSE stream |

## Example curl commands

```bash
curl "http://localhost:8080/api/v1/health"

curl "http://localhost:8080/api/v1/terminal/snapshot?region=global&include=routes,alerts,sources"

curl "http://localhost:8080/api/v1/news?region=middle-east&limit=10"

curl "http://localhost:8080/api/v1/conflicts?minScore=70&sort=score"

curl "http://localhost:8080/api/v1/globe/routes?mode=flight"

curl "http://localhost:8080/api/v1/alerts?limit=1"

curl -X POST "http://localhost:8080/api/v1/alerts/ack" \
  -H "Content-Type: application/json" \
  -d '{"alertId":"<alert-id>","ackBy":"ops-user"}'
```

## SSE EventSource example

```js
const source = new EventSource('http://localhost:8080/api/v1/stream?region=global');

source.addEventListener('snapshot.updated', (event) => {
  console.log('snapshot', JSON.parse(event.data));
});

source.addEventListener('conflict.spike', (event) => {
  console.log('conflict spike', JSON.parse(event.data));
});

source.addEventListener('alert.created', (event) => {
  console.log('alert', JSON.parse(event.data));
});

source.addEventListener('news.breaking', (event) => {
  console.log('breaking news', JSON.parse(event.data));
});

source.addEventListener('heartbeat', (event) => {
  console.log('heartbeat', JSON.parse(event.data));
});
```

## CORS

Allowed origins configured in `application.yml`:

- `http://localhost:3000`
- `http://127.0.0.1:3000`

Allowed methods: `GET`, `POST`, `OPTIONS`.
