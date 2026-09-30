import express from 'express';
import cors from 'cors';
import morgan from 'morgan';
import dotenv from 'dotenv';
import { Pool } from 'pg';
import { buildSelectQuery, buildModifyQuery } from './queryBuilder.js';

dotenv.config();

const app = express();
app.use(cors());
app.use(express.json());
app.use(morgan('dev'));

const pool = new Pool({
  host: process.env.PGHOST || 'localhost',
  port: +(process.env.PGPORT || 5432),
  user: process.env.PGUSER || 'postgres',
  password: process.env.PGPASSWORD || 'postgres',
  database: process.env.PGDATABASE || 'postgres'
});

app.get('/api/:table', async (req, res) => {
  try {
    const { table } = req.params;
    // Preserve duplicate query params (e.g., date_examen=gte... & date_examen=lte...)
    const url = new URL(req.originalUrl, `http://localhost`);
    const qp = {};
    for (const [k, v] of url.searchParams.entries()) {
      if (qp[k] === undefined) qp[k] = [];
      qp[k].push(v);
    }

async function normalizeSanctionRow(row) {
  const originalDate = toStr(row?.date_examen);
  const out = { ...row };
  if (out.date_examen != null) {
    try {
      const d = new Date(String(out.date_examen));
      out.date_examen = d.toLocaleDateString('en-CA', { timeZone: 'Indian/Antananarivo' });
    } catch (e) {}
  }
  if ((out.session == null || out.session === '') && originalDate && out.numero_salle) {
    try {
      const hd = await deriveHeureDebutFromContext(originalDate, out.numero_salle, out.id_surveillant);
      if (hd != null) {
        const hour = localHourOfDay(hd);
        out.session = (hour < 12) ? 'Matin' : 'Après-midi';
      }
    } catch (_) {}
  }
  return out;
}
    const { text, values } = buildSelectQuery(table, qp);
    const { rows } = await pool.query(text, values);
   
    const t = table.toLowerCase();
    if (t === 'planning_surveillance') {
      res.json(rows.map(normalizePlanningRow));
    } else if (t === 'sanction') {
      const outRows = await Promise.all(rows.map(normalizeSanctionRow));
      res.json(outRows);
    } else if (t === 'examen') {
      res.json(rows.map(normalizeExamenRow));
    } else {
      res.json(rows);
    }
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

app.post('/api/:table', async (req, res) => {
  try {
    const { table } = req.params;
    const body = req.body || {};
    if (table.toLowerCase() === 'sanction') {
      if ((body.session == null || body.session === '') && String(body.type).toUpperCase() === 'ABSENCE') {
        const d = body.date_examen ? String(body.date_examen) : null;
        const salle = body.numero_salle ? String(body.numero_salle) : null;
        if (d && salle) {
          const s = await deriveSessionFromExam(d, salle, body.id_surveillant);
          if (s) body.session = s;
        }
      }
    }
    const { text, values } = buildModifyQuery('insert', table, body);
    const { rows } = await pool.query(text, values);
    if (table.toLowerCase() === 'planning_surveillance') {
      res.json(rows.map(normalizePlanningRow));
    } else {
      res.json(rows);
    }
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

app.patch('/api/:table', async (req, res) => {
  try {
    const { table } = req.params;
    const body = req.body || {};
    const url = new URL(req.originalUrl, `http://localhost`);
    const qp = {};
    for (const [k, v] of url.searchParams.entries()) {
      if (qp[k] === undefined) qp[k] = [];
      qp[k].push(v);
    }
    if (table.toLowerCase() === 'sanction') {
      if ((body.session == null || body.session === '') && String(body.type || '').toUpperCase() === 'ABSENCE') {
        const d = body.date_examen ? String(body.date_examen) : null;
        const salle = body.numero_salle ? String(body.numero_salle) : null;
        if (d && salle) {
          const s = await deriveSessionFromExam(d, salle, body.id_surveillant);
          if (s) body.session = s;
        }
      }
    }
    const { text, values } = buildModifyQuery('update', table, body, qp);
    const { rows } = await pool.query(text, values);
    if (table.toLowerCase() === 'planning_surveillance') {
      res.json(rows.map(normalizePlanningRow));
    } else {
      res.json(rows);
    }
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

app.delete('/api/:table', async (req, res) => {
  try {
    const { table } = req.params;
    const url = new URL(req.originalUrl, `http://localhost`);
    const qp = {};
    for (const [k, v] of url.searchParams.entries()) {
      if (qp[k] === undefined) qp[k] = [];
      qp[k].push(v);
    }
    const { text, values } = buildModifyQuery('delete', table, null, qp);
    await pool.query(text, values);
    res.json([]);
  } catch (err) {
    res.status(400).json({ error: err.message });
  }
});

const port = +(process.env.PORT || 8080);
app.listen(port, () => {
  console.log(`API listening on http://localhost:${port}/api/`);
});


function normalizePlanningRow(row) {
  const out = { ...row };
  const dateStr = toStr(out.date_examen);
  if (dateStr) {
   
    try {
      const d = new Date(String(out.date_examen));
      out.date_examen = d.toLocaleDateString('en-CA', { timeZone: 'Indian/Antananarivo' });
    } catch (_) {}
    // heure_debut
    if (out.heure_debut) {
      const hd = toStr(out.heure_debut);
      if (hd && (hd.includes('T') || hd.includes('-'))) {
        out.heure_debut = formatLocalDateTime(hd);
      } else {
        out.heure_debut = combineDateAndTimeIfNeeded(dateStr, hd);
      }
    }
    // heure_fin
    if (out.heure_fin) {
      const hf = toStr(out.heure_fin);
      if (hf && (hf.includes('T') || hf.includes('-'))) {
        out.heure_fin = formatLocalDateTime(hf);
      } else {
        out.heure_fin = combineDateAndTimeIfNeeded(dateStr, hf);
      }
    }
  }
  return out;
}

function toStr(v) {
  if (v === null || v === undefined) return null;
  return String(v);
}

function combineDateAndTimeIfNeeded(dateStr, timeOrTs) {
  const s = (timeOrTs || '').trim();
  
  if (s.includes('T') || s.includes('-')) return s;
  
  let t = s;
  if (/^\d{2}:\d{2}$/.test(t)) t = t + ':00';
  if (/^\d{2}:\d{2}:\d{2}$/.test(t)) return `${dateStr} ${t}`;
  return s; 
}

function normalizeExamenRow(row) {
  const out = { ...row };
  if (out.date_examen != null) {
    try {
      const d = new Date(String(out.date_examen));
      out.date_examen = d.toLocaleDateString('en-CA', { timeZone: 'Indian/Antananarivo' });
    } catch (_) {}
  }
  if (out.heure_debut != null) {
    try {
      out.heure_debut = formatLocalDateTime(out.heure_debut);
    } catch (_) {}
  }
  if (out.heure_fin != null) {
    try {
      out.heure_fin = formatLocalDateTime(out.heure_fin);
    } catch (_) {}
  }
  return out;
}

function formatLocalDateTime(v) {
  const d = new Date(String(v));
  const dateStr = d.toLocaleDateString('en-CA', { timeZone: 'Indian/Antananarivo' });
  const timeStr = d.toLocaleTimeString('fr-FR', {
    hour12: false,
    timeZone: 'Indian/Antananarivo',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
  return `${dateStr} ${timeStr}`;
}

function localHourOfDay(v) {
  const d = new Date(String(v));
  const timeStr = d.toLocaleTimeString('fr-FR', {
    timeZone: 'Indian/Antananarivo',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  });
  const hourStr = timeStr.split(':')[0];
  const n = parseInt(hourStr, 10);
  return Number.isFinite(n) ? n : 0;
}

async function deriveSessionFromExam(date_examen, numero_salle, id_surveillant) {
  try {
    const hd = await deriveHeureDebutFromContext(date_examen, numero_salle, id_surveillant);
    if (hd != null) {
      const hour = localHourOfDay(hd);
      return (hour < 12) ? 'Matin' : 'Après-midi';
    }
  } catch (_) {}
  return null;
}

async function deriveHeureDebutFromContext(date_examen, numero_salle, id_surveillant) {
  
  if (id_surveillant != null) {
    try {
      const q1 = `
        SELECT e."heure_debut"
        FROM "planning_surveillance" ps
        JOIN "examen" e
          ON e."date_examen" = ps."date_examen"
         AND e."numero_salle" = ps."numero_salle"
        WHERE ps."date_examen" = $1
          AND ps."numero_salle" = $2
          AND ps."id_surveillant" = $3
        ORDER BY e."heure_debut" ASC
        LIMIT 1`;
      const { rows } = await pool.query(q1, [date_examen, numero_salle, id_surveillant]);
      if (rows[0]?.heure_debut != null) return rows[0].heure_debut;
    } catch (_) {}
  }
 
  try {
    const q2 = 'SELECT "heure_debut" FROM "examen" WHERE "date_examen" = $1 AND "numero_salle" = $2 ORDER BY "heure_debut" ASC LIMIT 1';
    const { rows } = await pool.query(q2, [date_examen, numero_salle]);
    return rows[0]?.heure_debut ?? null;
  } catch (_) {
    return null;
  }
}
