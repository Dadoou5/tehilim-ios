// Configuration PM2 — backend Tehilim (VPS).
// Déployé dans /var/www/tehilim. Trois process indépendants :
//   - tehilim-api       : serveur HTTP (push + healthcheck)
//   - tehilim-scheduler : cron (phase 2) — NE PAS démarrer tant que pg_cron est actif.
//   - tehilim-realtime  : serveur WebSocket (phase 3) — requiert DATABASE_URL +
//                         SUPABASE_JWT_SECRET ; démarrer en dual-run (cf. doc).
// Les variables d'env sont chargées par Node via --env-file (Node ≥ 20.6).

module.exports = {
  apps: [
    {
      name: "tehilim-api",
      script: "dist/api.js",
      cwd: "/var/www/tehilim",
      node_args: "--env-file=/var/www/tehilim/.env",
      instances: 1,
      exec_mode: "fork",
      max_memory_restart: "300M",
      error_file: "/var/log/tehilim/api-error.log",
      out_file: "/var/log/tehilim/api-out.log",
      time: true,
    },
    {
      name: "tehilim-scheduler",
      script: "dist/scheduler.js",
      cwd: "/var/www/tehilim",
      node_args: "--env-file=/var/www/tehilim/.env",
      instances: 1,
      exec_mode: "fork",
      max_memory_restart: "200M",
      error_file: "/var/log/tehilim/scheduler-error.log",
      out_file: "/var/log/tehilim/scheduler-out.log",
      time: true,
    },
    {
      name: "tehilim-realtime",
      script: "dist/realtime.js",
      cwd: "/var/www/tehilim",
      node_args: "--env-file=/var/www/tehilim/.env",
      instances: 1,
      exec_mode: "fork",
      max_memory_restart: "400M",
      error_file: "/var/log/tehilim/realtime-error.log",
      out_file: "/var/log/tehilim/realtime-out.log",
      time: true,
    },
  ],
};
