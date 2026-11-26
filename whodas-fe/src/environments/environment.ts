export const environment = {
  production: false,
  // URL relativi: le chiamate restano same-origin e il proxy di sviluppo
  // (src/proxy.conf.json) le inoltra al gateway su localhost:7001.
  // Nessun header CORS e' quindi necessario lato backend.
  baseUrl: '/api',
  apiVersion: 'v1'
};