# newport-whale-api

Spring Boot REST API serving Newport Beach whale‐sighting data via scheduled web‐scrapes

# Overview

The **Newport Whale API** is a Spring Boot 3 REST service that transforms the public sighting logs of Southern California's marine life, published at [Newport Whale Sightings](newportwhales.com), into clean, queryable JSON. A small crawler fetches the daily `whalecount.html` page, parses each row into domain records, and stores them in PostgreSQL so clients can filter, paginate, and analyze sightings without scraping.

## Who this is for

* **Researchers & students** who need structured time-series data on sightings (by species, by day).
* **Operators & analysts** who want quick rollups (e.g., “show me days with sharks but no whales”).
* **Developers** building dashboards, alerts, or lightweight data products over a stable API.

## What you can do

### Browse species

* `GET /api/v1/species` – list all species in the dataset with derived metadata (first/last seen, totals).
* `GET /api/v1/species/{id}` – detail + rollups over an optional date window.

### Browse daily reports

* `GET /api/v1/reports` – list day-level reports with filters: `start`, `end`, `speciesId`, `group`, `hasSightings`, `status`.
* `GET /api/v1/reports/{date}` – fetch a single day including per-species tallies.

### Filter flexibly

* Windowed queries, species-presence queries, or “days without sightings of group X”.

### Paginate reliably

* All list endpoints return Spring-style pages for predictable client UX.

---

# Table of Contents

