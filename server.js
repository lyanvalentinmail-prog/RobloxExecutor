const http = require('http');
const fs = require('fs');
const path = require('path');
const root = __dirname;
http.createServer(async (req, res) => {
  const url = new URL(req.url, 'http://localhost');
  if (url.pathname === '/api/search') {
    const q = url.searchParams.get('q') || '';
    try {
      const upstream = await fetch('https://scriptblox.com/api/script/search?q=' + encodeURIComponent(q) + '&max=20', {signal: AbortSignal.timeout(10000), headers: {'User-Agent':'ExecutorWorkspace/1.0'}});
      if (!upstream.ok) throw new Error('Servicio no disponible');
      const data = await upstream.json();
      res.writeHead(200, {'Content-Type':'application/json'}); res.end(JSON.stringify(data));
    } catch { res.writeHead(502, {'Content-Type':'application/json'}); res.end(JSON.stringify({error:'No se pudo conectar con ScriptBlox. Inténtalo de nuevo más tarde.'})); }
    return;
  }
  const files = {'/':'index.html','/index.html':'index.html','/style.css':'style.css','/app.js':'app.js'};
  const file = files[url.pathname];
  if (!file) {res.writeHead(404); return res.end('Not found');}
  res.writeHead(200, {'Content-Type': file.endsWith('.css')?'text/css':file.endsWith('.js')?'text/javascript':'text/html; charset=utf-8'});
  fs.createReadStream(path.join(root,file)).pipe(res);
}).listen(process.env.PORT || 3000, '0.0.0.0');
