# WCFC Manuals Client

This is the SvelteKit-based frontend for the Wings of Carolina Flight Club manuals system.

## Getting started

Install dependencies and run the project in development mode:

```bash
npm install
npm run dev
```

Open up [localhost:3000](http://localhost:3000) and start using the application.

## Building

To create a production build:

```bash
npm run build
```

## Structure

This is a standard SvelteKit application with the following structure:

### src/routes

Contains the application pages and API routes:

- Pages are Svelte components in `+page.svelte` files
- Server-side logic is in `+page.server.js` files
- API routes are in `+server.js` files

### static

Contains static assets that are served directly.

## Deployment

The application is built as part of the Maven build process and integrated with the Java backend.