- [Quickstart](#quickstart)
  - [1) "Hello, API" (curl)](#1-hello-api-curl)
  - [2) JavaScript (Browser fetch)](#2-javascript-browser-fetch)
  - [3) Node.js (Axios)](#3-nodejs-axios)
  - [4) Python (requests)](#4-python-requests)
  - [5) Swift (iOS/macOS – URLSession + async/await)](#5-swfit-iosmacos--urlsession--asyncawait)

- [Core Data Model & Concepts](#core-data-model--concepts)
  - [Entities & relationships](#entities--relationships)
    - [Species](#species)
    - [DailyReport](#dailyreport)
    - [Observation](#observation)
  - [Enums](#enums)
  - [Identifiers & casing](#identifiers--casing)

- [Using the API](#using-the-api)
  - [Versioning](#versioning)
  - [Pagination & sorting](#pagination--sorting)
  - [Dates & time zones](#dates--time-zones)
  - [Authentication & rate limits](#authentication--rate-limits)
  - [Practical Tips](#practical-tips)

- [Error Handling](#error-handling)
  - [Common HTTP status codes](#common-http-status-codes)
  - [Error response schema](#error-response-schema)
  - [Examples](#examples)
  - [Client guidance](#client-guidance)

- [Reference – Endpoints](#reference--endpoints)
  - [`GET /api/v1/species`](#get-apiv1species)
  - [`GET /api/v1/species/{id}`](#get-apiv1speciesid)
  - [`GET /api/v1/reports`](#get-apiv1reports)
  - [`GET /api/v1/reports/{date}`](#get-apiv1reportsdate)

- [Appendix – Catalogs (Authoritative Lists)](#appendix--catalogs-authoritative-lists)
  - [A. Sighting Groups](#a-sighting-groups)
  - [B. Species Catalog](#b-species-catalog)
  - [C. Report Statuses](#c-report-statuses)

---

# Quickstart

This section shows how to call the API in minutes using popular languages and tools.

**Base URL:** `https://newport-whale-api.onrender.com`

All endpoints extend from the base, e.g.

* `GET /api/v1/species` → `https://newport-whale-api.onrender.com/api/v1/species`
* `GET /api/v1/reports` → `https://newport-whale-api.onrender.com/api/v1/reports`

Responses are JSON. Dates use **ISO `YYYY-MM-DD`**.

## 1) "Hello, API" (curl)

List a few species and their rollups:

```bash
curl -s 'https://newport-whale-api.onrender.com/api/v1/species?size=3&sort=lastSeen,desc'
```

Get daily reports for a window, sorted by date ascending:

```bash
curl -s 'https://newport-whale-api.onrender.com/api/v1/reports?start=2025-08-01&end=2025-08-12&sort=date,asc&size=5'
```

Fetch a single day:

```bash
curl -s 'https://newport-whale-api.onrender.com/api/v1/reports/2025-08-12'
```

## 2) JavaScript (Browser fetch)

> [!WARNING]
> Works in Node 18+ (native `fetch`) and modern browsers. If you see a CORS error in the browser, call the API from your backend or ensure your origin is allowed.

```html
<script>
  async function loadReports() {
    const base = 'https://newport-whale-api.onrender.com/api/v1/reports';
    const params = new URLSearchParams({
      start: '2025-08-01',
      end:   '2025-08-12',
      sort:  'date,asc',
      page:  '0',
      size:  '5'
    });
    const res = await fetch(`${base}?${params.toString()}`);
    if (!res.ok) {
      // See Error Handling section for schema
      const err = await res.json();
      throw new Error(`${res.status} ${err.message}`);
    }
    const page = await res.json();
    console.log('Total reports:', page.totalElements);
    for (const r of page.content) {
      console.log(r.date, r.status, r.tours, r.observations);
    }
  }
  loadReports().catch(console.error);
</script>
```

## 3) Node.js (Axios)

```ts
import axios from 'axios';

const api = axios.create({
  baseURL: 'https://newport-whale-api.onrender.com/api/v1',
  timeout: 10_000
});

async function listSpecies() {
  const { data } = await api.get('/species', {
    params: { group: 'whale', page: 0, size: 10, sort: 'lastSeen,desc' }
  });
  for (const s of data.content) {
    console.log(`${s.id} → first=${s.firstSeen} last=${s.lastSeen}`);
  }
}

listSpecies().catch(err => {
  if (err.response) {
    console.error(err.response.status, err.response.data);
  } else {
    console.error(err.message);
  }
});
```

## 4) Python (requests)

```python
import requests

BASE = "https://newport-whale-api.onrender.com/api/v1"

def get_reports(start, end):
    params = {
        "start": start, "end": end,
        "sort": "date,asc", "page": 0, "size": 5
    }
    r = requests.get(f"{BASE}/reports", params=params, timeout=10)
    r.raise_for_status()
    page = r.json()
    return page

page = get_reports("2025-08-01", "2025-08-12")
print("items:", page["numberOfElements"])
for item in page["content"]:
    print(item["date"], item["status"], item["tours"])
```

## 5) Swfit (iOS/macOS) – URLSession + async/await

```swift
import Foundation

struct ObservationDTO: Decodable { let speciesId: String; let count: Int }

struct ReportDTO: Decodable {
    let date: String
    let tours: Int
    let status: String
    let sourceUrl: String
    let fetchedAt: String
    let observations: [ObservationDTO]
}
struct Page<T: Decodable>: Decodable {
    let content: [T]
    let totalElements: Int
    let totalPages: Int
    let number: Int
    let size: Int
}

func fetchReports() async throws -> Page<ReportDTO> {
    var comps = URLComponents(string: "https://newport-whale-api.onrender.com/api/v1/reports")!
    comps.queryItems = [
        .init(name: "start", value: "2025-08-01"),
        .init(name: "end", value: "2025-08-12"),
        .init(name: "sort", value: "date,asc"),
        .init(name: "page", value: "0"),
        .init(name: "size", value: "5")
    ]
    let (data, resp) = try await URLSession.shared.data(from: comps.url!)
    guard let http = resp as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
        throw URLError(.badServerResponse)
    }
    return try JSONDecoder().decode(Page<ReportDTO>.self, from: data)
}

// Example call
Task {
    do {
        let page = try await fetchReports()
        print("Total:", page.totalElements)
        for r in page.content { print(r.date, r.status, r.tours) }
    } catch {
        print("Error:", error)
    }
}
```

---

# Core Data Model & Concepts

This section introduces the **objects**, **IDs**, and **enums** youʼll see in responses and use in requests.

## Entities & relationships

### Species

A canonical catalog entry for an animal you may see in reports.

* `id` (*string*, *slug*) — stable identifier (e.g., `humpback-whale`, `blue-whale`).
  Use this in filters: `?speciesId=humpback-whale`.
  For all possible `species-id`, please reference [Appendix B](#b-species-catalog).
* `group` (*enum*) — one of: `whale | dolphin | shark | fish | other`.
  For what each group entails, please reference [Appendix A](#a-sighting-groups).
* `commonName` (*string*) — e.g., “Humpback whale”.
* `binomialName` (*string*) — scientific name.
* `aliases` (*string\[]*) — alternative labels the scraper recognizes (e.g., “humpbacks”, “killer whale”).
* `firstSeen` / `lastSeen` (*date, ISO*) — earliest/latest date we have observations for that species.
* `totalReports` (*number*) — count of days in which the species appears.
* `totalIndividuals` (*number*) — sum of individuals across those days.

**Sample**

```json
{
  "id": "humpback-whale",
  "group": "whale",
  "commonName": "Humpback whale",
  "binomialName": "Megaptera novaeangliae",
  "aliases": ["humpback", "humpback whale", "humpbacks"],
  "firstSeen": "2024-05-12",
  "lastSeen": "2025-08-01",
  "totalReports": 12,
  "totalIndividuals": 54
}
```

### DailyReport

One row per calendar date with optional observations.

* `date` (date, ISO) — the reportʼs calendar day (e.g., `2025-08-12`).
  Not every calendar day exists; only days present on the source site.
* `tours` (number ≥ 0) — number of tours that day.
* `status` (enum) — `ok` or `bad_weather`.
  `bad_weather` means trips were canceled/limited; `observations` will be empty.
* `fetchedAt` (timestamp, ISO with offset) — when the crawler fetched the page.
* `sourceUrl` (string) — the page we scraped.
* `observations` (array) — list of species tallies for that day; may be empty.

**Sample**

```json
{
  "date": "2025-08-12",
  "tours": 14,
  "status": "ok",
  "fetchedAt": "2025-08-13T01:05:00Z",
  "sourceUrl": "https://newportwhales.com/whalecount.html",
  "observations": [
    { "speciesId": "fin-whale", "count": 4 },
    { "speciesId": "sunfish", "count": 1 },
    { "speciesId": "common-dolphin", "count": 2855 },
    { "speciesId": "bottlenose-dolphin", "count": 195 }
  ]
}
```

### Observation

A per-species tally within a day.

* (`date`, `speciesId`) — composite identity; thereʼs at most one row per species per day.
* `individuals` / `count` (number ≥ 0) — number of individuals reported that day.

**Sample (as embedded in a DailyReport)**

```json
{ "speciesId": "blue-whale", "count": 2 }
```

**Semantics**

* Counts represent the operatorʼs day-level tallies; they are not guaranteed to be unique individuals across the whole day—treat them as the official per-day totals as published.
* On `bad_weather` days, `observations` are always empty by design.

## Enums

**SightingGroup**
`whale | dolphin | shark | fish | other`
Input is case-insensitive (`?group=WhAle` is accepted). Responses use the canonical lowercase tokens above.

**ReportStatus**
`ok | bad_weather`
Input is case-insensitive. `bad_weather` rows have `tours ≥ 0` but no `observations`.

## Identifiers & casing

* `speciesId` is a **stable slug**; prefer values returned by `GET /api/v1/species`.
  Example: `humpback-whale`, `white-shark`, `common-dolphin`.
* **Dates** are ISO `YYYY-MM-DD` (no time).
* When filtering with `start` / `end`, the window is inclusive.
* **Case handling**: enum inputs (`group`, `status`) are case-insensitive; responses are canonical lowercase.

---

# Using the API

## Versioning

* **Stable base path**: `/api/v1/...`
* Backwards-compatible changes may add optional fields or new endpoints.
* **Breaking changes** (renamed/removed fields, semantics) will bump the major version (`/api/v2`).
* **Deprecations** (if any) will be announced in the README and response headers.

## Pagination & sorting

All list endpoints return a **Spring Page**.

**Query parameters**

* `page` — zero-based page index. Default: `0`.
* `size` — page size. Default: `20`. (Dataset is small; typical sizes 20–100.)
* `sort` — one or more sort directives:

  ```
  sort=field,asc
  sort=field,desc
  ```

  You can repeat `sort` to stack multiple fields.

**Response shape (Page)**

```json
{
  "content": [ /* items */ ],
  "totalElements": 123,
  "totalPages": 7,
  "size": 20,
  "number": 0,
  "sort": { /* Spring sort metadata */ },
  "first": true,
  "last": false,
  "numberOfElements": 20,
  "empty": false
}
```

**Sortable fields (whitelisted)**

* **Reports** (`GET /api/v1/reports`)

    * `date` (maps to `daily_report.report_date`)
    * `tours`
    * `status`
    * `fetchedAt`
      Default: `date,asc` (earliest → latest). You can override with `sort=date,desc`.

* **Species list** (`GET /api/v1/species`)

    * **Effective default**: `lastSeen desc NULLS LAST, id asc`
        (Surfaces active species first; stable tiebreaker by `id`.)
    * Additional explicit sorts may be added later.

> [!TIP]
> When building UIs, always pass an explicit `sort` so changes to defaults donʼt affect UX.

## Dates & time zones

* Date fields (`date`, `start`, `end`) use **ISO-8601** calendar dates: `YYYY-MM-DD`.
* Timestamps (`fetchedAt`) use **ISO-8601** with offset (e.g., `2025-07-27T06:10:00Z`).

**Source semantics**

* Each `date` corresponds to the operatorʼs published calendar day.
* Ingestion runs daily around **6:00 PM America/Los\_Angeles**; `fetchedAt` reflects crawl time.

**Window semantics**

* `start` and `end` are inclusive.
* If both are omitted, the query covers all available data.

## Authentication & rate limits

* Auth: **none** (public preview).
* Rate limiting: **none** at this time. Please be considerate—cache results and avoid tight polling.

## Practical Tips

* Always pass an explicit `sort` for predictable UX.
* Use windows (`start`/`end`) to keep responses small and cacheable.
* For “presence/absence by group,” pair `group` with `hasSightings` (`true` / `false`).
* Prefer species IDs from `/api/v1/species` when building links (e.g., `"blue-whale"`).

---

# Error Handling

## Common HTTP status codes

* **400 Bad Request** – Your input is invalid.
  * Examples:

    * `start` / `end` window where `end < start`
    * `speciesId` and `group` provided together (mutually exclusive)
    * Invalid enum value for `group` (e.g., `?group=whaless`) or `status`
    * Malformed date (e.g., `?start=2025-13-40`)
    * Non-numeric paging (e.g., `?page=foo&size=-1`)

* **404 Not Found** – The requested resource doesnʼt exist.
  * Examples:

    * `GET /api/v1/reports/2024-01-01` where no report exists for that date
    * `GET /api/v1/species/{id}` where `{id}` is unknown

* **405 Method Not Allowed** – The HTTP method is unsupported for the route (e.g., `POST /api/v1/reports`).

* **409 Conflict** – Reserved for future write endpoints (e.g., idempotency/unique constraints).

* **415 Unsupported Media Type** – Reserved for future write endpoints (wrong `Content-Type`).

* **422 Unprocessable Entity** – Reserved for future write endpoints (fails business rules).

* **429 Too Many Requests** – Not enabled right now (no rate limiting).

* **500 Internal Server Error** – Unexpected server error.
    * Youʼll never see a stack trace; message is generic by design.

* **503 Service Unavailable** – Temporary backend outage (e.g., database is unavailable).
    * Retry with backoff; keep requests idempotent.

>[!TIP]
> Treat **400/404** as non-retryable, **503** as retryable, and **500** as retryable with caution.

## Error response schema

Every error returns the same envelope:

```json
{
  "timestamp": "2025-08-31T04:12:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "start must be on or after end",
  "path": "/api/v1/reports"
}
```

**Field semantics**

* `timestamp` — ISO-8601 with offset when the error was generated.
* `status` — HTTP status code (integer).
* `error` — Reason phrase (string).
* `message` — Human-readable explanation. Values are short and safe to display to users.
* `path` — The request path.

## Examples

**400 – Invalid date window**

```http
GET /api/v1/reports?start=2025-08-10&end=2025-08-01
```

```json
{
  "timestamp": "2025-08-31T04:12:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "End date must be on or after start date.",
  "path": "/api/v1/reports"
}
```

**400 – Mutually exclusive filters**

```http
GET /api/v1/reports?speciesId=blue-whale&group=whale
```

```json
{
  "timestamp": "2025-08-31T04:12:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Filters 'speciesId' and 'group' are mutually exclusive.",
  "path": "/api/v1/reports"
}
```

**400 – Invalid enum value**

```http
GET /api/v1/reports?group=whaless
```

```json
{
  "timestamp": "2025-08-31T04:12:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Failed to convert value of type 'java.lang.String' to required type 'SightingGroup'",
  "path": "/api/v1/reports"
}
```

> [!NOTE]
> Enum values are case-insensitive in practice, but unknown values are rejected.

**404 – Report date not found**

```http
GET /api/v1/reports/2024-01-01
```

```json
{
  "timestamp": "2025-08-31T04:12:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Report not found for date: 2024-01-01",
  "path": "/api/v1/reports/2024-01-01"
}
```

**503 – Database unavailable (retryable)**

```http
GET /api/v1/species
```

```json
{
  "timestamp": "2025-08-31T04:12:00Z",
  "status": 503,
  "error": "Service Unavailable",
  "message": "Database is temporarily unavailable. Please retry.",
  "path": "/api/v1/species"
}
```

## Client guidance

* **Validate locally:** ensure dates are ISO (`YYYY-MM-DD`), `end ≥ start`, and avoid sending `speciesId` and `group` together.
* **Enum inputs:** `group` in `whale|dolphin|shark|fish|other`, `status` in `ok|bad_weather`.
* **Retry policy:** Donʼt retry `4xx`. Apply exponential backoff on `503/500`.
* **Paging safety:** keep `size` reasonable (e.g., ≤100). For reproducible results, pass an explicit `sort`.

---

# Reference – Endpoints

This section is the contract for clients. It lists every public endpoint, parameters, validation rules, and concrete examples.

Paging uses **Spring Page** format (see **Using API** section).

For accepted enum/species values, see [Appendix — Catalogs](#appendix--catalogs-authoritative-lists).

## `GET /api/v1/species`

List species present in the dataset with derived rollups (first/last seen dates, counts).

### Query parameters

| Name     | Type   | Required | Notes                                                                                       |
| -------- | ------ | -------- | ------------------------------------------------------------------------------------------- |
| `group`  | enum   | no       | One of `whale, dolphin, shark, fish, other`.                                                |
| `search` | string | no       | Matches `id`, `commonName`, `binomialName`, and `aliases` (case-insensitive, normalized).   |
| `page`   | int    | no       | 0-based. Default `0`.                                                                       |
| `size`   | int    | no       | Default `20`. Reasonable max: `100`.                                                        |
| `sort`   | string | no       | e.g. `sort=lastSeen,desc&sort=id,asc`. Default order is `lastSeen desc NULLS LAST, id asc`. |

### Response (200)

```json
{
  "content": [
    {
      "id": "blue-whale",
      "group": "whale",
      "commonName": "Blue whale",
      "binomialName": "Balaenoptera musculus",
      "aliases": ["blue", "blue whale", "blue whales"],
      "firstSeen": "2024-05-10",
      "lastSeen": "2025-05-11",
      "totalReports": 9,
      "totalIndividuals": 51
    }
  ],
  "totalElements": 42,
  "totalPages": 9,
  "size": 5,
  "number": 0,
  "sort": { "...": "..." },
  "first": true,
  "last": false,
  "numberOfElements": 5,
  "empty": false
}
```

### Examples

```bash
# List whales, newest activity first
curl -s 'https://newport-whale-api.onrender.com/api/v1/species?group=whale&page=0&size=10&sort=lastSeen,desc'

# Free-text search across names/aliases
curl -s 'https://newport-whale-api.onrender.com/api/v1/species?search=bottlenose&size=5'
```

---

## `GET /api/v1/species/{id}`

Details + rollups for a single species over an **optional** date window.

### Path & query

*(as per heading)*

| Name    | Type | Required | Notes                                                          |
| ------- | ---- | -------- | -------------------------------------------------------------- |
| `id`    | path | yes      | `speciesId` from the catalog (e.g., `humpback-whale`).         |
| `start` | date | no       | ISO `YYYY-MM-DD` (inclusive).                                  |
| `end`   | date | no       | ISO `YYYY-MM-DD` (inclusive). If both provided, `end ≥ start`. |

### Response (200)

```json
{
  "id": "humpback-whale",
  "group": "whale",
  "commonName": "Humpback whale",
  "binomialName": "Megaptera novaeangliae",
  "aliases": ["humpback", "humpback whale"],
  "firstSeen": "2024-05-12",
  "lastSeen": "2025-08-01",
  "reportCount": 12,
  "totalIndividuals": 54
}
```

### Errors

* **400 Bad Request** if `end < start`.
* **404 Not Found** if `{id}` doesnʼt exist.

### Examples

```bash
# Windowed rollup
curl -s 'https://newport-whale-api.onrender.com/api/v1/species/humpback-whale?start=2025-06-01&end=2025-08-01'
```

---

## `GET /api/v1/reports`

Page through daily reports with presence/absence filters.

### Query parameters

| Name           | Type    | Required | Notes                                                                                                                                                                                                         |
| -------------- | ------- | -------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `start`        | date    | no       | ISO `YYYY-MM-DD` (inclusive).                                                                                                                                                                                 |
| `end`          | date    | no       | ISO `YYYY-MM-DD` (inclusive). If both provided, `end ≥ start`.                                                                                                                                                |
| `speciesId`    | string  | no       | Only include days where this species appears. **Mutually exclusive** with `group`.                                                                                                                            |
| `group`        | enum    | no       | One of `whale, dolphin, shark, fish, other`.                                                                                                                                                                  |
| `hasSightings` | boolean | no       | With `group`: `true` → days **with** that group; `false` → days **without** that group. If `group` omitted applies to any `observations`. If `group` provided and `hasSightings` omitted → defaults to `true`. |
| `status`       | enum    | no       | `ok` or `bad_weather`.                                                                                                                                                                                        |
| `page`         | int     | no       | 0-based. Default `0`.                                                                                                                                                                                         |
| `size`         | int     | no       | Default `20`.                                                                                                                                                                                                 |
| `sort`         | string  | no       | Whitelisted fields (see **Using API** section).                                                                                                                                                               |

### Validation

* `speciesId` and `group` cannot be used together → `400 Bad Request`.
* If both `start` and `end` are provided, `end` must be **on/after** `start`.

### Response (200)

```json
{
  "content": [
    {
      "date": "2025-07-26",
      "tours": 18,
      "status": "ok",
      "sourceUrl": "https://newportwhales.com/whalecount.html",
      "fetchedAt": "2025-07-27T06:10:00Z",
      "observations": [
        {"speciesId": "humpback-whale", "count": 5},
        {"speciesId": "minke-whale",   "count": 3},
        {"speciesId": "common-dolphin","count": 1750}
      ]
    }
  ],
  "totalElements": 240,
  "totalPages": 48,
  "size": 5,
  "number": 0,
  "sort": { "...": "..." },
  "first": true,
  "last": false,
  "numberOfElements": 5,
  "empty": false
}
```

### Examples

**A) Basic Window (May 1 → May 31, 2025)**

```bash
curl -s 'https://newport-whale-api.onrender.com/api/v1/reports?start=2025-05-01&end=2025-05-31&page=0&size=10&sort=date,asc'
```

**B) Species filter (`blue-whale`) within a window**
Returns only dates whose `observations` contain `"speciesId":"blue-whale"`.

```bash
curl -s 'https://newport-whale-api.onrender.com/api/v1/reports?speciesId=blue-whale&start=2025-05-01&end=2025-08-20&sort=date,asc'
```

**C) Group filter with absence (days where sharks were not seen)**

```bash
curl -s 'https://newport-whale-api.onrender.com/api/v1/reports?group=shark&hasSightings=false&start=2025-06-01&end=2025-06-30&sort=date,asc'
```

**D) Status filter (bad weather only)**

```bash
curl -s 'https://newport-whale-api.onrender.com/api/v1/reports?status=bad_weather&start=2025-01-01&end=2025-03-31&sort=date,asc'
```

**E) Sorting & paging**
Descending by `tours`, then newest `fetchedAt` as tiebreaker

```bash
curl -s 'https://newport-whale-api.onrender.com/api/v1/reports?start=2025-07-25&end=2025-07-27&sort=tours,desc&sort=fetchedAt,desc&page=0&size=3'
```

**F) Bad Request (mutually exclusive filters)**
Returns 400 with a structured error body

```bash
curl -s -i 'https://newport-whale-api.onrender.com/api/v1/reports?speciesId=blue-whale&group=whale'
```

---

## `GET /api/v1/reports/{date}`

Fetch the single daily report by date.

### Path

| Name   | Type | Required | Notes                                                               |
| ------ | ---- | -------- | ------------------------------------------------------------------- |
| `date` | date | yes      | ISO `YYYY-MM-DD`. Must match a date weʼve ingested from the source. |

### Response

```json
{
  "date": "2025-08-12",
  "tours": 14,
  "status": "ok",
  "sourceUrl": "https://newportwhales.com/whalecount.html",
  "fetchedAt": "2025-08-13T01:05:00Z",
  "observations": [
    { "speciesId": "fin-whale", "count": 4 },
    { "speciesId": "sunfish",   "count": 1 },
    { "speciesId": "common-dolphin", "count": 2855 },
    { "speciesId": "bottlenose-dolphin", "count": 195 }
  ]
}
```

### Errors

* `404 Not Found` if no report exists for that date.

### Examples

```bash
curl -s 'https://newport-whale-api.onrender.com/api/v1/reports/2025-07-26'
```

---

# Appendix – Catalogs (Authoritative Lists)

These are the **canonical values** accepted by the API and stored in the database. IDs are stable. Input for enums and aliases is **case-insensitive** (e.g., `?group=WhAle` is fine). To obtain the active set programmatically, prefer `GET /api/v1/species`.

## A. Sighting Groups

| value   | description                  |
| ------- | ---------------------------- |
| whale   | Whales (baleen & toothed)    |
| dolphin | Dolphins                     |
| shark   | Sharks                       |
| fish    | Non-shark fish               |
| other   | Misc. cetaceans / rare cases |

## B. Species Catalog

**Columns:** `speciesId` (stable key), `group`, human-readable `commonName` and `binomialName`, and example `aliases` that the resolver recognizes. Aliases are normalized (spacing, dashes, quotes).

| speciesId                   | group   | commonName                  | binomialName                 | exampleAliases                                                                                         |
| --------------------------- | ------- | --------------------------- | ---------------------------- | ------------------------------------------------------------------------------------------------------ |
| gray-whale                  | whale   | Gray whale                  | *Eschrichtius robustus*      | gray, gray whale, gray whales, grey, grey whales                                                       |
| blue-whale                  | whale   | Blue whale                  | *Balaenoptera musculus*      | blue, blue whale, blue whales                                                                          |
| fin-whale                   | whale   | Fin whale                   | *Balaenoptera physalus*      | fin whale, fin whales, fin                                                                             |
| humpback-whale              | whale   | Humpback whale              | *Megaptera novaeangliae*     | humpback, humpback whale, humpback whales, humpbacks                                                   |
| minke-whale                 | whale   | Minke whale                 | *Balaenoptera acutorostrata* | minke, minke whale, minkes, minke whales                                                               |
| orca                        | whale   | Orca                        | *Orcinus orca*               | orca, killer whale, killer whales                                                                      |
| brydes-whale                | whale   | Brydeʼs whale               | *Balaenoptera brydei*        | bryde, brydeʼs whale, brydes whale, brydeʼs whales                                                     |
| common-dolphin              | dolphin | Common dolphin              | *Delphinus delphis*          | common dolphin, common dolphins, common                                                                |
| bottlenose-dolphin          | dolphin | Bottlenose dolphin          | *Tursiops truncatus*         | bottlenose dolphin, bottlenose dolphins, bottlenose                                                    |
| pacific-white-sided-dolphin | dolphin | Pacific white-sided dolphin | *Lagenorhynchus obliquidens* | pws dolphin, pacific white-sided dolphin, pacific white sided dolphin, white-sided/white sided dolphin |
| rissos-dolphin              | dolphin | Rissoʼs dolphin             | *Grampus griseus*            | risso, rissoʼs dolphin, rissoʼs dolphins, rissos dolphin, risso dolphin                                |
| mako-shark                  | shark   | Mako shark                  | *Isurus oxyrinchus*          | mako shark, mako, mako sharks                                                                          |
| thresher-shark              | shark   | Thresher shark              | *Alopias vulpinus*           | thresher shark, thresher, thresher sharks                                                              |
| hammerhead-shark            | shark   | Hammerhead shark            | *Sphyrna zygaena*            | hammerhead shark, hammerhead, hammerhead sharks                                                        |
| white-shark                 | shark   | White shark                 | *Carcharodon carcharias*     | white shark, white sharks, great white, great white shark                                              |
| sunfish                     | fish    | Ocean sunfish               | *Mola mola*                  | sunfish, ocean sunfish, mola mola                                                                      |
| false-killer-whale          | other   | False killer whale          | *Pseudorca crassidens*       | false killer whale, false-killer whale, false killer                                                   |

> [!NOTE]
> **Why aliases matter:** the scraper normalizes text from the source site and resolves it through these aliases to the canonical `speciesId`. If you display user-facing names, prefer `commonName` from `/api/v1/species`.

## C. Report Statuses

| status       | meaning                               |
| ------------ | ------------------------------------- |
| ok           | Data collected normally               |
| bad\_weather | Trips canceled/limited due to weather |

**Notes**

* API inputs/outputs use the exact enum tokens above (e.g., `status=bad_weather`).
* Bad weather days legitimately return an empty `observations` array.

---



