const OPERATORS = {
  'eq.': '=',
  'neq.': '!=',
  'gt.': '>',
  'gte.': '>=',
  'lt.': '<',
  'lte.': '<=',
  'like.': 'LIKE'
};

function parseFilters(query) {
  const filters = [];
  for (const [key, rawVal] of Object.entries(query)) {
    if (key === 'select' || key === 'order' || key === 'limit' || key === 'offset') continue;
    const values = Array.isArray(rawVal) ? rawVal : [rawVal];
    for (const raw of values) {
      if (typeof raw !== 'string') continue;
      const opKey = Object.keys(OPERATORS).find(k => raw.startsWith(k));
      if (!opKey) continue;
      const sqlOp = OPERATORS[opKey];
      let value = raw.substring(opKey.length);
      if (opKey === 'like.') {
        value = value.replaceAll('*', '%');
      }
      filters.push({ column: key, op: sqlOp, value });
    }
  }
  return filters;
}

export function buildSelectQuery(table, query) {
  const selectParam = Array.isArray(query.select) ? query.select[0] : query.select;
  const select = selectParam || '*';
  const filters = parseFilters(query);
  const orderParam = Array.isArray(query.order) ? query.order[0] : query.order; // e.g., "date_examen.desc"
  const limitParam = Array.isArray(query.limit) ? query.limit[0] : query.limit;
  const offsetParam = Array.isArray(query.offset) ? query.offset[0] : query.offset;
  const limit = limitParam ? parseInt(limitParam, 10) : undefined;
  const offset = offsetParam ? parseInt(offsetParam, 10) : undefined;

  const values = [];
  let text = `SELECT ${select} FROM ${safeIdent(table)}`;

  if (filters.length) {
    const clauses = filters.map((f, i) => {
      values.push(f.value);
      return `${safeIdent(f.column)} ${f.op} $${values.length}`;
    });
    text += ' WHERE ' + clauses.join(' AND ');
  }

  if (orderParam) {
    const parts = String(orderParam).split(',').map(p => p.trim()).filter(Boolean);
    if (parts.length) {
      const orderSql = parts.map(p => {
        const [col, dir] = p.split('.');
        const direction = (dir && dir.toUpperCase() === 'DESC') ? 'DESC' : 'ASC';
        return `${safeIdent(col)} ${direction}`;
      }).join(', ');
      text += ` ORDER BY ${orderSql}`;
    }
  }

  if (limit && Number.isFinite(limit)) {
    text += ` LIMIT ${limit}`;
  }
  if (offset && Number.isFinite(offset)) {
    text += ` OFFSET ${offset}`;
  }

  return { text, values };
}

export function buildModifyQuery(kind, table, body, query = {}) {
  const filters = parseFilters(query);
  const values = [];

  if (kind === 'insert') {
    if (!body || typeof body !== 'object' || Array.isArray(body)) {
      throw new Error('Body must be a JSON object');
    }
    const cols = Object.keys(body);
    if (cols.length === 0) throw new Error('Body cannot be empty');
    const placeholders = cols.map((_, i) => `$${i + 1}`);
    cols.forEach(c => values.push(body[c]));
    const text = `INSERT INTO ${safeIdent(table)} (${cols.map(safeIdent).join(',')}) VALUES (${placeholders.join(',')}) RETURNING *`;
    return { text, values };
  }

  if (kind === 'update') {
    if (!filters.length) throw new Error('Update requires at least one filter');
    const setCols = Object.keys(body || {});
    if (!setCols.length) throw new Error('Update body cannot be empty');

    const setSql = setCols.map((c, i) => {
      values.push(body[c]);
      return `${safeIdent(c)} = $${values.length}`;
    }).join(', ');

    const whereSql = filters.map((f) => {
      values.push(f.value);
      return `${safeIdent(f.column)} ${f.op} $${values.length}`;
    }).join(' AND ');

    const text = `UPDATE ${safeIdent(table)} SET ${setSql} WHERE ${whereSql} RETURNING *`;
    return { text, values };
  }

  if (kind === 'delete') {
    if (!filters.length) throw new Error('Delete requires at least one filter');
    const whereSql = filters.map((f, i) => {
      values.push(f.value);
      return `${safeIdent(f.column)} ${f.op} $${values.length}`;
    }).join(' AND ');
    const text = `DELETE FROM ${safeIdent(table)} WHERE ${whereSql}`;
    return { text, values };
  }

  throw new Error('Unknown modify kind');
}

function safeIdent(ident) {
  if (!/^[a-zA-Z_][a-zA-Z0-9_]*$/.test(ident)) {
    throw new Error('Invalid identifier: ' + ident);
  }
  return '"' + ident.replaceAll('"', '""') + '"';
}
