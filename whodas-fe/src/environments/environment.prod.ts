export const environment = {
  production: true,
  // Stessa forma della configurazione di sviluppo: in produzione il reverse
  // proxy (stesso dominio) inoltra /api al gateway, quindi nessun CORS.
  baseUrl: '/api',
  apiVersion: 'v1'
};